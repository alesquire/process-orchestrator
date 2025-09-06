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
import java.util.UUID;

/**
 * Controller for managing processes via API calls
 * 
 * Works with the new table structure:
 * - Process Definitions: User-managed templates with execution history
 * - Active Processes: Engine-managed execution instances
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

    // ==================== PROCESS DEFINITION MANAGEMENT ====================

    /**
     * Create a new process definition (user-managed)
     */
    public ProcessManager.ProcessDefinition createProcessDefinition(String id, String type, String inputData, String schedule) {
        logger.info("Creating process definition: id={}, type={}, schedule={}", id, type, schedule);
        
        // Validate that process definition doesn't already exist
        if (processManager.getProcessDefinition(id).isPresent()) {
            throw new IllegalArgumentException("Process definition with ID " + id + " already exists");
        }
        
        // Create the process definition
        processManager.createProcessDefinition(id, type, inputData, schedule);
        
        // Return the created process definition
        return processManager.getProcessDefinition(id).orElseThrow(() -> 
            new RuntimeException("Failed to retrieve created process definition"));
    }

    /**
     * Get a process definition by ID
     */
    public ProcessManager.ProcessDefinition getProcessDefinition(String id) {
        logger.info("Getting process definition: {}", id);
        
        return processManager.getProcessDefinition(id)
            .orElseThrow(() -> new IllegalArgumentException("Process definition not found: " + id));
    }

    /**
     * Get all process definitions
     */
    public List<ProcessManager.ProcessDefinition> getAllProcessDefinitions() {
        logger.info("Getting all process definitions");
        return processManager.getAllProcessDefinitions();
    }

    /**
     * Get process definitions by status
     */
    public List<ProcessManager.ProcessDefinition> getProcessDefinitionsByStatus(String status) {
        logger.info("Getting process definitions by status: {}", status);
        return processManager.getProcessDefinitionsByStatus(status);
    }

    /**
     * Get scheduled process definitions
     */
    public List<ProcessManager.ProcessDefinition> getScheduledProcessDefinitions() {
        logger.info("Getting scheduled process definitions");
        return processManager.getScheduledProcessDefinitions();
    }

    /**
     * Delete a process definition (manual deletion only)
     */
    public ProcessDeleteResponse deleteProcessDefinition(String id) {
        logger.info("Deleting process definition: {}", id);
        
        try {
            processManager.deleteProcessDefinition(id);
            return new ProcessDeleteResponse(true, "Process definition deleted successfully");
        } catch (Exception e) {
            logger.error("Failed to delete process definition: {}", id, e);
            return new ProcessDeleteResponse(false, "Failed to delete process definition: " + e.getMessage());
        }
    }

    // ==================== PROCESS EXECUTION MANAGEMENT ====================

    /**
     * Start a process by definition ID
     */
    public ProcessStartResponse startProcess(String definitionId) {
        logger.info("Starting process for definition: {}", definitionId);
        
        // Get the process definition
        ProcessManager.ProcessDefinition definition = processManager.getProcessDefinition(definitionId)
            .orElseThrow(() -> new IllegalArgumentException("Process definition not found: " + definitionId));
        
        // Check if process can be started
        if ("IN_PROGRESS".equals(definition.getCurrentStatus())) {
            throw new IllegalStateException("Process is already running: " + definitionId);
        }
        
        if ("COMPLETED".equals(definition.getCurrentStatus())) {
            logger.info("Restarting completed process definition: {}", definitionId);
        }
        
        try {
            // Parse input data
            ProcessInputData inputData = parseInputData(definition.getInputData());
            
            // Generate unique process ID for this execution
            String processId = generateProcessId(definitionId);
            
            // Create active process instance
            processManager.createActiveProcess(processId, definitionId, definition.getType(), definition.getInputData());
            
            // Update process definition status
            processManager.updateProcessDefinitionStatus(definitionId, "IN_PROGRESS", processId, Instant.now(), null);
            
            // Start the process using the orchestrator
            String orchestratorProcessId = processOrchestrator.startProcess(definition.getType(), inputData);
            
            // Update active process with orchestrator details
            processManager.updateActiveProcessExecution(processId, 0, 0, Instant.now(), null);
            
            logger.info("Process {} started successfully with orchestrator ID: {}", definitionId, orchestratorProcessId);
            
            return new ProcessStartResponse(true, "Process started successfully", processId, orchestratorProcessId);
            
        } catch (Exception e) {
            logger.error("Failed to start process: {}", definitionId, e);
            
            // Update process definition status to failed
            processManager.updateProcessDefinitionStatus(definitionId, "FAILED", null, Instant.now(), e.getMessage());
            
            return new ProcessStartResponse(false, "Failed to start process: " + e.getMessage(), null, null);
        }
    }

    /**
     * Stop a process by definition ID
     */
    public ProcessStopResponse stopProcess(String definitionId) {
        logger.info("Stopping process for definition: {}", definitionId);
        
        // Get the process definition
        ProcessManager.ProcessDefinition definition = processManager.getProcessDefinition(definitionId)
            .orElseThrow(() -> new IllegalArgumentException("Process definition not found: " + definitionId));
        
        // Check if process can be stopped
        if (!"IN_PROGRESS".equals(definition.getCurrentStatus())) {
            throw new IllegalStateException("Process is not running: " + definitionId);
        }
        
        try {
            // Update process definition status to stopped
            processManager.updateProcessDefinitionStatus(definitionId, "STOPPED", null, Instant.now(), null);
            
            // Update active process if it exists
            if (definition.getCurrentProcessId() != null) {
                processManager.updateActiveProcessStatus(definition.getCurrentProcessId(), "STOPPED");
            }
            
            logger.info("Process {} stopped successfully", definitionId);
            
            return new ProcessStopResponse(true, "Process stopped successfully", definition.getCurrentProcessId());
            
        } catch (Exception e) {
            logger.error("Failed to stop process: {}", definitionId, e);
            return new ProcessStopResponse(false, "Failed to stop process: " + e.getMessage(), null);
        }
    }

    /**
     * Get process state by definition ID
     */
    public ProcessStateResponse getProcessState(String definitionId) {
        logger.info("Getting process state for definition: {}", definitionId);
        
        // Get the process definition
        ProcessManager.ProcessDefinition definition = processManager.getProcessDefinition(definitionId)
            .orElseThrow(() -> new IllegalArgumentException("Process definition not found: " + definitionId));
        
        // Get active process details if running
        ProcessManager.ActiveProcess activeProcess = null;
        List<TaskData> tasks = null;
        
        if (definition.getCurrentProcessId() != null) {
            activeProcess = processManager.getActiveProcess(definition.getCurrentProcessId()).orElse(null);
            
            if (activeProcess != null && "IN_PROGRESS".equals(activeProcess.getStatus())) {
                try {
                    // Try to get tasks from the orchestrator
                    tasks = processOrchestrator.getProcessTasks(definition.getCurrentProcessId());
                } catch (Exception e) {
                    logger.warn("Could not retrieve task details for process: {}", definition.getCurrentProcessId(), e);
                }
            }
        }
        
        return new ProcessStateResponse(definition, activeProcess, tasks);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Generate a unique process ID for execution
     */
    private String generateProcessId(String definitionId) {
        return definitionId + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
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

    // ==================== RESPONSE CLASSES ====================

    public static class ProcessStartResponse {
        private final boolean success;
        private final String message;
        private final String processId;
        private final String orchestratorProcessId;

        public ProcessStartResponse(boolean success, String message, String processId, String orchestratorProcessId) {
            this.success = success;
            this.message = message;
            this.processId = processId;
            this.orchestratorProcessId = orchestratorProcessId;
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

        public ProcessStopResponse(boolean success, String message, String processId) {
            this.success = success;
            this.message = message;
            this.processId = processId;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getProcessId() { return processId; }
    }

    public static class ProcessStateResponse {
        private final ProcessManager.ProcessDefinition definition;
        private final ProcessManager.ActiveProcess activeProcess;
        private final List<TaskData> tasks;

        public ProcessStateResponse(ProcessManager.ProcessDefinition definition, 
                                  ProcessManager.ActiveProcess activeProcess, 
                                  List<TaskData> tasks) {
            this.definition = definition;
            this.activeProcess = activeProcess;
            this.tasks = tasks;
        }

        public ProcessManager.ProcessDefinition getDefinition() { return definition; }
        public ProcessManager.ActiveProcess getActiveProcess() { return activeProcess; }
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