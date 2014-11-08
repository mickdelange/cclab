package com.cclab.core.scheduler;

import com.amazonaws.services.ec2.model.Instance;
import com.cclab.core.AwsConnect;
import com.cclab.core.MasterInstance;
import com.cclab.core.data.Database;
import com.cclab.core.utils.NodeLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Scheduler class that assigns tasks, starts and stops nodes.
 *
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
    int availableNodes;
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
            NodeLogger.get().info("Scheduler started in Test Mode.");
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
     *
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
    	boolean backupNotified = false;
        try {
            while (true) {
                if (shouldExit) // Stop the loop.
                    break;
                synchronized (this) {
                    wait(interval);
                }
                // Check availability of new Nodes
                updateNodeStates();

                // Update main queue
                backupNotified = getTasks();

                // Assign any new tasks
                assignTasks();
                
                if (!backupNotified)
                	myMaster.backupStillAlive();
            }
        } catch (InterruptedException e) {
            NodeLogger.get().error(e.getMessage(), e);
        }
    }

    /**
     * Assign tasks to an available Node.
     * @return True is task was assigned to an active node, False otherwise.
     */
    private void assignTasks() {
    	
    	boolean idleNodes = true;

        while (!mainQ.isEmpty() && idleNodes) {

	        // Search for an IDLE node
	        Node n = findIdleNode();
	        if (n != null) {
	            // IDLE node available, assign Task
	            n.assign(mainQ.poll());
	        } else {
		        NodeLogger.get().debug("No idle nodes available");
	        	// No IDLE nodes available, check if new one should be started
	        	idleNodes = false;
	        	if (queueSize()/availableNodes > loadThresh) {
	        		n = startNewNode();
	        		if (n == null) {
	        	        // No new nodes available, all nodes are running
	        			NodeLogger.get().debug("No new nodes available");
	        		}
	        	}
	        }
        }
    }

    /**
     * Received a connection with a node.
     *
     * @param instanceId
     */
    public void nodeConnected(String instanceId) {
        for (Node n : workerNodes) {
            if (n.instanceId.equals(instanceId)) {
                n.nodeStarted();
                break;
            }
        }
        synchronized (this) {
            notify();
        }
    }

    /**
     * Received notification that a Task was finished.
     *
     * @param instanceId
     */
    public void taskFinished(String instanceId) {
        for (Node n : workerNodes) {
            if (n.instanceId.equals(instanceId)) {
                n.taskFinished();
                break;
            }
        }
        synchronized (this) {
            notify();
        }
    }

    /**
     * Add a new task to be assigned
     *
     * @param t
     */
    public void addTask(Task t) {
        mainQ.add(t);
    }

    /**
     * Get all tasks from input folder
     */
    private boolean getTasks() {
        String newInput = Database.getInstance().getNextRecordId();
        Task nT;
        boolean backupNotified = false;
        while (newInput != null) {
            // Create and add task
            nT = new Task(newInput);
            addTask(nT);
            
            // Backup new task
 			myMaster.backupNewTask(nT.inputId);
			backupNotified = true;

            // Get next
            newInput = Database.getInstance().getNextRecordId();
        }
        return backupNotified;
    }

    /**
     * Update node states bases on AWS response
     */
    private void updateNodeStates() {
        Set<Instance> instances = AwsConnect.getInstances();
        availableNodes = 0;

        Node wn = null;
        Task currT = null;
        for (Instance inst : instances) {
            wn = getWorkerById(inst.getInstanceId());
            if (wn != null) {
                wn.updateState(inst);
                if (wn.state != Node.State.STOPPED)
                	availableNodes++;
                // Check if node has lost tasks
                if (wn.hasLostTask()) {
                    // Add tasks back to main queue
                    currT = wn.currTask;
                    // Only add tasks back to queue that have not caused too many errors.
                    if (currT.errorCount < maxTaskRetry)
                        addTask(currT);
                    else
                        NodeLogger.get().error("Task " + currT.inputId + " has caused too many errors, removed from queue.");
                    wn.currTask = null; // reset task for node
                }
            }
        }
    }

    /**
     * Search for an idle Node.
     *
     * @return A Node if found, null otherwise.
     */
    private Node findIdleNode() {
        for (Node n : workerNodes) {
            if (n.state == Node.State.IDLE)
                return n;
        }
        return null;
    }

    /**
     * Start a new node, if available.
     * If a node is already starting, do not start another one.
     *
     * @return A Node if found, null otherwise.
     */
    private Node startNewNode() {
        for (Node n : workerNodes) {
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
     * Start a specific node.
     * For command line usage.
     */
    public void startNode(String instanceId) {
    	NodeLogger.getBoot().info("START_" + instanceId);
        Node n = getWorkerById(instanceId);
        if (n != null)
        	n.start();
        else
        	NodeLogger.getBoot().error("Could not find instance");
    }

    /**
     * Get a worker node by its instanceId
     *
     * @param instId
     * @return Node, null if not found
     */
    private Node getWorkerById(String instId) {
        for (Node n : workerNodes) {
            if (n.instanceId.equals(instId))
                return n;
        }
        return null;
    }

    /**
     * Get the queue size
     *
     * @return
     */
    public int queueSize() {
        return mainQ.size();
    }

    /**
     * Kill scheduler thread.
     */
    public void quit() {
        shouldExit = true;
    }

}
