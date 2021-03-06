package com.cclab.core;

import com.cclab.core.network.GeneralComm;
import com.cclab.core.utils.NodeLogger;

import java.io.IOException;

/**
 * The main class for running a node instance.
 * <br/>
 * Created on 10/19/14 for CCLabCore.
 *
 * @author an3m0na
 */
public class NodeStarter {

    private static final String usage = "Usage:\t<single>\n" +
            "\t<name> <master> <backup_name> [<port>]\n" +
            "\t<name> <backup> <master_name> <master_ip> [<master_port>]\n" +
            "\t<name> <worker> [<master_ip> [<master_port>]]";

    public static void main(String[] args) {
    	if (args.length == 1 && args[0].equals("single")) {
    		// Run a single instance
    		new SingleInstance();
        } else if (args.length < 2) {
            System.out.println(usage);
            System.exit(1);
        } else {
	        String me = args[0];
	        if (args[1].equals("master")) {
	            try {
	                if (args.length == 3)
	                    new MasterInstance(me, args[2], GeneralComm.DEFAULT_PORT);
	                else if (args.length > 3)
	                    new MasterInstance(me, args[2], Integer.parseInt(args[3]));
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
	        } else if (args[1].equals("backup")) {
	        	 try {
	                 if (args.length == 4)
	                     new BackupInstance(me, args[2], args[3], GeneralComm.DEFAULT_PORT);
	                 else if (args.length > 4)
	                     new BackupInstance(me, args[2], args[3], Integer.parseInt(args[4]));
	             } catch (IOException e) {
	                 System.out.println("Could not start backup master");
	                 NodeLogger.get().error(e.getMessage(), e);
	                 System.exit(1);
	             }
	        } else {
	            System.out.println(usage);
	            System.exit(1);
	        }
        }
    }

}
