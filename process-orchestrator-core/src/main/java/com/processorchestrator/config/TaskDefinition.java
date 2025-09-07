package com.processorchestrator.config;

/**
 * Task definition within a process type
 */
public class TaskDefinition {
    private String name;
    private String command;
    private String workingDirectory;
    private int timeoutMinutes;
    private int maxRetries;

    public TaskDefinition() {}

    public TaskDefinition(String name, String command) {
        this.name = name;
        this.command = command;
        this.timeoutMinutes = 60;
        this.maxRetries = 3;
    }

    public TaskDefinition(String name, String command, String workingDirectory, 
                         int timeoutMinutes, int maxRetries) {
        this.name = name;
        this.command = command;
        this.workingDirectory = workingDirectory;
        this.timeoutMinutes = timeoutMinutes;
        this.maxRetries = maxRetries;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getWorkingDirectory() { return workingDirectory; }
    public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }

    public int getTimeoutMinutes() { return timeoutMinutes; }
    public void setTimeoutMinutes(int timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
}