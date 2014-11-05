package com.cclab.core.scheduler;

/**
 * Task object, for task maintainance.
 * @author Mick de Lange
 *
 */
public class Task {
	
	public String inputId;
	public int errorCount = 0;
	
	public Task(String id) {
		inputId = id;
	}
	
	public void flagProblem() {
		errorCount++;
	}
}
