package com.processorchestrator.controller;

import com.processorchestrator.dao.ProcessRecordDAO;
import com.processorchestrator.model.ProcessInputData;
import com.processorchestrator.model.ProcessDetails;
import com.processorchestrator.service.ProcessOrchestrator;
import com.processorchestrator.model.TaskData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for managing process execution via ProcessDetails IDs
 * Simplified to work with 3-table schema
 * 
 * This controller operates on processes using ProcessDetails IDs as the primary identifier
 */
public class ProcessController {
    private static final Logger logger = LoggerFactory.getLogger(ProcessController.class);
    
    private final ProcessRecordDAO processRecordDAO;
    private final ProcessOrchestrator processOrchestrator;

    public ProcessController(ProcessRecordDAO processRecordDAO, ProcessOrchestrator processOrchestrator) {
        this.processRecordDAO = processRecordDAO;
        this.processOrchestrator = processOrchestrator;
    }

    // ==================== PROCESS EXECUTION MANAGEMENT ====================

    /**
     * Start a process by ProcessDetails ID
     */
    public ProcessStartResponse startProcess(String processRecordId) {
        logger.info("Starting process for record: {}", processRecordId);
        
        // Get the process record
        Optional<ProcessDetails> recordOpt = processRecordDAO.findById(processRecordId);
        if (!recordOpt.isPresent()) {
            return ProcessStartResponse.error("Process record not found: " + processRecordId);
        }
        
        ProcessDetails record = recordOpt.get();
        
        // Check if process can be started
        if (!record.canBeStarted()) {
            return ProcessStartResponse.error("Process is already running: " + processRecordId);
        }
        
        try {
            // Parse input data
            ProcessInputData inputData = parseInputData(record.getInputData());
            
            // Generate unique process ID for this execution
            String processId = generateProcessId(processRecordId);
            
            // Update process record status
            processRecordDAO.updateStatus(processRecordId, "IN_PROGRESS", Instant.now(), null);
            
            // Start the process using the orchestrator
            String orchestratorProcessId = processOrchestrator.startProcess(record.getType(), inputData);
            
            logger.info("Process {} started successfully with orchestrator ID: {}", processRecordId, orchestratorProcessId);
            
            return ProcessStartResponse.success("Process started successfully", processId, orchestratorProcessId);
            
        } catch (Exception e) {
            logger.error("Failed to start process: {}", processRecordId, e);
            
            // Update process record status to failed
            processRecordDAO.updateStatus(processRecordId, "FAILED", Instant.now(), e.getMessage());
            
            return ProcessStartResponse.error("Failed to start process: " + e.getMessage());
        }
    }

    /**
     * Stop a process by ProcessDetails ID
     */
    public ProcessStopResponse stopProcess(String processRecordId) {
        logger.info("Stopping process for record: {}", processRecordId);
        
        // Get the process record
        Optional<ProcessDetails> recordOpt = processRecordDAO.findById(processRecordId);
        if (!recordOpt.isPresent()) {
            return ProcessStopResponse.error("Process record not found: " + processRecordId);
        }
        
        ProcessDetails record = recordOpt.get();
        
        // Check if process can be stopped
        if (!record.canBeStopped()) {
            return ProcessStopResponse.error("Process is not running: " + processRecordId);
        }
        
        try {
            // Update process record status to stopped
            processRecordDAO.updateStatus(processRecordId, "STOPPED", Instant.now(), null);
            
            logger.info("Process {} stopped successfully", processRecordId);
            
            return ProcessStopResponse.success("Process stopped successfully", processRecordId);
            
        } catch (Exception e) {
            logger.error("Failed to stop process: {}", processRecordId, e);
            return ProcessStopResponse.error("Failed to stop process: " + e.getMessage());
        }
    }

    /**
     * Get process state by ProcessDetails ID
     */
    public ProcessStateResponse getProcessState(String processRecordId) {
        logger.info("Getting process state for record: {}", processRecordId);
        
        // Get the process record
        Optional<ProcessDetails> recordOpt = processRecordDAO.findById(processRecordId);
        if (!recordOpt.isPresent()) {
            return ProcessStateResponse.error("Process record not found: " + processRecordId);
        }
        
        ProcessDetails record = recordOpt.get();
        
        // Get active process details if running
        List<TaskData> tasks = null;
        
        if (record.isRunning()) {
            try {
                // Try to get tasks from the orchestrator (simplified - using process record ID)
                tasks = processOrchestrator.getProcessTasks(processRecordId);
            } catch (Exception e) {
                logger.warn("Could not retrieve task details for process: {}", processRecordId, e);
            }
        }
        
        return ProcessStateResponse.success(record, tasks, "Process state retrieved successfully");
    }

    /**
     * Restart a process by ProcessDetails ID
     */
    public ProcessStartResponse restartProcess(String processRecordId) {
        logger.info("Restarting process for record: {}", processRecordId);
        
        // Get the process record
        Optional<ProcessDetails> recordOpt = processRecordDAO.findById(processRecordId);
        if (!recordOpt.isPresent()) {
            return ProcessStartResponse.error("Process record not found: " + processRecordId);
        }
        
        ProcessDetails record = recordOpt.get();
        
        // If currently running, stop it first
        if (record.isRunning()) {
            ProcessStopResponse stopResponse = stopProcess(processRecordId);
            if (!stopResponse.isSuccess()) {
                return ProcessStartResponse.error("Failed to stop running process: " + stopResponse.getMessage());
            }
            
            // Wait a moment for cleanup
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ProcessStartResponse.error("Interrupted while stopping process");
            }
        }
        
        // Start the process
        return startProcess(processRecordId);
    }

    /**
     * Get process execution history for a ProcessDetails
     */
    public ProcessHistoryResponse getProcessHistory(String processRecordId) {
        logger.info("Getting process history for record: {}", processRecordId);
        
        // Get the process record
        Optional<ProcessDetails> recordOpt = processRecordDAO.findById(processRecordId);
        if (!recordOpt.isPresent()) {
            return ProcessHistoryResponse.error("Process record not found: " + processRecordId);
        }
        
        ProcessDetails record = recordOpt.get();
        
        // Build execution history from the record
        ProcessExecutionHistory history = new ProcessExecutionHistory();
        history.setProcessDetailsId(processRecordId);
        history.setCurrentStatus(record.getCurrentStatus());
        history.setCurrentProcessId(processRecordId);
        history.setStartedWhen(record.getStartedWhen());
        history.setCompletedWhen(record.getCompletedWhen());
        history.setFailedWhen(record.getFailedWhen());
        history.setStoppedWhen(record.getStoppedWhen());
        history.setLastErrorMessage(record.getLastErrorMessage());
        history.setCreatedAt(record.getCreatedAt());
        history.setUpdatedAt(record.getUpdatedAt());
        
        return ProcessHistoryResponse.success(history, "Process history retrieved successfully");
    }

    // ==================== HELPER METHODS ====================

    /**
     * Generate a unique process ID for execution
     */
    private String generateProcessId(String processRecordId) {
        return processRecordId + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Parse input data JSON string to ProcessInputData object
     */
    private ProcessInputData parseInputData(String inputDataJson) {
        try {
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

    // ==================== RESPONSE CLASSES ====================

    public static class ProcessStartResponse {
        private final boolean success;
        private final String message;
        private final String processId;
        private final String orchestratorProcessId;

        private ProcessStartResponse(boolean success, String message, String processId, String orchestratorProcessId) {
            this.success = success;
            this.message = message;
            this.processId = processId;
            this.orchestratorProcessId = orchestratorProcessId;
        }

        public static ProcessStartResponse success(String message, String processId, String orchestratorProcessId) {
            return new ProcessStartResponse(true, message, processId, orchestratorProcessId);
        }

        public static ProcessStartResponse error(String message) {
            return new ProcessStartResponse(false, message, null, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getProcessId() { return processId; }
        public String getOrchestratorProcessId() { return orchestratorProcessId; }
    }

    public static class ProcessStopResponse {
        private final boolean success;
        private final String message;
        private final String processId;

        private ProcessStopResponse(boolean success, String message, String processId) {
            this.success = success;
            this.message = message;
            this.processId = processId;
        }

        public static ProcessStopResponse success(String message, String processId) {
            return new ProcessStopResponse(true, message, processId);
        }

        public static ProcessStopResponse error(String message) {
            return new ProcessStopResponse(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getProcessId() { return processId; }
    }

    public static class ProcessStateResponse {
        private final boolean success;
        private final String message;
        private final ProcessDetails processRecord;
        private final List<TaskData> tasks;

        private ProcessStateResponse(boolean success, String message, ProcessDetails processRecord, List<TaskData> tasks) {
            this.success = success;
            this.message = message;
            this.processRecord = processRecord;
            this.tasks = tasks;
        }

        public static ProcessStateResponse success(ProcessDetails processRecord, List<TaskData> tasks, String message) {
            return new ProcessStateResponse(true, message, processRecord, tasks);
        }

        public static ProcessStateResponse error(String message) {
            return new ProcessStateResponse(false, message, null, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public ProcessDetails getProcessDetails() { return processRecord; }
        public ProcessDetails getProcessRecord() { return processRecord; } // Alias for compatibility
        public List<TaskData> getTasks() { return tasks; }
    }

    public static class ProcessHistoryResponse {
        private final boolean success;
        private final String message;
        private final ProcessExecutionHistory history;

        private ProcessHistoryResponse(boolean success, String message, ProcessExecutionHistory history) {
            this.success = success;
            this.message = message;
            this.history = history;
        }

        public static ProcessHistoryResponse success(ProcessExecutionHistory history, String message) {
            return new ProcessHistoryResponse(true, message, history);
        }

        public static ProcessHistoryResponse error(String message) {
            return new ProcessHistoryResponse(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public ProcessExecutionHistory getHistory() { return history; }
    }

    public static class ProcessExecutionHistory {
        private String processRecordId;
        private String currentStatus;
        private String currentProcessId;
        private Instant startedWhen;
        private Instant completedWhen;
        private Instant failedWhen;
        private Instant stoppedWhen;
        private String lastErrorMessage;
        private Instant createdAt;
        private Instant updatedAt;

        // Getters and setters
        public String getProcessDetailsId() { return processRecordId; }
        public void setProcessDetailsId(String processRecordId) { this.processRecordId = processRecordId; }
        public String getProcessRecordId() { return processRecordId; } // Alias for compatibility
        
        public String getCurrentStatus() { return currentStatus; }
        public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
        
        public String getCurrentProcessId() { return currentProcessId; }
        public void setCurrentProcessId(String currentProcessId) { this.currentProcessId = currentProcessId; }
        
        public Instant getStartedWhen() { return startedWhen; }
        public void setStartedWhen(Instant startedWhen) { this.startedWhen = startedWhen; }
        
        public Instant getCompletedWhen() { return completedWhen; }
        public void setCompletedWhen(Instant completedWhen) { this.completedWhen = completedWhen; }
        
        public Instant getFailedWhen() { return failedWhen; }
        public void setFailedWhen(Instant failedWhen) { this.failedWhen = failedWhen; }
        
        public Instant getStoppedWhen() { return stoppedWhen; }
        public void setStoppedWhen(Instant stoppedWhen) { this.stoppedWhen = stoppedWhen; }
        
        public String getLastErrorMessage() { return lastErrorMessage; }
        public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
        
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
        
        public Instant getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    }
}