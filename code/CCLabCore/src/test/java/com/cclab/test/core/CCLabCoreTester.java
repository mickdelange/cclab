package com.cclab.test.core;

import com.amazonaws.services.ec2.model.Instance;
import com.cclab.core.AwsConnect;

/**
 * Created by ane on 10/15/14.
 */
public class CCLabCoreTester {
    public static void main(String[] args) {

        System.out.println("===========================================");
        System.out.println("Creating thumbnail");
        System.out.println("===========================================");

//        try {
//            ImageProcessor.process("/Users/ane/Downloads/strawberry.jpg", "/Users/ane/Downloads/strawberry_small.jpg", "blur");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        System.out.println("===========================================");
        System.out.println("Listing AWS instances");
        System.out.println("===========================================");

        try {
            AwsConnect.init();

//            AwsConnect.startInstance("i-56afa3bd");
//            AwsConnect.startInstance("i-c1bbb72a");
//            AwsConnect.startInstance("i-1bb57cfa");
//            AwsConnect.startInstance("i-3eb27bdf");

            // List all instance IDs, IPs & states
            for (Instance inst : AwsConnect.getInstances()) {
                System.out.println(inst.getInstanceId() + " : " + inst.getPublicIpAddress() + " : " + inst.getState().getName());
            }


        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }

        System.out.println("===========================================");
        System.out.println("DONE!");
        System.out.println("===========================================");
    }
}
