package com.cclab.core.scheduler;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import com.amazonaws.services.ec2.model.Instance;
import com.cclab.core.AwsConnect;
import com.cclab.core.utils.NodeLogger;

/**
 * Scheduler class that assigns tasks, starts and stops nodes.
 * @author Mick de Lange
 */
public class Scheduler extends Thread {
	
	Queue<Task> mainQ = new LinkedList<Task>();
	Node[] nodes;
	final int loadThresh = 2;
	final int interval = 2000; // Milliseconds before next run.
	
	public Scheduler() {
		try {
			AwsConnect.init();
			Set<Instance> instances = AwsConnect.getInstances();
			
			// init node list
			nodes = new Node[instances.size()];
			
			int i = 0;
			for (Instance inst : instances) {
				nodes[i] = new Node(inst);
				i++;
			}
		} catch (Exception e) {
			NodeLogger.get().error(e.getMessage(), e);
		}
		
	}
	
	public void run() {
		Task currT;
			try {
				while(true) {
					Thread.sleep(interval);
					// Check availability of new Nodes
					updateNodeStates();
					
					// Check main queue
					while (mainQ.size() > 0) {
						currT = mainQ.poll();
						if (currT != null) {
							assignTask(currT);
						}
					}
					
				}
			} catch (InterruptedException e) {
				NodeLogger.get().error(e.getMessage(), e);
			}
	}
	
	/**
	 * Received notification that a Task was finished.
	 * @param t
	 * @param instanceId
	 */
	public void taskFinished(String instanceId) {
		for (Node n: nodes) {
			if (n.instanceId == instanceId) {
				n.taskFinished();
				return;
			}
		}
	}
	
	/**
	 * Add a new task to be assigned
	 * @param t
	 */
	public void addTask(Task t) {
		mainQ.add(t);
	}
	
	private void updateNodeStates() {
		Set<Instance> instances = AwsConnect.getInstances();
		
		int i = 0;
		for (Instance inst : instances) {
			nodes[i].updateState(inst);
			i++;
		}
	}
	
	/**
	 * Assign a task to an available Node.
	 */
	private boolean assignTask(Task t) {
		Node n = findIdleNode();
		if (n != null) {
			// IDLE node available
			n.assign(t);
			return true;
		} else {
			n = findNodeUnderThreshold();
			if (n != null) {
				// Low load node available
				n.assign(t);
				return true;
			} else {
				// All RUNNING Nodes busy
				n = startNewNode();
				if (n!=null) {
					// New node started to work on task
					// Wait for next round to assign task
					return false;
				} else {
					// No new nodes available
					n = findNodeLowestLoad();
					if (n != null) {
						// Assign task to least busiest node
						n.assign(t);
						return true;
					} else {
						// This would mean no node could be started and no node is currently running
						// Something went terribly wrong...
						NodeLogger.get().error("SCHEDULER ERROR - There seem to be no Nodes available, and none can be started.");
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Search for an idle Node.
	 * @return A Node if found, null otherwise.
	 */
	private Node findIdleNode() {
		for (Node n: nodes) {
			if (n.state == Node.State.IDLE)
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
			if (n.state != Node.State.STOPPED && n.queueSize() <= loadThresh)
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
			if (n.state != Node.State.STOPPED && n.queueSize() <= lowestLoad)
				sel = n;
		}
		return sel;
	}
	
	/**
	 * Start a new node, if available.
	 * If a node is already starting, do not start another one.
	 * @return A Node if found, null otherwise.
	 */
	private Node startNewNode() {
		Node sel = null;
		for (Node n: nodes) {
			if (n.state == Node.State.STARTING) {
				sel = n;
			}
			if (n.state == Node.State.STOPPED && sel == null) {
				sel = n;
			}
		}
		// Only start a new node if no STARTING node can be found.
		if (sel != null && sel.state == Node.State.STOPPED) {
			sel.start();
		}
		return sel;
	}

}
