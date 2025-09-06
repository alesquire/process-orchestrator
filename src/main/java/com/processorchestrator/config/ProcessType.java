package com.processorchestrator.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for a process type - defines the sequence of tasks
 */
public class ProcessType {
    private String name;
    private String description;
    private List<TaskDefinition> tasks;

    public ProcessType() {
        this.tasks = new ArrayList<>();
    }

    public ProcessType(String name, String description) {
        this.name = name;
        this.description = description;
        this.tasks = new ArrayList<>();
    }

    public ProcessType addTask(String name, String command) {
        return addTask(name, command, null, 60, 3);
    }

    public ProcessType addTask(String name, String command, String workingDirectory, 
                              int timeoutMinutes, int maxRetries) {
        TaskDefinition taskDef = new TaskDefinition(name, command, workingDirectory, timeoutMinutes, maxRetries);
        this.tasks.add(taskDef);
        return this;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<TaskDefinition> getTasks() { return tasks; }
    public void setTasks(List<TaskDefinition> tasks) { this.tasks = tasks; }

    public int getTaskCount() { return tasks.size(); }

    public TaskDefinition getTask(int index) {
        if (index >= 0 && index < tasks.size()) {
            return tasks.get(index);
        }
        return null;
    }
}