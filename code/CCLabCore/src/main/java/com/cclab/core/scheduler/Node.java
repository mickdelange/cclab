package com.cclab.core.scheduler;

import java.util.LinkedList;
import java.util.Queue;

import com.amazonaws.services.ec2.model.Instance;
import com.cclab.core.AwsConnect;

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
			case STOPPED :
				idleSince = Integer.MAX_VALUE;
				workingSince = Integer.MAX_VALUE;
				break;
			case STARTING :
				idleSince = Integer.MAX_VALUE;
				workingSince = Integer.MAX_VALUE;
				break;
		}
		
	}
	
	String instanceId;
	Queue<Task> q = new LinkedList<Task>();
	State state;
	long idleSince = Integer.MAX_VALUE;
	long workingSince = Integer.MAX_VALUE;
	long maxTaskTime = 60000; // Maximum time allowed for one task, in milliseconds.
	
	/**
	 * Construct Node object.
	 * Used by the scheduler to monitor Nodes.
	 * @param inst
	 */
	Node(Instance inst) {
		instanceId = inst.getInstanceId();
		updateState(inst);
	}
	
	/**
	 * Check the state of the node
	 * @param inst
	 */
	public void updateState(Instance inst) {
		long currTime = System.currentTimeMillis();
		String currState = inst.getState().getName();
		
		if (inst.getInstanceId() == instanceId) {
			// Check if node has finished booting
			if (state == State.STARTING && currState == "running") {
				switchState(State.IDLE);
				// Start processing queue
				doWork();
			} // Check if node is taking too long to perform task
			else if (state == State.WORKING && (currTime-workingSince) > maxTaskTime) {
				// Kill node
				stop();
			} // Machine has unexpectedly quit
			else if (currState != "running" && state != State.STOPPED) {
				switchState(State.STOPPED);
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
		if (AwsConnect.startInstance(instanceId)) {
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
		if (AwsConnect.stopInstance(instanceId)) {
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
		// Reset working timer
		switchState(State.WORKING);
		
		// Get first job in queue
//		Task t = q.peek();
		
		// TODO: send task to Node: execute task t
	}
	
	/**
	 * After receiving confirmation that a Task was completed, execute this.
	 * @param t The completed Task
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
