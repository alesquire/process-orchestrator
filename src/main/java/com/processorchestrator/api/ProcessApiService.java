package com.processorchestrator.api;

import com.processorchestrator.controller.ProcessController;
import com.processorchestrator.service.ProcessManager;
import com.processorchestrator.model.TaskData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Simple API service demonstrating how to use the Process Controller
 * with the new table structure (process_definitions + processes)
 */
public class ProcessApiService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessApiService.class);
    
    private final ProcessController processController;
    private final ProcessManager processManager;

    public ProcessApiService(ProcessController processController, ProcessManager processManager) {
        this.processController = processController;
        this.processManager = processManager;
    }

    // ==================== PROCESS DEFINITION MANAGEMENT ====================

    /**
     * API endpoint: Create a new process definition
     */
    public ApiResponse<ProcessManager.ProcessDefinition> createProcessDefinition(String id, String type, String inputData, String schedule) {
        try {
            logger.info("API: Creating process definition - id={}, type={}, schedule={}", id, type, schedule);
            
            ProcessManager.ProcessDefinition definition = processController.createProcessDefinition(id, type, inputData, schedule);
            
            return ApiResponse.success(definition, "Process definition created successfully");
            
        } catch (Exception e) {
            logger.error("API: Failed to create process definition", e);
            return ApiResponse.error("Failed to create process definition: " + e.getMessage());
        }
    }

    /**
     * API endpoint: Get a process definition
     */
    public ApiResponse<ProcessManager.ProcessDefinition> getProcessDefinition(String id) {
        try {
            logger.info("API: Getting process definition - id={}", id);
            
            ProcessManager.ProcessDefinition definition = processController.getProcessDefinition(id);
            
            return ApiResponse.success(definition, "Process definition retrieved successfully");
            
        } catch (Exception e) {
            logger.error("API: Failed to get process definition", e);
            return ApiResponse.error("Failed to get process definition: " + e.getMessage());
        }
    }

    /**
     * API endpoint: Get all process definitions
     */
    public ApiResponse<List<ProcessManager.ProcessDefinition>> getAllProcessDefinitions() {
        try {
            logger.info("API: Getting all process definitions");
            
            List<ProcessManager.ProcessDefinition> definitions = processController.getAllProcessDefinitions();
            
            return ApiResponse.success(definitions, "Process definitions retrieved successfully");
            
        } catch (Exception e) {
            logger.error("API: Failed to get all process definitions", e);
            return ApiResponse.error("Failed to get all process definitions: " + e.getMessage());
        }
    }

    /**
     * API endpoint: Get process definitions by status
     */
    public ApiResponse<List<ProcessManager.ProcessDefinition>> getProcessDefinitionsByStatus(String status) {
        try {
            logger.info("API: Getting process definitions by status - status={}", status);
            
            List<ProcessManager.ProcessDefinition> definitions = processController.getProcessDefinitionsByStatus(status);
            
            return ApiResponse.success(definitions, "Process definitions retrieved successfully");
            
        } catch (Exception e) {
            logger.error("API: Failed to get process definitions by status", e);
            return ApiResponse.error("Failed to get process definitions by status: " + e.getMessage());
        }
    }

    /**
     * API endpoint: Get scheduled process definitions
     */
    public ApiResponse<List<ProcessManager.ProcessDefinition>> getScheduledProcessDefinitions() {
        try {
            logger.info("API: Getting scheduled process definitions");
            
            List<ProcessManager.ProcessDefinition> definitions = processController.getScheduledProcessDefinitions();
            
            return ApiResponse.success(definitions, "Scheduled process definitions retrieved successfully");
            
        } catch (Exception e) {
            logger.error("API: Failed to get scheduled process definitions", e);
            return ApiResponse.error("Failed to get scheduled process definitions: " + e.getMessage());
        }
    }

    /**
     * API endpoint: Delete a process definition
     */
    public ApiResponse<ProcessController.ProcessDeleteResponse> deleteProcessDefinition(String id) {
        try {
            logger.info("API: Deleting process definition - id={}", id);
            
            ProcessController.ProcessDeleteResponse response = processController.deleteProcessDefinition(id);
            
            return ApiResponse.success(response, response.getMessage());
            
        } catch (Exception e) {
            logger.error("API: Failed to delete process definition", e);
            return ApiResponse.error("Failed to delete process definition: " + e.getMessage());
        }
    }

    // ==================== PROCESS EXECUTION MANAGEMENT ====================

    /**
     * API endpoint: Start a process
     */
    public ApiResponse<ProcessController.ProcessStartResponse> startProcess(String definitionId) {
        try {
            logger.info("API: Starting process - definitionId={}", definitionId);
            
            ProcessController.ProcessStartResponse response = processController.startProcess(definitionId);
            
            return ApiResponse.success(response, response.getMessage());
            
        } catch (Exception e) {
            logger.error("API: Failed to start process", e);
            return ApiResponse.error("Failed to start process: " + e.getMessage());
        }
    }

    /**
     * API endpoint: Stop a process
     */
    public ApiResponse<ProcessController.ProcessStopResponse> stopProcess(String definitionId) {
        try {
            logger.info("API: Stopping process - definitionId={}", definitionId);
            
            ProcessController.ProcessStopResponse response = processController.stopProcess(definitionId);
            
            return ApiResponse.success(response, response.getMessage());
            
        } catch (Exception e) {
            logger.error("API: Failed to stop process", e);
            return ApiResponse.error("Failed to stop process: " + e.getMessage());
        }
    }

    /**
     * API endpoint: Get process state
     */
    public ApiResponse<ProcessController.ProcessStateResponse> getProcessState(String definitionId) {
        try {
            logger.info("API: Getting process state - definitionId={}", definitionId);
            
            ProcessController.ProcessStateResponse response = processController.getProcessState(definitionId);
            
            return ApiResponse.success(response, "Process state retrieved successfully");
            
        } catch (Exception e) {
            logger.error("API: Failed to get process state", e);
            return ApiResponse.error("Failed to get process state: " + e.getMessage());
        }
    }

    // ==================== LEGACY COMPATIBILITY METHODS ====================

    /**
     * Legacy method for backward compatibility
     * @deprecated Use createProcessDefinition instead
     */
    @Deprecated
    public ApiResponse<ProcessManager.ProcessDefinition> createProcess(String id, String type, String inputData, String schedule) {
        return createProcessDefinition(id, type, inputData, schedule);
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use getAllProcessDefinitions instead
     */
    @Deprecated
    public ApiResponse<List<ProcessManager.ProcessDefinition>> getAllProcesses() {
        return getAllProcessDefinitions();
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use getProcessDefinitionsByStatus instead
     */
    @Deprecated
    public ApiResponse<List<ProcessManager.ProcessDefinition>> getProcessesByStatus(String status) {
        return getProcessDefinitionsByStatus(status);
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use deleteProcessDefinition instead
     */
    @Deprecated
    public ApiResponse<ProcessController.ProcessDeleteResponse> deleteProcess(String id) {
        return deleteProcessDefinition(id);
    }

    // ==================== GENERIC API RESPONSE WRAPPER ====================

    /**
     * Generic API response wrapper
     */
    public static class ApiResponse<T> {
        private final boolean success;
        private final String message;
        private final T data;
        private final long timestamp;

        private ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public static <T> ApiResponse<T> success(T data, String message) {
            return new ApiResponse<>(true, message, data);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public T getData() { return data; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("ApiResponse{success=%s, message='%s', data=%s, timestamp=%d}", 
                               success, message, data, timestamp);
        }
    }
}
