package com.processorchestrator.controller;

import com.processorchestrator.model.ProcessInputData;
import com.processorchestrator.service.ProcessManager;
import com.processorchestrator.service.ProcessOrchestrator;
import com.processorchestrator.service.ProcessResultService;
import com.processorchestrator.model.ProcessData;
import com.processorchestrator.model.TaskData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for managing processes via API calls
 */
public class ProcessController {
    private static final Logger logger = LoggerFactory.getLogger(ProcessController.class);
    
    private final ProcessManager processManager;
    private final ProcessOrchestrator processOrchestrator;
    private final ProcessResultService resultService;

    public ProcessController(ProcessManager processManager, ProcessOrchestrator processOrchestrator, ProcessResultService resultService) {
        this.processManager = processManager;
        this.processOrchestrator = processOrchestrator;
        this.resultService = resultService;
    }

    /**
     * Create a new process definition
     */
    public ProcessManager.ProcessRecord createProcess(String id, String type, String inputData, String schedule) {
        logger.info("Creating process: id={}, type={}, schedule={}", id, type, schedule);
        
        // Validate that process doesn't already exist
        if (processManager.getProcess(id).isPresent()) {
            throw new IllegalArgumentException("Process with ID " + id + " already exists");
        }
        
        // Create the process record
        processManager.createProcess(id, type, inputData, schedule);
        
        // Return the created process
        return processManager.getProcess(id).orElseThrow(() -> 
            new RuntimeException("Failed to retrieve created process"));
    }

    /**
     * Start a process by ID
     */
    public ProcessStartResponse startProcess(String id) {
        logger.info("Starting process: {}", id);
        
        // Get the process record
        ProcessManager.ProcessRecord processRecord = processManager.getProcess(id)
            .orElseThrow(() -> new IllegalArgumentException("Process not found: " + id));
        
        // Check if process can be started
        if ("IN_PROGRESS".equals(processRecord.getStatus())) {
            throw new IllegalStateException("Process is already running: " + id);
        }
        
        if ("COMPLETED".equals(processRecord.getStatus())) {
            logger.info("Restarting completed process: {}", id);
        }
        
        try {
            // Parse input data
            ProcessInputData inputData = parseInputData(processRecord.getInputData());
            
            // Generate execution ID for tracking
            String executionId = UUID.randomUUID().toString();
            
            // Record execution start
            processManager.recordExecution(executionId, id, "MANUAL", Instant.now(), null, "STARTED", null);
            
            // Update process status
            processManager.updateProcessStatus(id, "IN_PROGRESS");
            
            // Start the process using the orchestrator
            String orchestratorProcessId = processOrchestrator.startProcess(processRecord.getType(), inputData);
            
            // Update process with orchestrator details
            processManager.updateProcessExecution(id, 0, 0, Instant.now(), null);
            
            logger.info("Process {} started successfully with orchestrator ID: {}", id, orchestratorProcessId);
            
            return new ProcessStartResponse(true, "Process started successfully", executionId, orchestratorProcessId);
            
        } catch (Exception e) {
            logger.error("Failed to start process: {}", id, e);
            
            // Update process status to failed
            processManager.updateProcessStatus(id, "FAILED", e.getMessage());
            
            // Record failed execution
            String executionId = UUID.randomUUID().toString();
            processManager.recordExecution(executionId, id, "MANUAL", Instant.now(), Instant.now(), "FAILED", e.getMessage());
            
            return new ProcessStartResponse(false, "Failed to start process: " + e.getMessage(), executionId, null);
        }
    }

    /**
     * Stop a process by ID
     */
    public ProcessStopResponse stopProcess(String id) {
        logger.info("Stopping process: {}", id);
        
        // Get the process record
        ProcessManager.ProcessRecord processRecord = processManager.getProcess(id)
            .orElseThrow(() -> new IllegalArgumentException("Process not found: " + id));
        
        // Check if process can be stopped
        if (!"IN_PROGRESS".equals(processRecord.getStatus())) {
            throw new IllegalStateException("Process is not running: " + id);
        }
        
        try {
            // Update process status to stopped
            processManager.updateProcessStatus(id, "STOPPED");
            processManager.updateProcessExecution(id, processRecord.getCurrentTaskIndex(), 
                                                processRecord.getTotalTasks(), 
                                                processRecord.getStartedAt(), Instant.now());
            
            // Record execution stop
            String executionId = UUID.randomUUID().toString();
            processManager.recordExecution(executionId, id, "MANUAL", 
                                         processRecord.getStartedAt(), Instant.now(), "STOPPED", null);
            
            logger.info("Process {} stopped successfully", id);
            
            return new ProcessStopResponse(true, "Process stopped successfully", executionId);
            
        } catch (Exception e) {
            logger.error("Failed to stop process: {}", id, e);
            return new ProcessStopResponse(false, "Failed to stop process: " + e.getMessage(), null);
        }
    }

    /**
     * Get process state by ID
     */
    public ProcessStateResponse getProcessState(String id) {
        logger.info("Getting process state: {}", id);
        
        // Get the process record
        ProcessManager.ProcessRecord processRecord = processManager.getProcess(id)
            .orElseThrow(() -> new IllegalArgumentException("Process not found: " + id));
        
        // Get execution history
        List<ProcessManager.ExecutionRecord> executionHistory = processManager.getExecutionHistory(id);
        
        // Get task details if process is running
        List<TaskData> tasks = null;
        if ("IN_PROGRESS".equals(processRecord.getStatus())) {
            try {
                // Try to get tasks from the orchestrator
                tasks = processOrchestrator.getProcessTasks(id);
            } catch (Exception e) {
                logger.warn("Could not retrieve task details for process: {}", id, e);
            }
        }
        
        return new ProcessStateResponse(processRecord, executionHistory, tasks);
    }

    /**
     * Get all processes
     */
    public List<ProcessManager.ProcessRecord> getAllProcesses() {
        logger.info("Getting all processes");
        return processManager.getAllProcesses();
    }

    /**
     * Get processes by status
     */
    public List<ProcessManager.ProcessRecord> getProcessesByStatus(String status) {
        logger.info("Getting processes by status: {}", status);
        return processManager.getProcessesByStatus(status);
    }

    /**
     * Delete a process (only if not completed)
     */
    public ProcessDeleteResponse deleteProcess(String id) {
        logger.info("Deleting process: {}", id);
        
        try {
            processManager.deleteProcess(id);
            return new ProcessDeleteResponse(true, "Process deleted successfully");
        } catch (Exception e) {
            logger.error("Failed to delete process: {}", id, e);
            return new ProcessDeleteResponse(false, "Failed to delete process: " + e.getMessage());
        }
    }

    /**
     * Parse input data JSON string to ProcessInputData object
     */
    private ProcessInputData parseInputData(String inputDataJson) {
        try {
            // For now, create a simple ProcessInputData from the JSON string
            // In a real implementation, you might want to use Jackson or Gson
            ProcessInputData inputData = new ProcessInputData();
            
            // Simple parsing - in production, use proper JSON parsing
            if (inputDataJson != null && !inputDataJson.trim().isEmpty()) {
                // For this example, we'll assume the input data contains key-value pairs
                // separated by semicolons, e.g., "input_file:/data/input.json;output_dir:/data/output"
                String[] pairs = inputDataJson.split(";");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();
                        
                        switch (key) {
                            case "input_file":
                                inputData.setInputFile(value);
                                break;
                            case "output_dir":
                                inputData.setOutputDir(value);
                                break;
                            case "user_id":
                                inputData.setUserId(value);
                                break;
                            default:
                                inputData.addConfig(key, value);
                                break;
                        }
                    }
                }
            }
            
            return inputData;
            
        } catch (Exception e) {
            logger.error("Failed to parse input data: {}", inputDataJson, e);
            throw new RuntimeException("Invalid input data format", e);
        }
    }

    // Response classes
    public static class ProcessStartResponse {
        private final boolean success;
        private final String message;
        private final String executionId;
        private final String orchestratorProcessId;

        public ProcessStartResponse(boolean success, String message, String executionId, String orchestratorProcessId) {
            this.success = success;
            this.message = message;
            this.executionId = executionId;
            this.orchestratorProcessId = orchestratorProcessId;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getExecutionId() { return executionId; }
        public String getOrchestratorProcessId() { return orchestratorProcessId; }
    }

    public static class ProcessStopResponse {
        private final boolean success;
        private final String message;
        private final String executionId;

        public ProcessStopResponse(boolean success, String message, String executionId) {
            this.success = success;
            this.message = message;
            this.executionId = executionId;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getExecutionId() { return executionId; }
    }

    public static class ProcessStateResponse {
        private final ProcessManager.ProcessRecord processRecord;
        private final List<ProcessManager.ExecutionRecord> executionHistory;
        private final List<TaskData> tasks;

        public ProcessStateResponse(ProcessManager.ProcessRecord processRecord, 
                                  List<ProcessManager.ExecutionRecord> executionHistory, 
                                  List<TaskData> tasks) {
            this.processRecord = processRecord;
            this.executionHistory = executionHistory;
            this.tasks = tasks;
        }

        public ProcessManager.ProcessRecord getProcessRecord() { return processRecord; }
        public List<ProcessManager.ExecutionRecord> getExecutionHistory() { return executionHistory; }
        public List<TaskData> getTasks() { return tasks; }
    }

    public static class ProcessDeleteResponse {
        private final boolean success;
        private final String message;

        public ProcessDeleteResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
