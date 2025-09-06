package com.processorchestrator.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data class for process information stored in db-scheduler task data
 */
public class ProcessData implements Serializable {
    private String processId;
    private String processType;
    private ProcessStatus status;
    private int currentTaskIndex;
    private int totalTasks;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private List<TaskData> tasks;
    
    // Input context
    private ProcessInputData inputData;
    private Map<String, Object> processContext; // Runtime context

    public ProcessData() {
        this.processContext = new HashMap<>();
    }

    public ProcessData(String processId, String processType, int totalTasks, ProcessInputData inputData) {
        this();
        this.processId = processId;
        this.processType = processType;
        this.totalTasks = totalTasks;
        this.inputData = inputData;
        this.status = ProcessStatus.NOT_STARTED;
        this.currentTaskIndex = 0;
    }

    // Getters and Setters
    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }

    public String getProcessType() { return processType; }
    public void setProcessType(String processType) { this.processType = processType; }

    public ProcessStatus getStatus() { return status; }
    public void setStatus(ProcessStatus status) { this.status = status; }

    public int getCurrentTaskIndex() { return currentTaskIndex; }
    public void setCurrentTaskIndex(int currentTaskIndex) { this.currentTaskIndex = currentTaskIndex; }

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public List<TaskData> getTasks() { return tasks; }
    public void setTasks(List<TaskData> tasks) { this.tasks = tasks; }

    public ProcessInputData getInputData() { return inputData; }
    public void setInputData(ProcessInputData inputData) { this.inputData = inputData; }

    public Map<String, Object> getProcessContext() { return processContext; }
    public void setProcessContext(Map<String, Object> processContext) { this.processContext = processContext; }

    // Business methods
    public boolean isCompleted() {
        return status == ProcessStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == ProcessStatus.FAILED;
    }

    public boolean hasMoreTasks() {
        return currentTaskIndex < totalTasks;
    }

    public TaskData getCurrentTask() {
        if (tasks != null && currentTaskIndex < tasks.size()) {
            return tasks.get(currentTaskIndex);
        }
        return null;
    }

    public void moveToNextTask() {
        this.currentTaskIndex++;
        if (currentTaskIndex >= totalTasks) {
            this.status = ProcessStatus.COMPLETED;
            this.completedAt = Instant.now();
        }
    }

    public void markAsFailed(String errorMessage) {
        this.status = ProcessStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    public void markAsStarted() {
        this.status = ProcessStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }
    
    public void addContext(String key, Object value) {
        this.processContext.put(key, value);
    }
    
    public Object getContext(String key) {
        return this.processContext.get(key);
    }
}