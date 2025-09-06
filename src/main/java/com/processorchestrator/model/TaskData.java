package com.processorchestrator.model;

import java.io.Serializable;
import java.time.Instant;

/**
 * Data class for individual task information stored in db-scheduler task data
 */
public class TaskData implements Serializable {
    private String taskId;
    private String processId;
    private String name;
    private String command;
    private String workingDirectory;
    private int timeoutMinutes;
    private int maxRetries;
    private int retryCount;
    private String status;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private int exitCode;
    private String output;

    public TaskData() {}

    public TaskData(String taskId, String processId, String name, String command, 
                   String workingDirectory, int timeoutMinutes, int maxRetries) {
        this.taskId = taskId;
        this.processId = processId;
        this.name = name;
        this.command = command;
        this.workingDirectory = workingDirectory;
        this.timeoutMinutes = timeoutMinutes;
        this.maxRetries = maxRetries;
        this.status = "PENDING";
        this.retryCount = 0;
    }

    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }

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

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getExitCode() { return exitCode; }
    public void setExitCode(int exitCode) { this.exitCode = exitCode; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    // Business methods
    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void markAsCompleted(int exitCode, String output) {
        this.status = "COMPLETED";
        this.exitCode = exitCode;
        this.output = output;
        this.completedAt = Instant.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    public void markAsStarted() {
        this.status = "IN_PROGRESS";
        this.startedAt = Instant.now();
    }
}