package com.processorchestrator.model;

import java.time.Instant;

/**
 * Process Details model - extends ProcessRecord with engine-managed execution details
 * These fields are immutable for users and managed by the engine
 */
public class ProcessDetails extends ProcessRecord {
    // Engine-managed fields (immutable for users)
    private final String currentStatus;
    private final String currentProcessId;
    private final Instant startedWhen;
    private final Instant completedWhen;
    private final Instant failedWhen;
    private final Instant stoppedWhen;
    private final String lastErrorMessage;
    private final Instant createdAt;
    private final Instant updatedAt;

    // Constructor for creating ProcessDetails from ProcessRecord and engine data
    public ProcessDetails(ProcessRecord record, String currentStatus, String currentProcessId,
                         Instant startedWhen, Instant completedWhen, Instant failedWhen, 
                         Instant stoppedWhen, String lastErrorMessage, 
                         Instant createdAt, Instant updatedAt) {
        // Initialize parent fields
        super(record.getId(), record.getType(), record.getInputData(), record.getSchedule());
        
        // Set engine-managed fields
        this.currentStatus = currentStatus;
        this.currentProcessId = currentProcessId;
        this.startedWhen = startedWhen;
        this.completedWhen = completedWhen;
        this.failedWhen = failedWhen;
        this.stoppedWhen = stoppedWhen;
        this.lastErrorMessage = lastErrorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Constructor for creating ProcessDetails with default engine values
    public ProcessDetails(ProcessRecord record) {
        this(record, "PENDING", null, null, null, null, null, null, 
             Instant.now(), Instant.now());
    }

    // Getters for engine-managed fields (no setters - immutable)
    public String getCurrentStatus() { return currentStatus; }
    public String getCurrentProcessId() { return currentProcessId; }
    public Instant getStartedWhen() { return startedWhen; }
    public Instant getCompletedWhen() { return completedWhen; }
    public Instant getFailedWhen() { return failedWhen; }
    public Instant getStoppedWhen() { return stoppedWhen; }
    public String getLastErrorMessage() { return lastErrorMessage; }
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
    public ProcessDetails withStatus(String newStatus, String newProcessId, Instant executionTime, String errorMessage) {
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
            newProcessId,
            newStartedWhen,
            newCompletedWhen,
            newFailedWhen,
            newStoppedWhen,
            newErrorMessage,
            createdAt,
            Instant.now() // Update timestamp
        );
    }

    // Factory method to create ProcessDetails with updated process ID
    public ProcessDetails withCurrentProcessId(String newProcessId) {
        return new ProcessDetails(
            this,
            currentStatus,
            newProcessId,
            startedWhen,
            completedWhen,
            failedWhen,
            stoppedWhen,
            lastErrorMessage,
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
        if (currentProcessId != null ? !currentProcessId.equals(that.currentProcessId) : that.currentProcessId != null) return false;
        if (startedWhen != null ? !startedWhen.equals(that.startedWhen) : that.startedWhen != null) return false;
        if (completedWhen != null ? !completedWhen.equals(that.completedWhen) : that.completedWhen != null) return false;
        if (failedWhen != null ? !failedWhen.equals(that.failedWhen) : that.failedWhen != null) return false;
        if (stoppedWhen != null ? !stoppedWhen.equals(that.stoppedWhen) : that.stoppedWhen != null) return false;
        if (lastErrorMessage != null ? !lastErrorMessage.equals(that.lastErrorMessage) : that.lastErrorMessage != null) return false;
        if (createdAt != null ? !createdAt.equals(that.createdAt) : that.createdAt != null) return false;
        if (updatedAt != null ? !updatedAt.equals(that.updatedAt) : that.updatedAt != null) return false;
        
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (currentStatus != null ? currentStatus.hashCode() : 0);
        result = 31 * result + (currentProcessId != null ? currentProcessId.hashCode() : 0);
        result = 31 * result + (startedWhen != null ? startedWhen.hashCode() : 0);
        result = 31 * result + (completedWhen != null ? completedWhen.hashCode() : 0);
        result = 31 * result + (failedWhen != null ? failedWhen.hashCode() : 0);
        result = 31 * result + (stoppedWhen != null ? stoppedWhen.hashCode() : 0);
        result = 31 * result + (lastErrorMessage != null ? lastErrorMessage.hashCode() : 0);
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (updatedAt != null ? updatedAt.hashCode() : 0);
        return result;
    }
}
