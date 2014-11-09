package com.cclab.core.redundancy;

public abstract class BootSettings {

	/**
	 * 
	 * @param name Name of the Master instance
	 * @param backupName Name of the Backup instance
	 * @return
	 */
	public static String master(String name, String backupName) {
		return "\"" + name + "\" \"master\" \""  + backupName + "\"";
	}
	
	/**
	 * 
	 * @param name Name of the Backup instance
	 * @param masterName Name of the Master instance
	 * @param masterIP IP of the Master instance
	 * @return
	 */
	public static String backup(String name, String masterName, String masterIP) {
		return "\"" + name + "\" \"backup\" \""  + masterName + "\" \""  + masterIP + "\"";
	}
	
	/**
	 * 
	 * @param name Name of the Worker instance
	 * @param masterIP IP of the Master instance
	 * @return
	 */
	public static String worker(String name, String masterIP) {
		return "\"" + name + "\" \"worker\" \""  + masterIP + "\"";
	}

}
