package com.cclab.core;

/**
 * Created by ane on 10/19/14.
 */
public class NodeStarter {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Not enough args!");
            System.exit(1);
        }
        if (args[0].equals("master"))
            new MasterInstance();
        else if (args[0].equals("worker"))
            new WorkerInstance();
        else {
            System.out.println("Specify node type!");
            System.exit(1);
        }
    }

}
