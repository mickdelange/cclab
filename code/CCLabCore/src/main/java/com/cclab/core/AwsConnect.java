package com.cclab.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RebootInstancesRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
/***
 * This class manages all connections to the AWS system.
 * @author Mick de Lange
 */
public class AwsConnect {

    static AmazonEC2 ec2;
    static Set<Instance> instances = new HashSet<Instance>();
    static long lastUpdate = 0;
    static final int updateInterval = 5000; // Update the list at most every 5 seconds
    static boolean initialised = false;
    
    /**
     * Initialise the instances list
     * @throws Exception
     */
    public static void init() throws Exception {
    	if (!initialised) { // Prevent multiple initialisations
	    	// Get credentials from file
	        AWSCredentials credentials = new PropertiesCredentials(
	        		AwsConnect.class.getResourceAsStream("AwsCredentials.properties"));
	
	        // Init EC2 object
	        ec2 = new AmazonEC2Client(credentials);
	        
	        initialised = true;
    	}
    }
    
    /**
     * Retrieves the instances from AWS API, when update interval has passed.
     */
    private static void retrieveInstances() {
    	
    	long currTime = System.currentTimeMillis();
    	
    	if ((lastUpdate + updateInterval) < currTime) { // Prevent updating too often.
    		// List all instances
            try {
                DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
                List<Reservation> reservations = describeInstancesRequest.getReservations();
                instances.clear();

                for (Reservation reservation : reservations) {
                    instances.addAll(reservation.getInstances());
                }
            } catch (AmazonServiceException ase) {
                    System.out.println("Caught Exception: " + ase.getMessage());
                    System.out.println("Reponse Status Code: " + ase.getStatusCode());
                    System.out.println("Error Code: " + ase.getErrorCode());
                    System.out.println("Request ID: " + ase.getRequestId());
            }
            lastUpdate = System.currentTimeMillis();
    	}
       
    }
    
    /**
     * Return the list of all instances
     * @return list of all instances
     */
    public static Set<Instance> getInstances() {
        retrieveInstances();
    	return instances;
    }
    
    /**
     * Return a specific instance
     * @return requested instance, null if not found
     */
    public static Instance getInstance(String instanceId) {
        retrieveInstances();
        for (Instance inst : instances) {
    		if (instanceId.equals(inst.getInstanceId()))
    			return inst;
    	}
    	return null;
    }
    
    /**
     * Get the private IP address for an instance
     * @param instanceId
     * @return Private IP of instance
     */
    public static String getInstancePrivIP(String instanceId) {
    	Instance inst = getInstance(instanceId);
    	if (inst != null)
    		return inst.getPrivateIpAddress();
    	else
    		return "";
    }
    
    /**
     * Get the public IP address for an instance
     * @param instanceId
     * @return Public IP of instance
     */
    public static String getInstancePubIP(String instanceId) {
    	Instance inst = getInstance(instanceId);
    	if (inst != null)
    		return inst.getPublicIpAddress();
    	else
    		return "";
    }
    
    /**
     * Get the state of an instance
     * @param instanceId
     * @return State of instance: (pending, running, shutting-down, terminated, stopping, stopped)
     */
    public static String getInstanceState(String instanceId) {
    	Instance inst = getInstance(instanceId);
    	if (inst != null)
    		return inst.getState().getName();
    	else
    		return "";
    }

    /**
     * Start an instance by instanceId
     * @param instanceId ID of the instance to start
     */
    public static boolean startInstance(String instanceId) {
    	try {
	        List<String> instancesToStart = new ArrayList<String>();
	        instancesToStart.add(instanceId);
	        
	        StartInstancesRequest sir = new StartInstancesRequest();
	        sir.setInstanceIds(instancesToStart);
	        
	        ec2.startInstances(sir);
	        return true;
    	} catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
    	}
    	return false;
    }
    
    /**
     * Stop an instance by instanceId
     * @param instanceId ID of the instance to stop
     */
    public static boolean stopInstance(String instanceId) {
    	try {
	        List<String> instancesToStop = new ArrayList<String>();
	        instancesToStop.add(instanceId);
	        
	        StopInstancesRequest sir = new StopInstancesRequest();
	        sir.setInstanceIds(instancesToStop);
	        
	        ec2.stopInstances(sir);
	        return true;
    	} catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
    	}
    	return false;
    }
    
    /**
     * Reboot an instance by instanceId
     * @param instanceId ID of the instance to reboot
     */
    public static boolean rebootInstance(String instanceId) {
    	try {
	        List<String> instancesToReboot = new ArrayList<String>();
	        instancesToReboot.add(instanceId);
	        
	        RebootInstancesRequest rir = new RebootInstancesRequest();
	        rir.setInstanceIds(instancesToReboot);
	        
	        ec2.rebootInstances(rir);
	        return true;
    	} catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
    	}
    	return false;
    }
}
