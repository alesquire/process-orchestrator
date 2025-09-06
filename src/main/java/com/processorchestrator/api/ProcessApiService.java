package com.processorchestrator.api;

import com.processorchestrator.controller.ProcessController;
import com.processorchestrator.service.ProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Simple API service demonstrating how to use the Process Controller
 * This could be extended to create REST endpoints or other API interfaces
 */
public class ProcessApiService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessApiService.class);
    
    private final ProcessController processController;
    private final ProcessManager processManager;

    public ProcessApiService(ProcessController processController, ProcessManager processManager) {
        this.processController = processController;
        this.processManager = processManager;
    }

    /**
     * API endpoint: Create a new process
     */
    public ApiResponse<ProcessManager.ProcessRecord> createProcess(String id, String type, String inputData, String schedule) {
        try {
            logger.info("API: Creating process - id={}, type={}, schedule={}", id, type, schedule);
            
            ProcessManager.ProcessRecord process = processController.createProcess(id, type, inputData, schedule);
            
            return ApiResponse.success(process, "Process created successfully");
            
        } catch (Exception e) {
            logger.error("API: Failed to create process", e);
            return ApiResponse.error("Failed to create process: " + e.getMessage());
        }
    }

    /**
     * API endpoint: Start a process
     */
    public ApiResponse<ProcessController.ProcessStartResponse> startProcess(String id) {
        try {
            logger.info("API: Starting process - id={}", id);
            
            ProcessController.ProcessStartResponse response = processController.startProcess(id);
            
            return ApiResponse.success(response, response.getMessage());
            
        } catch (Exception e) {
            logger.error("API: Failed to start process", e);
            return ApiResponse.error("Failed to start process: " + e.getMessage());
        }
    }

    /**
     * API endpoint: Stop a process
     */
    public ApiResponse<ProcessController.ProcessStopResponse> stopProcess(String id) {
        try {
            logger.info("API: Stopping process - id={}", id);
            
            ProcessController.ProcessStopResponse response = processController.stopProcess(id);
            
            return ApiResponse.success(response, response.getMessage());
            
        } catch (Exception e) {
            logger.error("API: Failed to stop process", e);
            return ApiResponse.error("Failed to stop process: " + e.getMessage());
        }
    }

    /**
     * API endpoint: Get process state
     */
    public ApiResponse<ProcessController.ProcessStateResponse> getProcessState(String id) {
        try {
            logger.info("API: Getting process state - id={}", id);
            
            ProcessController.ProcessStateResponse response = processController.getProcessState(id);
            
            return ApiResponse.success(response, "Process state retrieved successfully");
            
        } catch (Exception e) {
            logger.error("API: Failed to get process state", e);
            return ApiResponse.error("Failed to get process state: " + e.getMessage());
        }
    }

    /**
     * API endpoint: Get all processes
     */
    public ApiResponse<List<ProcessManager.ProcessRecord>> getAllProcesses() {
        try {
            logger.info("API: Getting all processes");
            
            List<ProcessManager.ProcessRecord> processes = processController.getAllProcesses();
            
            return ApiResponse.success(processes, "Processes retrieved successfully");
            
        } catch (Exception e) {
            logger.error("API: Failed to get all processes", e);
            return ApiResponse.error("Failed to get all processes: " + e.getMessage());
        }
    }

    /**
     * API endpoint: Get processes by status
     */
    public ApiResponse<List<ProcessManager.ProcessRecord>> getProcessesByStatus(String status) {
        try {
            logger.info("API: Getting processes by status - status={}", status);
            
            List<ProcessManager.ProcessRecord> processes = processController.getProcessesByStatus(status);
            
            return ApiResponse.success(processes, "Processes retrieved successfully");
            
        } catch (Exception e) {
            logger.error("API: Failed to get processes by status", e);
            return ApiResponse.error("Failed to get processes by status: " + e.getMessage());
        }
    }

    /**
     * API endpoint: Delete a process
     */
    public ApiResponse<ProcessController.ProcessDeleteResponse> deleteProcess(String id) {
        try {
            logger.info("API: Deleting process - id={}", id);
            
            ProcessController.ProcessDeleteResponse response = processController.deleteProcess(id);
            
            return ApiResponse.success(response, response.getMessage());
            
        } catch (Exception e) {
            logger.error("API: Failed to delete process", e);
            return ApiResponse.error("Failed to delete process: " + e.getMessage());
        }
    }

    /**
     * API endpoint: Get scheduled processes
     */
    public ApiResponse<List<ProcessManager.ProcessRecord>> getScheduledProcesses() {
        try {
            logger.info("API: Getting scheduled processes");
            
            List<ProcessManager.ProcessRecord> processes = processManager.getScheduledProcesses();
            
            return ApiResponse.success(processes, "Scheduled processes retrieved successfully");
            
        } catch (Exception e) {
            logger.error("API: Failed to get scheduled processes", e);
            return ApiResponse.error("Failed to get scheduled processes: " + e.getMessage());
        }
    }

    /**
     * API endpoint: Get execution history for a process
     */
    public ApiResponse<List<ProcessManager.ExecutionRecord>> getExecutionHistory(String processId) {
        try {
            logger.info("API: Getting execution history - processId={}", processId);
            
            List<ProcessManager.ExecutionRecord> executions = processManager.getExecutionHistory(processId);
            
            return ApiResponse.success(executions, "Execution history retrieved successfully");
            
        } catch (Exception e) {
            logger.error("API: Failed to get execution history", e);
            return ApiResponse.error("Failed to get execution history: " + e.getMessage());
        }
    }

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
