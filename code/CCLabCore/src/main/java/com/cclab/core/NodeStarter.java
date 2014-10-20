package com.cclab.core;

import com.cclab.core.network.GeneralComm;

import java.io.IOException;

/**
 * Created by ane on 10/19/14.
 */
public class NodeStarter {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Not enough args!");
            System.exit(1);
        }
        //TODO find out hostname
        String me = "localhost";
        if (args[0].equals("master")) {
            try {
                if (args.length > 1)
                    new MasterInstance(me, Integer.parseInt(args[1]));
                else
                    new MasterInstance(me, GeneralComm.DEFAULT_PORT);
            } catch (IOException e) {
                System.out.println("Could not start master");
                NodeLogger.get().error(e.getMessage(), e);
                System.exit(1);
            }
        } else if (args[0].equals("worker")) {
            try {
                if (args.length == 2)
                    new WorkerInstance(me, args[1], GeneralComm.DEFAULT_PORT);
                if (args.length > 2)
                    new WorkerInstance(me, args[1], Integer.parseInt(args[2]));
                else
                    new WorkerInstance(me, "localhost", GeneralComm.DEFAULT_PORT);
            } catch (IOException e) {
                System.out.println("Could not start worker");
                NodeLogger.get().error(e.getMessage(), e);
                System.exit(1);
            }
        } else {
            System.out.println("Specify node type!");
            System.exit(1);
        }
    }

}
