package com.cclab.core;

import com.cclab.core.network.GeneralComm;
import com.cclab.core.utils.NodeLogger;

import java.io.IOException;

/**
 * Created by ane on 10/19/14.
 */
public class NodeStarter {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Not enough args!");
            System.exit(1);
        }

        String me = args[0];
        if (args[1].equals("master")) {
            try {
                if (args.length > 2)
                    new MasterInstance(me, Integer.parseInt(args[2]));
                else
                    new MasterInstance(me, GeneralComm.DEFAULT_PORT);
            } catch (IOException e) {
                System.out.println("Could not start master");
                NodeLogger.get().error(e.getMessage(), e);
                System.exit(1);
            }
        } else if (args[1].equals("worker")) {
            try {
                if (args.length == 3)
                    new WorkerInstance(me, args[2], GeneralComm.DEFAULT_PORT);
                else if (args.length > 3)
                    new WorkerInstance(me, args[2], Integer.parseInt(args[3]));
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
