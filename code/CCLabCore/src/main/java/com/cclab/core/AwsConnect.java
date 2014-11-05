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
    
    /**
     * Initialise the instances list
     * @throws Exception
     */
    public static void init() throws Exception {
    	// Get credentials from file
        AWSCredentials credentials = new PropertiesCredentials(
        		AwsConnect.class.getResourceAsStream("AwsCredentials.properties"));

        // Init EC2 object
        ec2 = new AmazonEC2Client(credentials);
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
}
