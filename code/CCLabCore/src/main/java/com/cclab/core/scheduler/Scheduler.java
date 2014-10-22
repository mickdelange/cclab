package com.cclab.core.scheduler;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import com.amazonaws.services.ec2.model.Instance;
import com.cclab.core.scheduler.Node.State;

/**
 * Scheduler class that assigns tasks, starts and stops nodes.
 * @author Mick de Lange
 */
public class Scheduler {
	
	Queue<Task> mainQ = new LinkedList<Task>();
	Node[] nodes;
	final int loadThresh = 2;
	
	public void init(Set<Instance> instances) {
		// init node list
		nodes = new Node[instances.size()];
		
		int i = 0;
		for (Instance inst : instances) {
			nodes[i] = new Node(inst);
			i++;
		}
	}
	
	/**
	 * Search for an idle Node.
	 * @return A Node if found, null otherwise.
	 */
	private Node findIdleNode() {
		for (Node n: nodes) {
			if (n.state == State.IDLE)
				return n;
		}
		return null;
	}
	
	/**
	 * Find a Node with load under threshold.
	 * @return A Node if found, null otherwise.
	 */
	private Node findNodeUnderThreshold() {
		for (Node n: nodes) {
			if (n.state != State.STOPPED && n.queueSize() <= loadThresh)
				return n;
		}
		return null;
	}
	
	/**
	 * Find the Node with the lowest load.
	 * Will return null if no node is running.
	 * @return A Node if found, null otherwise.
	 */
	private Node findNodeLowestLoad() {
		int lowestLoad = Integer.MAX_VALUE;
		Node sel = null;
		for (Node n: nodes) {
			if (n.state != State.STOPPED && n.queueSize() <= lowestLoad)
				sel = n;
		}
		return sel;
	}
	
	/**
	 * Start a new node, if available.
	 * @return A Node if found, null otherwise.
	 */
	private Node startNewNode() {
		for (Node n: nodes) {
			if (n.state == State.STOPPED) {
				n.start();
				return n;
			}
		}
		return null;
	}
	
	/**
	 * Assign a new received task to an available Node.
	 */
	public void assignTask(Task t) {
		Node n = findIdleNode();
		if (n != null) {
			// IDLE node available
			n.assign(t);
		} else {
			n = findNodeUnderThreshold();
			if (n != null) {
				// Low load node available
				n.assign(t);
			} else {
				// All RUNNING Nodes busy
				n = startNewNode();
				if (n!=null) {
					// New node started to work on task
					// TODO: implement method that waits for boot and assigns task
				} else {
					// No new nodes available
					n = findNodeLowestLoad();
					if (n != null) {
						// Assign task to least busiest node
						n.assign(t);
					} else {
						// This would mean no node could be started and no node is currently running
						// Something went terribly wrong...
						throw new Error("There seem to be no Nodes available, and none can be started.");
					}
				}
			}
		}
	}
	
	/**
	 * Received notification that a Task was finished.
	 * @param t
	 * @param instanceId
	 */
	public void taskFinished(Task t, String instanceId) {
		for (Node n: nodes) {
			if (n.instanceId == instanceId) {
				n.taskFinished(t);
				return;
			}
		}
	}

}
