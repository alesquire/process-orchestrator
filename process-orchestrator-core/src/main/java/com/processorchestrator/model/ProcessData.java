package com.processorchestrator.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Simplified ProcessData model for db-scheduler integration
 * Contains runtime process information for task execution
 */
public class ProcessData implements Serializable {
    private static final long serialVersionUID = 1L;
    private String processId;
    private String processTypeName;
    private int totalTasks;
    private ProcessInputData inputData;
    private List<TaskData> tasks;
    private String processRecordId; // Link to ProcessRecord in database
    private int currentTaskIndex;
    private String status;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private Map<String, Object> processContext;

    public ProcessData() {
        this.currentTaskIndex = 0;
        this.status = "PENDING";
        this.processContext = new HashMap<>();
    }

    public ProcessData(String processId, String processTypeName, int totalTasks, ProcessInputData inputData) {
        this(processId, processTypeName, totalTasks, inputData, null);
    }
    
    public ProcessData(String processId, String processTypeName, int totalTasks, ProcessInputData inputData, String processRecordId) {
        this();
        this.processId = processId;
        this.processTypeName = processTypeName;
        this.totalTasks = totalTasks;
        this.inputData = inputData;
        this.processRecordId = processRecordId;
    }

    // Getters and Setters
    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }

    public String getProcessTypeName() { return processTypeName; }
    public void setProcessTypeName(String processTypeName) { this.processTypeName = processTypeName; }

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }

    public ProcessInputData getInputData() { return inputData; }
    public void setInputData(ProcessInputData inputData) { this.inputData = inputData; }

    public List<TaskData> getTasks() { return tasks; }
    public void setTasks(List<TaskData> tasks) { this.tasks = tasks; }

    public String getProcessRecordId() { return processRecordId; }
    public void setProcessRecordId(String processRecordId) { this.processRecordId = processRecordId; }

    public int getCurrentTaskIndex() { return currentTaskIndex; }
    public void setCurrentTaskIndex(int currentTaskIndex) { this.currentTaskIndex = currentTaskIndex; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Map<String, Object> getProcessContext() { return processContext; }
    public void setProcessContext(Map<String, Object> processContext) { this.processContext = processContext; }

    public void addContext(String key, Object value) {
        this.processContext.put(key, value);
    }

    // Utility methods
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    public boolean isRunning() {
        return "IN_PROGRESS".equals(status);
    }

    public TaskData getCurrentTask() {
        if (tasks != null && currentTaskIndex < tasks.size()) {
            return tasks.get(currentTaskIndex);
        }
        return null;
    }

    public boolean hasMoreTasks() {
        return currentTaskIndex < totalTasks;
    }

    public void moveToNextTask() {
        this.currentTaskIndex++;
    }

    public void markAsCompleted() {
        this.status = "COMPLETED";
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
