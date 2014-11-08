package com.cclab.test.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import com.cclab.core.network.ClientComm;

/**
 * Parse the log files to produce CSV type data files.
 * @author Mick de Lange
 *
 */
public class LogParser {
    public static void main(String[] args) {
    	
    	if (args.length > 0) {
            System.out.println("===========================================");
            System.out.println("Running LogParser");
            System.out.println("===========================================");

            String basePath = args[0];
            String fileName = args[1];
            
            for (int i = 2; i < args.length; i++) {
                System.out.println();
                System.out.println(args[i].split("/")[0]);
            	BufferedReader reader;
				try {
					reader = new BufferedReader(new FileReader(basePath + args[i] + fileName));
	            	String line = null;
	            	String[] splitLine;
	            	String[] splitLine2;
	            	String task, file, machine;
	            	long start = 0, end = 0;
	            	HashMap<String, Long> fileStart = new HashMap<String, Long>();
	            	HashMap<String, Long> fileTime = new HashMap<String, Long>();
	            	HashMap<String, Long> machineStart = new HashMap<String, Long>();
	            	HashMap<String, Long> machineTime = new HashMap<String, Long>();
	            	HashMap<String, String> fileMachine = new HashMap<String, String>();
	            	long time;
	            	while ((line = reader.readLine()) != null) {
	            		splitLine = line.split(" -> ");
	            		
	            		// Parse work
	            		splitLine2 = splitLine[1].split("_");
	            		task = splitLine2[0];
	            		file = splitLine2[1];
	            		machine = splitLine2[2];
	            		
	            		// Parse time
	            		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
	            		Date date = sdf.parse(splitLine[0]);
	            		time = date.getTime();
	            		
	            		if (task.equals("ASSIGN")) {
	            			fileStart.put(file, time);
	            			fileMachine.put(file,  machine);
	            			if (!machineStart.containsKey(machine))
	            				machineStart.put(machine, time);
	            		}
	            		else if (task.equals("DONE")) {
	            			fileTime.put(file, (time - fileStart.get(file)));
	            			// Replace machine time
	            			machineTime.remove(machine);
	            			machineTime.put(machine, (time - machineStart.get(machine)));
	            		}
	            		
	            		if (start == 0)
	            			start = time;
	            		
	            		end = time;
	            	}

            		System.out.println();
            		System.out.println("File;Time;Machine");
            		for (String f : fileTime.keySet()) {
            			System.out.println(f + ";" + fileTime.get(f) + ";" + fileMachine.get(f));
            		}

            		System.out.println();
            		System.out.println("Machine;Time");
            		for (String m : machineTime.keySet()) {
            			System.out.println(m + ";" + machineTime.get(m));
            		}
            		
            		System.out.println("Total;" + (end - start));
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println(e.getMessage());
				}
            }

            System.out.println("===========================================");
            System.out.println("DONE!");
            System.out.println("===========================================");
    	} else {
            System.out.println("===========================================");
            System.out.println("No arguments provided!");
            System.out.println("Usage: <basepath> <filename> <directory> [<directory2> [...]]");
            System.out.println("No arguments provided!");
            System.out.println("===========================================");
    	}
    }
}
