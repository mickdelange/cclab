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

public class Master {

    static AmazonEC2 ec2;
    static Set<Instance> instances;
    
    /***
     * Initialise all required values
     * @throws Exception
     */
    private static void init() throws Exception {
    	// Get credentials from file
        AWSCredentials credentials = new PropertiesCredentials(
        		Master.class.getResourceAsStream("AwsCredentials.properties"));

        // Init EC2 object
        ec2 = new AmazonEC2Client(credentials);

        // List all instances
        try {
            DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
            List<Reservation> reservations = describeInstancesRequest.getReservations();
            instances = new HashSet<Instance>();

            for (Reservation reservation : reservations) {
                instances.addAll(reservation.getInstances());
            }
        } catch (AmazonServiceException ase) {
                System.out.println("Caught Exception: " + ase.getMessage());
                System.out.println("Reponse Status Code: " + ase.getStatusCode());
                System.out.println("Error Code: " + ase.getErrorCode());
                System.out.println("Request ID: " + ase.getRequestId());
        }
    }
    
    public static void main(String[] args) throws Exception {

        System.out.println("===========================================");
        System.out.println("Welcome to the AWS Java SDK!");
        System.out.println("===========================================");

        init();
        
        // List all instance IDs, IPs & states
        for (Instance inst : instances) {
        	System.out.println(inst.getInstanceId() + " : " + inst.getPublicIpAddress() + " : " + inst.getState().getName());
        }

        System.out.println("===========================================");
        System.out.println("DONE!");
        System.out.println("===========================================");
    }

    /***
     * Start an instance by ID
     * @param instanceId ID of the instance to start
     */
    private static void startInstance(String instanceId) {
    	try {
	        List<String> instancesToStop = new ArrayList<String>();
	        instancesToStop.add(instanceId);
	        
	        StartInstancesRequest sir = new StartInstancesRequest();
	        sir.setInstanceIds(instancesToStop);
	        
	        ec2.startInstances(sir);
    	} catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
    	}
    }
    
    /***
     * Stop an instance by ID
     * @param instanceId ID of the instance to stop
     */
    private static void stopInstance(String instanceId) {
    	try {
	        List<String> instancesToStop = new ArrayList<String>();
	        instancesToStop.add(instanceId);
	        
	        StopInstancesRequest sir = new StopInstancesRequest();
	        sir.setInstanceIds(instancesToStop);
	        
	        ec2.stopInstances(sir);
    	} catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
    	}
    }
}
