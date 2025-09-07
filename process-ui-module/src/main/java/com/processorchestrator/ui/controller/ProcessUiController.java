package com.processorchestrator.ui.controller;

import com.processorchestrator.dao.ProcessRecordDAO;
import com.processorchestrator.model.ProcessDetails;
import com.processorchestrator.model.TaskData;
import com.processorchestrator.service.ProcessOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API controller for the Process UI module.
 * Provides endpoints for fetching process and task data in a format suitable for tabular display.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ProcessUiController {

    @Autowired
    private ProcessRecordDAO processRecordDAO;

    @Autowired
    private ProcessOrchestrator processOrchestrator;

    /**
     * Get all processes with their tasks in a tabular format
     */
    @GetMapping("/processes")
    public ProcessTableResponse getProcessesTable() {
        List<ProcessDetails> processes = processRecordDAO.findAll();
        
        // Get all unique task names from actual processes in the database
        Set<String> allTaskNames = new HashSet<>();
        Map<String, List<TaskData>> processTasksMap = new HashMap<>();
        
        // Collect task names from actual process executions in the database
        for (ProcessDetails process : processes) {
            List<TaskData> tasks = processOrchestrator.getProcessTasks(process.getId());
            processTasksMap.put(process.getId(), tasks);
            
            // Add task names from actual executed tasks
            for (TaskData task : tasks) {
                allTaskNames.add(task.getName());
            }
        }
        
        // Convert to sorted list for consistent column ordering
        List<String> taskNames = allTaskNames.stream()
                .sorted()
                .collect(Collectors.toList());
        
        return new ProcessTableResponse(taskNames, processes, processTasksMap);
    }

    /**
     * Get a specific process with its tasks
     */
    @GetMapping("/processes/{processId}")
    public ProcessDetailResponse getProcessDetail(@PathVariable String processId) {
        Optional<ProcessDetails> processOpt = processRecordDAO.findById(processId);
        if (!processOpt.isPresent()) {
            throw new RuntimeException("Process not found: " + processId);
        }
        
        ProcessDetails process = processOpt.get();
        List<TaskData> tasks = processOrchestrator.getProcessTasks(processId);
        return new ProcessDetailResponse(process, tasks);
    }

    /**
     * Get process statistics
     */
    @GetMapping("/statistics")
    public ProcessStatisticsResponse getStatistics() {
        List<ProcessDetails> allProcesses = processRecordDAO.findAll();
        
        long totalProcesses = allProcesses.size();
        long completedProcesses = allProcesses.stream()
                .filter(p -> "COMPLETED".equals(p.getCurrentStatus()))
                .count();
        long inProgressProcesses = allProcesses.stream()
                .filter(p -> "IN_PROGRESS".equals(p.getCurrentStatus()))
                .count();
        long failedProcesses = allProcesses.stream()
                .filter(p -> "FAILED".equals(p.getCurrentStatus()))
                .count();
        
        return new ProcessStatisticsResponse(totalProcesses, completedProcesses, inProgressProcesses, failedProcesses);
    }

    /**
     * Response class for the processes table
     */
    public static class ProcessTableResponse {
        private List<String> taskNames;
        private List<ProcessDetails> processes;
        private Map<String, List<TaskData>> processTasks;

        public ProcessTableResponse(List<String> taskNames, List<ProcessDetails> processes, Map<String, List<TaskData>> processTasks) {
            this.taskNames = taskNames;
            this.processes = processes;
            this.processTasks = processTasks;
        }

        // Getters
        public List<String> getTaskNames() { return taskNames; }
        public List<ProcessDetails> getProcesses() { return processes; }
        public Map<String, List<TaskData>> getProcessTasks() { return processTasks; }
    }

    /**
     * Response class for process detail
     */
    public static class ProcessDetailResponse {
        private ProcessDetails process;
        private List<TaskData> tasks;

        public ProcessDetailResponse(ProcessDetails process, List<TaskData> tasks) {
            this.process = process;
            this.tasks = tasks;
        }

        // Getters
        public ProcessDetails getProcess() { return process; }
        public List<TaskData> getTasks() { return tasks; }
    }

    /**
     * Response class for statistics
     */
    public static class ProcessStatisticsResponse {
        private long totalProcesses;
        private long completedProcesses;
        private long inProgressProcesses;
        private long failedProcesses;

        public ProcessStatisticsResponse(long totalProcesses, long completedProcesses, long inProgressProcesses, long failedProcesses) {
            this.totalProcesses = totalProcesses;
            this.completedProcesses = completedProcesses;
            this.inProgressProcesses = inProgressProcesses;
            this.failedProcesses = failedProcesses;
        }

        // Getters
        public long getTotalProcesses() { return totalProcesses; }
        public long getCompletedProcesses() { return completedProcesses; }
        public long getInProgressProcesses() { return inProgressProcesses; }
        public long getFailedProcesses() { return failedProcesses; }
    }
}
