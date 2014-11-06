package com.cclab.core.scheduler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import com.amazonaws.services.ec2.model.Instance;
import com.cclab.core.AwsConnect;
import com.cclab.core.MasterInstance;
import com.cclab.core.data.Database;
import com.cclab.core.utils.NodeLogger;

/**
 * Scheduler class that assigns tasks, starts and stops nodes.
 * @author Mick de Lange
 */
public class Scheduler extends Thread {
	
	Queue<Task> mainQ = new LinkedList<Task>();
	List<Node> workerNodes = new ArrayList<Node>();
	
	int loadThresh;
	int interval;
	long maxTaskTime;
	long maxIdleTime;
	int maxTaskRetry;
	boolean testMode; // Test Mode: do not actually start / stop nodes in AWS
	
    MasterInstance myMaster;
	List<String> masterIds;
	
    private boolean shouldExit = false;
	
	public Scheduler(List<String> mi, MasterInstance m) {
		myMaster = m;
		masterIds = mi;
		if (!loadProperties()) {
			// Something went wrong loading properties, set to default
			loadThresh = 5;
			interval = 2000;
			maxTaskTime = 60000;
			maxIdleTime = 3600000;
			maxTaskRetry = 2;
			testMode = false;
		}
		
		if (testMode) {
			System.out.println("Scheduler started in Test Mode.");
		}
		
		try {
			AwsConnect.init();
			Set<Instance> instances = AwsConnect.getInstances();
			
			for (Instance inst : instances) {
				if (!masterIds.contains(inst.getInstanceId()))
					workerNodes.add(new Node(inst, maxTaskTime, maxIdleTime, testMode, myMaster));
			}
		} catch (Exception e) {
			NodeLogger.get().error(e.getMessage(), e);
		}
		
	}
	
	/**
	 * Get the properties from the config file.
	 * @return True if succeeded, False otherwise.
	 */
	public boolean loadProperties() {
		Properties prop = new Properties();
		
		try {
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream("scheduler.properties");
			
			if (inputStream != null) {
				prop.load(inputStream);
				loadThresh = Integer.parseInt(prop.getProperty("loadThresh"));
				interval = Integer.parseInt(prop.getProperty("interval"));
				maxTaskTime = Integer.parseInt(prop.getProperty("maxTaskTime"));
				maxIdleTime = Integer.parseInt(prop.getProperty("maxIdleTime"));
				testMode = Boolean.parseBoolean(prop.getProperty("testMode"));
				maxTaskRetry = Integer.parseInt(prop.getProperty("maxTaskRetry"));
				return true;
			} else {
				NodeLogger.get().error("Scheduler properties file not found");
				return false;
			}
		} catch (IOException e) {
			NodeLogger.get().error(e.getMessage(), e);
			return false;
		}
	}
	
	/**
	 * Run the scheduler
	 */
	public void run() {
		Task currT;
		try {
			while(true) {
				if(shouldExit) // Stop the loop.
                    break;
				Thread.sleep(interval);
				// Check availability of new Nodes
				updateNodeStates();
				
				// Update main queue
				getTasks();
				
				// Assign any new tasks
				currT = mainQ.poll();
				while (currT != null) {
					// Assign
					assignTask(currT);
					// Get next
					currT = mainQ.poll();
				}
				
			}
		} catch (InterruptedException e) {
			NodeLogger.get().error(e.getMessage(), e);
		}
	}
	
	/**
	 * Received a connection with a node.
	 * @param instanceId
	 */
	public void nodeConnected(String instanceId) {
		for (Node n: workerNodes) {
			if (n.instanceId.equals(instanceId)) {
				n.nodeStarted();
				return;
			}
		}
	}
	
	/**
	 * Received notification that a Task was finished.
	 * @param instanceId
	 */
	public void taskFinished(String instanceId) {
		for (Node n: workerNodes) {
			if (n.instanceId.equals(instanceId)) {
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
	 * Get all tasks from input folder
	 */
	private void getTasks() {
		String newInput = Database.getInstance().getNextRecordId();
		Task nT;
		while (newInput != null) {
			// Create and add task
			nT = new Task(newInput);
			addTask(nT);
			
			// Backup new task
			myMaster.backupNewTask(nT.inputId);
			
			// Get next
			newInput = Database.getInstance().getNextRecordId();
		}
	}
	
	/**
	 * Update node states bases on AWS response
	 */
	private void updateNodeStates() {
		Set<Instance> instances = AwsConnect.getInstances();
		
		Node wn = null;
		Task currT = null;
		for (Instance inst : instances) {
			wn = getWorkerById(inst.getInstanceId());
			if (wn != null) {
				wn.updateState(inst);
				// Check if node has lost tasks
				if (wn.hasLostTasks()) {
					// Add tasks back to main queue
					currT = wn.q.poll();
					while (currT != null) {
						// Only add tasks back to queue that have not caused too many errors.
						if (currT.errorCount < maxTaskRetry)
							addTask(currT);
						else
							NodeLogger.get().error("Task " + currT.inputId + " has caused too many errors, removed from queue.");
						currT = wn.q.poll();
					}
				}
			}
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
		for (Node n: workerNodes) {
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
		for (Node n: workerNodes) {
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
		for (Node n: workerNodes) {
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
		for (Node n: workerNodes) {
			if (n.state == Node.State.STARTING) {
				return n;
			} else if (n.state == Node.State.STOPPED) {
				// Only start a new node if no STARTING node can be found.
				n.start();
				return n;
			}
		}
		return null;
	}
	
	/**
	 * Get a worker node by its instanceId
	 * @param instId
	 * @return Node, null if not found
	 */
	private Node getWorkerById(String instId) {
		for (Node n: workerNodes) {
			if (n.instanceId.equals(instId))
				return n;
		}
		return null;
	}
	
	/**
	 * Kill scheduler thread.
	 */
    public void quit(){
        shouldExit = true;
    }

}
