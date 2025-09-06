package com.processorchestrator.controller;

import com.processorchestrator.dao.ProcessRecordDAO;
import com.processorchestrator.model.ProcessDetails;
import com.processorchestrator.model.ProcessRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Controller for ProcessRecord CRUD operations
 * Provides a clean API for managing process records
 */
public class ProcessRecordController {
    private static final Logger logger = LoggerFactory.getLogger(ProcessRecordController.class);
    
    private final ProcessRecordDAO processRecordDAO;

    public ProcessRecordController(ProcessRecordDAO processRecordDAO) {
        this.processRecordDAO = processRecordDAO;
    }

    // ==================== CRUD OPERATIONS ====================

    /**
     * Create a new process record
     */
    public ProcessRecordResponse createProcessRecord(String id, String type, String inputData, String schedule) {
        try {
            logger.info("Creating process record: id={}, type={}, schedule={}", id, type, schedule);
            
            // Validate input
            if (id == null || id.trim().isEmpty()) {
                return ProcessRecordResponse.error("Process record ID cannot be null or empty");
            }
            
            if (type == null || type.trim().isEmpty()) {
                return ProcessRecordResponse.error("Process type cannot be null or empty");
            }
            
            if (inputData == null || inputData.trim().isEmpty()) {
                return ProcessRecordResponse.error("Input data cannot be null or empty");
            }
            
            // Check if record already exists
            if (processRecordDAO.exists(id)) {
                return ProcessRecordResponse.error("Process record with ID " + id + " already exists");
            }
            
            // Create the process record
            ProcessRecord record = new ProcessRecord(id, type, inputData, schedule);
            processRecordDAO.create(record);
            
            // Return ProcessDetails with default engine values
            ProcessDetails details = new ProcessDetails(record);
            
            logger.info("Successfully created process record: {}", id);
            return ProcessRecordResponse.success(details, "Process record created successfully");
            
        } catch (Exception e) {
            logger.error("Failed to create process record: {}", id, e);
            return ProcessRecordResponse.error("Failed to create process record: " + e.getMessage());
        }
    }

    /**
     * Get a process record by ID
     */
    public ProcessRecordResponse getProcessRecord(String id) {
        try {
            logger.info("Getting process record: {}", id);
            
            Optional<ProcessDetails> details = processRecordDAO.findById(id);
            
            if (details.isPresent()) {
                return ProcessRecordResponse.success(details.get(), "Process record retrieved successfully");
            } else {
                return ProcessRecordResponse.error("Process record not found: " + id);
            }
            
        } catch (Exception e) {
            logger.error("Failed to get process record: {}", id, e);
            return ProcessRecordResponse.error("Failed to get process record: " + e.getMessage());
        }
    }

    /**
     * Get all process records
     */
    public ProcessRecordListResponse getAllProcessRecords() {
        try {
            logger.info("Getting all process records");
            
            List<ProcessDetails> records = processRecordDAO.findAll();
            
            return ProcessRecordListResponse.success(records, "Process records retrieved successfully");
            
        } catch (Exception e) {
            logger.error("Failed to get all process records", e);
            return ProcessRecordListResponse.error("Failed to get all process records: " + e.getMessage());
        }
    }

    /**
     * Get process records by status
     */
    public ProcessRecordListResponse getProcessRecordsByStatus(String status) {
        try {
            logger.info("Getting process records by status: {}", status);
            
            List<ProcessDetails> records = processRecordDAO.findByStatus(status);
            
            return ProcessRecordListResponse.success(records, "Process records retrieved successfully");
            
        } catch (Exception e) {
            logger.error("Failed to get process records by status: {}", status, e);
            return ProcessRecordListResponse.error("Failed to get process records by status: " + e.getMessage());
        }
    }

    /**
     * Get scheduled process records
     */
    public ProcessRecordListResponse getScheduledProcessRecords() {
        try {
            logger.info("Getting scheduled process records");
            
            List<ProcessDetails> records = processRecordDAO.findScheduled();
            
            return ProcessRecordListResponse.success(records, "Scheduled process records retrieved successfully");
            
        } catch (Exception e) {
            logger.error("Failed to get scheduled process records", e);
            return ProcessRecordListResponse.error("Failed to get scheduled process records: " + e.getMessage());
        }
    }

    /**
     * Update a process record (user-managed fields only)
     */
    public ProcessRecordResponse updateProcessRecord(String id, String type, String inputData, String schedule) {
        try {
            logger.info("Updating process record: {}", id);
            
            // Get existing record
            Optional<ProcessDetails> existingDetails = processRecordDAO.findById(id);
            if (!existingDetails.isPresent()) {
                return ProcessRecordResponse.error("Process record not found: " + id);
            }
            
            ProcessDetails existing = existingDetails.get();
            
            // Create updated ProcessRecord with user-managed fields
            ProcessRecord updatedRecord = new ProcessRecord(id, type, inputData, schedule);
            
            // Save updated record
            processRecordDAO.update(updatedRecord);
            
            // Return updated ProcessDetails
            ProcessDetails updatedDetails = new ProcessDetails(
                updatedRecord,
                existing.getCurrentStatus(),
                existing.getCurrentTaskIndex(),
                existing.getTotalTasks(),
                existing.getStartedWhen(),
                existing.getCompletedWhen(),
                existing.getFailedWhen(),
                existing.getStoppedWhen(),
                existing.getLastErrorMessage(),
                existing.getTriggeredBy(),
                existing.getCreatedAt(),
                existing.getUpdatedAt()
            );
            
            logger.info("Successfully updated process record: {}", id);
            return ProcessRecordResponse.success(updatedDetails, "Process record updated successfully");
            
        } catch (Exception e) {
            logger.error("Failed to update process record: {}", id, e);
            return ProcessRecordResponse.error("Failed to update process record: " + e.getMessage());
        }
    }

    /**
     * Delete a process record
     */
    public ProcessRecordDeleteResponse deleteProcessRecord(String id) {
        try {
            logger.info("Deleting process record: {}", id);
            
            // Check if record exists
            Optional<ProcessDetails> details = processRecordDAO.findById(id);
            if (!details.isPresent()) {
                return ProcessRecordDeleteResponse.error("Process record not found: " + id);
            }
            
            // Check if record can be deleted (not running)
            if (!details.get().canBeDeleted()) {
                return ProcessRecordDeleteResponse.error("Cannot delete running process record: " + id);
            }
            
            // Delete the record
            processRecordDAO.delete(id);
            
            logger.info("Successfully deleted process record: {}", id);
            return ProcessRecordDeleteResponse.success("Process record deleted successfully");
            
        } catch (Exception e) {
            logger.error("Failed to delete process record: {}", id, e);
            return ProcessRecordDeleteResponse.error("Failed to delete process record: " + e.getMessage());
        }
    }

    // ==================== UTILITY OPERATIONS ====================

    /**
     * Check if a process record exists
     */
    public boolean exists(String id) {
        return processRecordDAO.exists(id);
    }

    /**
     * Count process records by status
     */
    public long countByStatus(String status) {
        return processRecordDAO.countByStatus(status);
    }

    /**
     * Get process record statistics
     */
    public ProcessRecordStatsResponse getStatistics() {
        try {
            logger.info("Getting process record statistics");
            
            long total = processRecordDAO.findAll().size();
            long pending = processRecordDAO.countByStatus("PENDING");
            long inProgress = processRecordDAO.countByStatus("IN_PROGRESS");
            long completed = processRecordDAO.countByStatus("COMPLETED");
            long failed = processRecordDAO.countByStatus("FAILED");
            long stopped = processRecordDAO.countByStatus("STOPPED");
            long scheduled = processRecordDAO.findScheduled().size();
            
            ProcessRecordStats stats = new ProcessRecordStats(total, pending, inProgress, completed, failed, stopped, scheduled);
            
            return ProcessRecordStatsResponse.success(stats, "Statistics retrieved successfully");
            
        } catch (Exception e) {
            logger.error("Failed to get process record statistics", e);
            return ProcessRecordStatsResponse.error("Failed to get statistics: " + e.getMessage());
        }
    }

    // ==================== RESPONSE CLASSES ====================

    public static class ProcessRecordResponse {
        private final boolean success;
        private final String message;
        private final ProcessDetails data;
        private final long timestamp;

        private ProcessRecordResponse(boolean success, String message, ProcessDetails data) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public static ProcessRecordResponse success(ProcessDetails data, String message) {
            return new ProcessRecordResponse(true, message, data);
        }

        public static ProcessRecordResponse error(String message) {
            return new ProcessRecordResponse(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public ProcessDetails getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }

    public static class ProcessRecordListResponse {
        private final boolean success;
        private final String message;
        private final List<ProcessDetails> data;
        private final long timestamp;

        private ProcessRecordListResponse(boolean success, String message, List<ProcessDetails> data) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public static ProcessRecordListResponse success(List<ProcessDetails> data, String message) {
            return new ProcessRecordListResponse(true, message, data);
        }

        public static ProcessRecordListResponse error(String message) {
            return new ProcessRecordListResponse(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public List<ProcessDetails> getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }

    public static class ProcessRecordDeleteResponse {
        private final boolean success;
        private final String message;
        private final long timestamp;

        private ProcessRecordDeleteResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public static ProcessRecordDeleteResponse success(String message) {
            return new ProcessRecordDeleteResponse(true, message);
        }

        public static ProcessRecordDeleteResponse error(String message) {
            return new ProcessRecordDeleteResponse(false, message);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }

    public static class ProcessRecordStatsResponse {
        private final boolean success;
        private final String message;
        private final ProcessRecordStats data;
        private final long timestamp;

        private ProcessRecordStatsResponse(boolean success, String message, ProcessRecordStats data) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public static ProcessRecordStatsResponse success(ProcessRecordStats data, String message) {
            return new ProcessRecordStatsResponse(true, message, data);
        }

        public static ProcessRecordStatsResponse error(String message) {
            return new ProcessRecordStatsResponse(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public ProcessRecordStats getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }

    public static class ProcessRecordStats {
        private final long total;
        private final long pending;
        private final long inProgress;
        private final long completed;
        private final long failed;
        private final long stopped;
        private final long scheduled;

        public ProcessRecordStats(long total, long pending, long inProgress, long completed, 
                                long failed, long stopped, long scheduled) {
            this.total = total;
            this.pending = pending;
            this.inProgress = inProgress;
            this.completed = completed;
            this.failed = failed;
            this.stopped = stopped;
            this.scheduled = scheduled;
        }

        public long getTotal() { return total; }
        public long getPending() { return pending; }
        public long getInProgress() { return inProgress; }
        public long getCompleted() { return completed; }
        public long getFailed() { return failed; }
        public long getStopped() { return stopped; }
        public long getScheduled() { return scheduled; }
        public long getManual() { return total - scheduled; }
    }
}
