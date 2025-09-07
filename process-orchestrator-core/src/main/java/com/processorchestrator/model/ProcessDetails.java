package com.processorchestrator.model;

import java.time.Instant;

/**
 * Process Details model - extends ProcessRecord with engine-managed execution details
 * These fields are immutable for users and managed by the engine
 */
public class ProcessDetails extends ProcessRecord {
    // Engine-managed fields (immutable for users)
    private final String currentStatus;
    private final Integer currentTaskIndex;
    private final Integer totalTasks;
    private final Instant startedWhen;
    private final Instant completedWhen;
    private final Instant failedWhen;
    private final Instant stoppedWhen;
    private final String lastErrorMessage;
    private final String triggeredBy;
    private final Instant createdAt;
    private final Instant updatedAt;

    // Constructor for creating ProcessDetails from ProcessRecord and engine data
    public ProcessDetails(ProcessRecord record, String currentStatus, Integer currentTaskIndex, Integer totalTasks,
                         Instant startedWhen, Instant completedWhen, Instant failedWhen, 
                         Instant stoppedWhen, String lastErrorMessage, String triggeredBy,
                         Instant createdAt, Instant updatedAt) {
        // Initialize parent fields
        super(record.getId(), record.getType(), record.getInputData(), record.getSchedule());
        
        // Set engine-managed fields
        this.currentStatus = currentStatus;
        this.currentTaskIndex = currentTaskIndex;
        this.totalTasks = totalTasks;
        this.startedWhen = startedWhen;
        this.completedWhen = completedWhen;
        this.failedWhen = failedWhen;
        this.stoppedWhen = stoppedWhen;
        this.lastErrorMessage = lastErrorMessage;
        this.triggeredBy = triggeredBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Constructor for creating ProcessDetails with default engine values
    public ProcessDetails(ProcessRecord record) {
        this(record, "PENDING", 0, 0, null, null, null, null, null, null, 
             Instant.now(), Instant.now());
    }

    // Getters for engine-managed fields (no setters - immutable)
    public String getCurrentStatus() { return currentStatus; }
    public Integer getCurrentTaskIndex() { return currentTaskIndex; }
    public Integer getTotalTasks() { return totalTasks; }
    public Instant getStartedWhen() { return startedWhen; }
    public Instant getCompletedWhen() { return completedWhen; }
    public Instant getFailedWhen() { return failedWhen; }
    public Instant getStoppedWhen() { return stoppedWhen; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public String getTriggeredBy() { return triggeredBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Helper methods for status checking
    public boolean isRunning() {
        return "IN_PROGRESS".equals(currentStatus);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(currentStatus);
    }

    public boolean isFailed() {
        return "FAILED".equals(currentStatus);
    }

    public boolean isStopped() {
        return "STOPPED".equals(currentStatus);
    }

    public boolean isPending() {
        return "PENDING".equals(currentStatus);
    }

    public boolean canBeStarted() {
        return !isRunning();
    }

    public boolean canBeStopped() {
        return isRunning();
    }

    public boolean canBeDeleted() {
        return !isRunning();
    }

    // Factory method to create ProcessDetails with updated status
    public ProcessDetails withStatus(String newStatus, Integer newTaskIndex, Instant executionTime, String errorMessage) {
        Instant newStartedWhen = startedWhen;
        Instant newCompletedWhen = completedWhen;
        Instant newFailedWhen = failedWhen;
        Instant newStoppedWhen = stoppedWhen;
        String newErrorMessage = lastErrorMessage;

        switch (newStatus.toUpperCase()) {
            case "IN_PROGRESS":
                newStartedWhen = executionTime;
                break;
            case "COMPLETED":
                newCompletedWhen = executionTime;
                break;
            case "FAILED":
                newFailedWhen = executionTime;
                newErrorMessage = errorMessage;
                break;
            case "STOPPED":
                newStoppedWhen = executionTime;
                break;
        }

        return new ProcessDetails(
            this, // Use this ProcessDetails as the base ProcessRecord
            newStatus,
            newTaskIndex,
            totalTasks,
            newStartedWhen,
            newCompletedWhen,
            newFailedWhen,
            newStoppedWhen,
            newErrorMessage,
            triggeredBy,
            createdAt,
            Instant.now() // Update timestamp
        );
    }

    // Factory method to create ProcessDetails with updated task progress
    public ProcessDetails withTaskProgress(Integer newTaskIndex, Integer newTotalTasks) {
        return new ProcessDetails(
            this,
            currentStatus,
            newTaskIndex,
            newTotalTasks,
            startedWhen,
            completedWhen,
            failedWhen,
            stoppedWhen,
            lastErrorMessage,
            triggeredBy,
            createdAt,
            Instant.now()
        );
    }

    // Factory method to create ProcessDetails with updated trigger info
    public ProcessDetails withTriggeredBy(String newTriggeredBy) {
        return new ProcessDetails(
            this,
            currentStatus,
            currentTaskIndex,
            totalTasks,
            startedWhen,
            completedWhen,
            failedWhen,
            stoppedWhen,
            lastErrorMessage,
            newTriggeredBy,
            createdAt,
            Instant.now()
        );
    }

    @Override
    public String toString() {
        return String.format("ProcessDetails{id='%s', type='%s', status='%s', schedule='%s', created=%s, updated=%s}", 
                           getId(), getType(), currentStatus, getSchedule(), createdAt, updatedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        
        ProcessDetails that = (ProcessDetails) o;
        
        if (currentStatus != null ? !currentStatus.equals(that.currentStatus) : that.currentStatus != null) return false;
        if (currentTaskIndex != null ? !currentTaskIndex.equals(that.currentTaskIndex) : that.currentTaskIndex != null) return false;
        if (totalTasks != null ? !totalTasks.equals(that.totalTasks) : that.totalTasks != null) return false;
        if (startedWhen != null ? !startedWhen.equals(that.startedWhen) : that.startedWhen != null) return false;
        if (completedWhen != null ? !completedWhen.equals(that.completedWhen) : that.completedWhen != null) return false;
        if (failedWhen != null ? !failedWhen.equals(that.failedWhen) : that.failedWhen != null) return false;
        if (stoppedWhen != null ? !stoppedWhen.equals(that.stoppedWhen) : that.stoppedWhen != null) return false;
        if (lastErrorMessage != null ? !lastErrorMessage.equals(that.lastErrorMessage) : that.lastErrorMessage != null) return false;
        if (triggeredBy != null ? !triggeredBy.equals(that.triggeredBy) : that.triggeredBy != null) return false;
        if (createdAt != null ? !createdAt.equals(that.createdAt) : that.createdAt != null) return false;
        if (updatedAt != null ? !updatedAt.equals(that.updatedAt) : that.updatedAt != null) return false;
        
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (currentStatus != null ? currentStatus.hashCode() : 0);
        result = 31 * result + (currentTaskIndex != null ? currentTaskIndex.hashCode() : 0);
        result = 31 * result + (totalTasks != null ? totalTasks.hashCode() : 0);
        result = 31 * result + (startedWhen != null ? startedWhen.hashCode() : 0);
        result = 31 * result + (completedWhen != null ? completedWhen.hashCode() : 0);
        result = 31 * result + (failedWhen != null ? failedWhen.hashCode() : 0);
        result = 31 * result + (stoppedWhen != null ? stoppedWhen.hashCode() : 0);
        result = 31 * result + (lastErrorMessage != null ? lastErrorMessage.hashCode() : 0);
        result = 31 * result + (triggeredBy != null ? triggeredBy.hashCode() : 0);
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (updatedAt != null ? updatedAt.hashCode() : 0);
        return result;
    }
}
