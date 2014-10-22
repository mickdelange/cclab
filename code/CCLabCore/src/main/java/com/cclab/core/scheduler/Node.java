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
	
	String instanceId;
	Queue<Task> q = new LinkedList<Task>();
	State state;
	long idleSince = 0;
	
	/**
	 * Construct Node object.
	 * Used by the scheduler to monitor Nodes.
	 * @param inst
	 */
	Node(Instance inst) {
		instanceId = inst.getInstanceId();
		updateState(inst);
	}
	
	public void updateState(Instance inst) {
		if (inst.getInstanceId() == instanceId) {
			if (inst.getState().getName() == "running") {
				setIdle();
			} else {
				state = State.STOPPED;
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
			setIdle();
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
			state = State.STOPPED;
			return true;
		}
		return false;
	}
	
	/**
	 * Assign a Task to a Node
	 * @param t
	 */
	public void assign(Task t) {
		// TODO: send task to Node
		q.add(t);
		state = State.WORKING;
	}
	
	/**
	 * After receiving confirmation that a Task was completed, execute this.
	 * @param t The completed Task
	 */
	public void taskFinished() {
		q.poll();
		if (queueSize() == 0)
			setIdle();
	}
	
	/**
	 * Get the queue size
	 * @return
	 */
	public int queueSize() {
		return q.size();
	}
	
	/**
	 * Set state to IDLE and record time since going idle.
	 */
	private void setIdle() {
		state = State.IDLE;
		idleSince = System.currentTimeMillis();
	}

}
