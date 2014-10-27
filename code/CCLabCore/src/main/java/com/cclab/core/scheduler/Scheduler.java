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
	final int loadThresh = 2; // Amount of tasks that defines a node with low load
	final int interval = 2000; // Milliseconds before next run.
    private boolean shouldExit = false;
	
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
					if(shouldExit) // Stop the loop.
	                    break;
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
	
	/**
	 * Update node states bases on AWS response
	 */
	private void updateNodeStates() {
		Set<Instance> instances = AwsConnect.getInstances();
		
		int i = 0;
		for (Instance inst : instances) {
			nodes[i].updateState(inst);
			// Check if node has lost tasks
			if (nodes[i].hasLostTasks()) {
				// Add tasks back to main queue
				for (Task t : nodes[i].q) {
					addTask(t);
				}
			}
			i++;
		}
	}
	
	/**
	 * Assign a task to an available Node.
	 * @param t Task to assign.
	 * @return True is task was assigned to an active node, False otherwise.
	 */
	private boolean assignTask(Task t) {
		
		// Search for an IDLE node
		Node n = findIdleNode();
		if (n != null) {
			// IDLE node available, assign Task
			n.assign(t);
			return true;
		} 
		
		// No IDLE node, find node with load < threshold
		n = findNodeUnderThreshold();
		if (n != null) {
			// Low load node available, assign Task
			n.assign(t);
			return true;
		}

		// All RUNNING Nodes busy, start new node
		n = startNewNode();
		if (n!=null) {
			// New node started, assign Task to run when finished booting
			n.assign(t);
			return true;
		}

		// No new nodes available, all nodes are running
		n = findNodeLowestLoad();
		if (n != null) {
			// Assign task to least busy node
			n.assign(t);
			return true;
		} else {
			// This would mean no node could be started and no node is currently running
			// Something went terribly wrong...
			NodeLogger.get().error("SCHEDULER ERROR - There seem to be no Nodes available, and none can be started.");
			return false;
		}
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
	
	/**
	 * Kill scheduler thread.
	 */
    public void quit(){
        shouldExit = true;
    }

}
