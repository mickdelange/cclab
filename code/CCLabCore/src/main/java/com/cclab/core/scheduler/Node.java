package com.cclab.core.scheduler;

import java.util.LinkedList;
import java.util.Queue;

import com.amazonaws.services.ec2.model.Instance;
import com.cclab.core.AwsConnect;
import com.cclab.core.MasterInstance;
import com.cclab.core.utils.NodeLogger;

/**
 * Class that keeps track of Node status for Scheduler
 * @author Mick de Lange
 */
public class Node {
	
	enum State {
		IDLE, WORKING, STOPPED, STARTING;
	}
	
	/**
	 * Set state and timers
	 */
	private void switchState(State s) {
		state = s;
		switch(state){
			case IDLE :
				idleSince = System.currentTimeMillis();
				workingSince = Integer.MAX_VALUE;
				break;
			case WORKING :
				idleSince = Integer.MAX_VALUE;
				workingSince = System.currentTimeMillis();
				break;
			default :
				idleSince = Integer.MAX_VALUE;
				workingSince = Integer.MAX_VALUE;
				break;
		}
		
	}
	
	String instanceId;
	Queue<Task> q = new LinkedList<Task>();
	State state = State.STARTING;
	long idleSince = Integer.MAX_VALUE;
	long workingSince = Integer.MAX_VALUE;
	long maxTaskTime;
	long maxIdleTime;
	boolean testMode; // Test Mode: do not actually start / stop nodes in AWS
    MasterInstance myMaster;
	
	/**
	 * Construct Node object.
	 * Used by the scheduler to monitor Nodes.
	 * @param inst
	 */
	Node(Instance inst, long mtt, long mit, boolean tm, MasterInstance m) {
		myMaster = m;
		instanceId = inst.getInstanceId();
		maxTaskTime = mtt;
		maxIdleTime = mit;
		testMode = tm;
		updateState(inst);
	}
	
	/**
	 * After receiving a connection, the node is IDLE and available for tasks.
	 * Automatically check if there are tasks in queue.
	 */
	public void nodeStarted() {
		NodeLogger.get().info("Node " + instanceId + " has finished booting up.");
		// Set state to IDLE
		switchState(State.IDLE);

		// Start processing queue
		doWork();
	}
	
	/**
	 * Check the state of the node
	 * @param inst
	 */
	public void updateState(Instance inst) {
		long currTime = System.currentTimeMillis();
		String currState = inst.getState().getName();
		
		if (inst.getInstanceId().equals(instanceId)) {
			// Check if node is taking too long to perform task
			if (state == State.WORKING && (currTime-workingSince) > maxTaskTime) {
				NodeLogger.get().error("Node " + instanceId + " was WORKING for too long.");
				// Flag Task as problematic
				q.peek().flagProblem();
				// Kill node
				stop();
			} // Machine has unexpectedly quit
			else if (state != State.STOPPED && !currState.equals("running")) {
				NodeLogger.get().error("Node " + instanceId + " has unexpectedly quit.");
				switchState(State.STOPPED);
			} // Machine has been IDLE for a long time
			else if (currState.equals("running") && state == State.IDLE && (currTime-idleSince) > maxIdleTime) {
				NodeLogger.get().info("Node " + instanceId + " was IDLE for too long.");
				// Kill node
				stop();
			}
		} else {
			throw new Error("InstanceId changed");
		}
	}
	
	/**
	 * Start the node
	 * @return
	 */
	public boolean start() {
		if (testMode) {
			switchState(State.STARTING);
			System.out.println("TESTMODE: " + instanceId + " was started.");
			return true;
		} else if(AwsConnect.startInstance(instanceId)) {
			switchState(State.STARTING);
			return true;
		}
		return false;
	}
	
	/**
	 * Stop the node
	 * @return
	 */
	public boolean stop() {
		if (testMode) {
			switchState(State.STOPPED);
			System.out.println("TESTMODE: " + instanceId + " was terminated.");
			return true;
		} else if (AwsConnect.stopInstance(instanceId)) {
			switchState(State.STOPPED);
			return true;
		}
		return false;
	}
	
	/**
	 * Assign a Task to a Node
	 * @param t
	 */
	public void assign(Task t) {
		// Add to queue
		q.add(t);
		
		// Execute task immediately if IDLE
		if (state == State.IDLE) {
			// Perform tasks
			doWork();
		}
	}
	
	/**
	 * Execute task in queue
	 */
	private void doWork() {
		// Get first job in queue
		Task t = q.peek();
		
		if (t != null) {
			// Send task to worker instance
			myMaster.sendTaskTo(instanceId, t.inputId);
			// Reset working timer
			switchState(State.WORKING);
		}
	}
	
	/**
	 * After receiving confirmation that a Task was completed, execute this.
	 */
	public void taskFinished() {
		// Remove finished job from queue
		q.poll();
		
		// Check if queue is empty
		if (queueSize() == 0) {
			switchState(State.IDLE);
		} else {
			// Execute next task
			doWork();
		}
	}

	/**
	 * The node is stopped, but not all tasks have been completed.
	 * Probably cause by a crash or the node got stuck on a task.
	 * @return True is tasks remain in queue while STOPPED, False otherwise
	 */
	public boolean hasLostTasks() {
		return state == State.STOPPED && queueSize() > 0;
	}
	
	/**
	 * Get the queue size
	 * @return
	 */
	public int queueSize() {
		return q.size();
	}

}
