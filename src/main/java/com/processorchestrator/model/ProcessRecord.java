package com.processorchestrator.model;

/**
 * Process Record model - represents a process template (user-managed fields only)
 * This contains only the fields that users can specify when creating a process record
 */
public class ProcessRecord {
    private String id;
    private String type;
    private String inputData;
    private String schedule;

    // Constructors
    public ProcessRecord() {
    }

    public ProcessRecord(String id, String type, String inputData, String schedule) {
        this.id = id;
        this.type = type;
        this.inputData = inputData;
        this.schedule = schedule;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getInputData() { return inputData; }
    public void setInputData(String inputData) { this.inputData = inputData; }
    
    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    // Helper methods
    public boolean isScheduled() {
        return schedule != null && !schedule.trim().isEmpty();
    }

    public boolean isManual() {
        return !isScheduled();
    }

    @Override
    public String toString() {
        return String.format("ProcessRecord{id='%s', type='%s', schedule='%s'}", 
                           id, type, schedule);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessRecord that = (ProcessRecord) o;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
