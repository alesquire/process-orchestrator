package com.processorchestrator.ui.controller;

import com.processorchestrator.config.ProcessType;
import com.processorchestrator.config.ProcessTypeRegistry;
import com.processorchestrator.dao.ProcessRecordDAO;
import com.processorchestrator.model.ProcessDetails;
import com.processorchestrator.model.TaskData;
import com.processorchestrator.service.ProcessOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API controller for the Process UI module.
 * Provides endpoints for fetching process and task data in a format suitable for tabular display.
 */
@Controller
public class ProcessUiController {

    @Autowired
    private ProcessRecordDAO processRecordDAO;

    @Autowired
    private ProcessOrchestrator processOrchestrator;

    @Autowired
    private ProcessTypeRegistry processTypeRegistry;

    /**
     * Serve the task details page
     */
    @GetMapping("/task-details")
    public String taskDetailsPage(@RequestParam String id, Model model) {
        model.addAttribute("taskInstanceId", id);
        return "task-details";
    }

    /**
     * REST API endpoints
     */
    @RestController
    @RequestMapping("/api")
    @CrossOrigin(origins = "*")
    public static class ProcessUiApiController {
        
        @Autowired
        private ProcessRecordDAO processRecordDAO;

        @Autowired
        private ProcessOrchestrator processOrchestrator;

        @Autowired
        private ProcessTypeRegistry processTypeRegistry;

        @Autowired
        private com.processorchestrator.controller.ProcessController processController;

        @PostMapping("/processes/{processId}/start")
        public Map<String, Object> startProcess(@PathVariable("processId") String processId) {
            try {
                com.processorchestrator.controller.ProcessController.ProcessStartResponse response = 
                    processController.startProcess(processId);
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", response.isSuccess());
                result.put("message", response.getMessage());
                if (response.isSuccess()) {
                    result.put("processId", response.getProcessId());
                    result.put("orchestratorProcessId", response.getOrchestratorProcessId());
                }
                return result;
            } catch (Exception e) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "Error starting process: " + e.getMessage());
                return result;
            }
        }

        @PostMapping("/processes/{processId}/stop")
        public Map<String, Object> stopProcess(@PathVariable("processId") String processId) {
            try {
                com.processorchestrator.controller.ProcessController.ProcessStopResponse response = 
                    processController.stopProcess(processId);
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", response.isSuccess());
                result.put("message", response.getMessage());
                if (response.isSuccess()) {
                    result.put("processId", response.getProcessId());
                }
                return result;
            } catch (Exception e) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "Error stopping process: " + e.getMessage());
                return result;
            }
        }

        @PostMapping("/processes/{processId}/restart")
        public Map<String, Object> restartProcess(@PathVariable("processId") String processId) {
            try {
                com.processorchestrator.controller.ProcessController.ProcessStartResponse response = 
                    processController.restartProcess(processId);
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", response.isSuccess());
                result.put("message", response.getMessage());
                if (response.isSuccess()) {
                    result.put("processId", response.getProcessId());
                    result.put("orchestratorProcessId", response.getOrchestratorProcessId());
                }
                return result;
            } catch (Exception e) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "Error restarting process: " + e.getMessage());
                return result;
            }
        }

        @GetMapping("/task-details/{taskId}")
        public Map<String, Object> getTaskDetails(@PathVariable("taskId") String taskId) {
            System.out.println("DEBUG: Requested task ID: " + taskId);
            Map<String, Object> result = processOrchestrator.getTaskDetails(taskId);
            System.out.println("DEBUG: Task details result: " + result);
            return result;
        }

        @GetMapping("/debug/task-ids")
        public List<String> getAllTaskIds() {
            return processOrchestrator.getAllTaskIds();
        }

        @GetMapping("/process-types")
        public ProcessTypesResponse getProcessTypes() {
            List<ProcessType> processTypes = processTypeRegistry.getAllProcessTypes().values().stream()
                    .collect(Collectors.toList());
            
            // Get all unique task names from all process types
            Set<String> allTaskNames = new HashSet<>();
            for (ProcessType processType : processTypes) {
                for (int i = 0; i < processType.getTaskCount(); i++) {
                    allTaskNames.add(processType.getTask(i).getName());
                }
            }
            
            List<String> taskNames = allTaskNames.stream()
                    .sorted()
                    .collect(Collectors.toList());
            
            return new ProcessTypesResponse(processTypes, taskNames);
        }

        /**
         * Get all processes with their tasks in a tabular format
         */
        @GetMapping("/processes")
        public ProcessTableResponse getProcessesTable() {
            List<ProcessDetails> processes = processRecordDAO.findAll();
            
            // Get all unique task names from all process types (not just executed tasks)
            Set<String> allTaskNames = new HashSet<>();
            Map<String, ProcessType> processTypeMap = processTypeRegistry.getAllProcessTypes();
            
            // Collect task names from all defined process types
            for (ProcessType processType : processTypeMap.values()) {
                for (int i = 0; i < processType.getTaskCount(); i++) {
                    allTaskNames.add(processType.getTask(i).getName());
                }
            }
            
            Map<String, List<TaskData>> processTasksMap = new HashMap<>();
            
            // Get actual task execution data for each process
            for (ProcessDetails process : processes) {
                List<TaskData> tasks = processOrchestrator.getProcessTasks(process.getId());
                processTasksMap.put(process.getId(), tasks);
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
        public ProcessDetailResponse getProcessDetail(@PathVariable("processId") String processId) {
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

        /**
         * Response class for process types
         */
        public static class ProcessTypesResponse {
            private List<ProcessType> processTypes;
            private List<String> taskNames;

            public ProcessTypesResponse(List<ProcessType> processTypes, List<String> taskNames) {
                this.processTypes = processTypes;
                this.taskNames = taskNames;
            }

            // Getters
            public List<ProcessType> getProcessTypes() { return processTypes; }
            public List<String> getTaskNames() { return taskNames; }
        }
    }
}
