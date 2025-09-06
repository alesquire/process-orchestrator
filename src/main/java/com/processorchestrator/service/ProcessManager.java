package com.processorchestrator.service;

import com.processorchestrator.model.ProcessData;
import com.processorchestrator.model.ProcessInputData;
import com.processorchestrator.model.ProcessStatus;
import com.processorchestrator.model.TaskData;
import com.processorchestrator.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing processes stored in the database
 */
public class ProcessManager {
    private static final Logger logger = LoggerFactory.getLogger(ProcessManager.class);
    private final DataSource dataSource;

    public ProcessManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Create a new process definition in the database
     */
    public void createProcess(String id, String type, String inputData, String schedule) {
        String sql = """
            INSERT INTO processes (id, type, input_data, schedule, status, total_tasks, created_at, updated_at)
            VALUES (?, ?, ?, ?, 'PENDING', 0, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            Instant now = Instant.now();
            stmt.setString(1, id);
            stmt.setString(2, type);
            stmt.setString(3, inputData);
            stmt.setString(4, schedule);
            stmt.setTimestamp(5, Timestamp.from(now));
            stmt.setTimestamp(6, Timestamp.from(now));
            
            stmt.executeUpdate();
            logger.info("Created process with ID: {}", id);
            
        } catch (SQLException e) {
            logger.error("Error creating process with ID: {}", id, e);
            throw new RuntimeException("Failed to create process", e);
        }
    }

    /**
     * Get a process by ID
     */
    public Optional<ProcessRecord> getProcess(String id) {
        String sql = "SELECT * FROM processes WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapRowToProcessRecord(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error retrieving process with ID: {}", id, e);
            throw new RuntimeException("Failed to retrieve process", e);
        }
        
        return Optional.empty();
    }

    /**
     * Get all processes
     */
    public List<ProcessRecord> getAllProcesses() {
        List<ProcessRecord> processes = new ArrayList<>();
        String sql = "SELECT * FROM processes ORDER BY created_at DESC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                processes.add(mapRowToProcessRecord(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error retrieving all processes", e);
            throw new RuntimeException("Failed to retrieve processes", e);
        }
        
        return processes;
    }

    /**
     * Get processes by status
     */
    public List<ProcessRecord> getProcessesByStatus(String status) {
        List<ProcessRecord> processes = new ArrayList<>();
        String sql = "SELECT * FROM processes WHERE status = ? ORDER BY created_at DESC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                processes.add(mapRowToProcessRecord(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error retrieving processes by status: {}", status, e);
            throw new RuntimeException("Failed to retrieve processes by status", e);
        }
        
        return processes;
    }

    /**
     * Get processes that should be scheduled (have cron expressions)
     */
    public List<ProcessRecord> getScheduledProcesses() {
        List<ProcessRecord> processes = new ArrayList<>();
        String sql = "SELECT * FROM processes WHERE schedule IS NOT NULL AND schedule != '' ORDER BY created_at DESC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                processes.add(mapRowToProcessRecord(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error retrieving scheduled processes", e);
            throw new RuntimeException("Failed to retrieve scheduled processes", e);
        }
        
        return processes;
    }

    /**
     * Update process status
     */
    public void updateProcessStatus(String id, String status) {
        updateProcessStatus(id, status, null);
    }

    /**
     * Update process status with error message
     */
    public void updateProcessStatus(String id, String status, String errorMessage) {
        String sql = """
            UPDATE processes 
            SET status = ?, error_message = ?, updated_at = ?
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            stmt.setString(2, errorMessage);
            stmt.setTimestamp(3, Timestamp.from(Instant.now()));
            stmt.setString(4, id);
            
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new RuntimeException("Process not found: " + id);
            }
            
            logger.info("Updated process {} status to {}", id, status);
            
        } catch (SQLException e) {
            logger.error("Error updating process status for ID: {}", id, e);
            throw new RuntimeException("Failed to update process status", e);
        }
    }

    /**
     * Update process execution details
     */
    public void updateProcessExecution(String id, int currentTaskIndex, int totalTasks, 
                                     Instant startedAt, Instant completedAt) {
        String sql = """
            UPDATE processes 
            SET current_task_index = ?, total_tasks = ?, started_at = ?, completed_at = ?, updated_at = ?
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, currentTaskIndex);
            stmt.setInt(2, totalTasks);
            stmt.setTimestamp(3, startedAt != null ? Timestamp.from(startedAt) : null);
            stmt.setTimestamp(4, completedAt != null ? Timestamp.from(completedAt) : null);
            stmt.setTimestamp(5, Timestamp.from(Instant.now()));
            stmt.setString(6, id);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Error updating process execution for ID: {}", id, e);
            throw new RuntimeException("Failed to update process execution", e);
        }
    }

    /**
     * Delete a process (only if not completed)
     */
    public void deleteProcess(String id) {
        String sql = "DELETE FROM processes WHERE id = ? AND status != 'COMPLETED'";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            int rowsDeleted = stmt.executeUpdate();
            
            if (rowsDeleted == 0) {
                throw new RuntimeException("Process not found or cannot be deleted (only completed processes can be deleted): " + id);
            }
            
            logger.info("Deleted process with ID: {}", id);
            
        } catch (SQLException e) {
            logger.error("Error deleting process with ID: {}", id, e);
            throw new RuntimeException("Failed to delete process", e);
        }
    }

    /**
     * Record process execution attempt
     */
    public void recordExecution(String executionId, String processId, String triggeredBy, 
                              Instant startedAt, Instant completedAt, String status, String errorMessage) {
        String sql = """
            INSERT INTO process_executions (execution_id, process_id, execution_started_at, 
                                          execution_completed_at, execution_status, triggered_by, 
                                          error_message, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, executionId);
            stmt.setString(2, processId);
            stmt.setTimestamp(3, Timestamp.from(startedAt));
            stmt.setTimestamp(4, completedAt != null ? Timestamp.from(completedAt) : null);
            stmt.setString(5, status);
            stmt.setString(6, triggeredBy);
            stmt.setString(7, errorMessage);
            stmt.setTimestamp(8, Timestamp.from(Instant.now()));
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Error recording execution for process ID: {}", processId, e);
            throw new RuntimeException("Failed to record execution", e);
        }
    }

    /**
     * Get execution history for a process
     */
    public List<ExecutionRecord> getExecutionHistory(String processId) {
        List<ExecutionRecord> executions = new ArrayList<>();
        String sql = """
            SELECT * FROM process_executions 
            WHERE process_id = ? 
            ORDER BY execution_started_at DESC
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, processId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                executions.add(mapRowToExecutionRecord(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error retrieving execution history for process ID: {}", processId, e);
            throw new RuntimeException("Failed to retrieve execution history", e);
        }
        
        return executions;
    }

    private ProcessRecord mapRowToProcessRecord(ResultSet rs) throws SQLException {
        ProcessRecord record = new ProcessRecord();
        record.setId(rs.getString("id"));
        record.setType(rs.getString("type"));
        record.setInputData(rs.getString("input_data"));
        record.setSchedule(rs.getString("schedule"));
        record.setStatus(rs.getString("status"));
        record.setCurrentTaskIndex(rs.getInt("current_task_index"));
        record.setTotalTasks(rs.getInt("total_tasks"));
        record.setStartedAt(Optional.ofNullable(rs.getTimestamp("started_at")).map(Timestamp::toInstant).orElse(null));
        record.setCompletedAt(Optional.ofNullable(rs.getTimestamp("completed_at")).map(Timestamp::toInstant).orElse(null));
        record.setErrorMessage(rs.getString("error_message"));
        record.setCreatedAt(Optional.ofNullable(rs.getTimestamp("created_at")).map(Timestamp::toInstant).orElse(null));
        record.setUpdatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toInstant).orElse(null));
        return record;
    }

    private ExecutionRecord mapRowToExecutionRecord(ResultSet rs) throws SQLException {
        ExecutionRecord record = new ExecutionRecord();
        record.setExecutionId(rs.getString("execution_id"));
        record.setProcessId(rs.getString("process_id"));
        record.setExecutionStartedAt(rs.getTimestamp("execution_started_at").toInstant());
        record.setExecutionCompletedAt(Optional.ofNullable(rs.getTimestamp("execution_completed_at")).map(Timestamp::toInstant).orElse(null));
        record.setExecutionStatus(rs.getString("execution_status"));
        record.setTriggeredBy(rs.getString("triggered_by"));
        record.setErrorMessage(rs.getString("error_message"));
        record.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return record;
    }

    /**
     * Process record data class
     */
    public static class ProcessRecord {
        private String id;
        private String type;
        private String inputData;
        private String schedule;
        private String status;
        private int currentTaskIndex;
        private int totalTasks;
        private Instant startedAt;
        private Instant completedAt;
        private String errorMessage;
        private Instant createdAt;
        private Instant updatedAt;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getInputData() { return inputData; }
        public void setInputData(String inputData) { this.inputData = inputData; }
        
        public String getSchedule() { return schedule; }
        public void setSchedule(String schedule) { this.schedule = schedule; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public int getCurrentTaskIndex() { return currentTaskIndex; }
        public void setCurrentTaskIndex(int currentTaskIndex) { this.currentTaskIndex = currentTaskIndex; }
        
        public int getTotalTasks() { return totalTasks; }
        public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }
        
        public Instant getStartedAt() { return startedAt; }
        public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
        
        public Instant getCompletedAt() { return completedAt; }
        public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
        
        public Instant getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    }

    /**
     * Execution record data class
     */
    public static class ExecutionRecord {
        private String executionId;
        private String processId;
        private Instant executionStartedAt;
        private Instant executionCompletedAt;
        private String executionStatus;
        private String triggeredBy;
        private String errorMessage;
        private Instant createdAt;

        // Getters and setters
        public String getExecutionId() { return executionId; }
        public void setExecutionId(String executionId) { this.executionId = executionId; }
        
        public String getProcessId() { return processId; }
        public void setProcessId(String processId) { this.processId = processId; }
        
        public Instant getExecutionStartedAt() { return executionStartedAt; }
        public void setExecutionStartedAt(Instant executionStartedAt) { this.executionStartedAt = executionStartedAt; }
        
        public Instant getExecutionCompletedAt() { return executionCompletedAt; }
        public void setExecutionCompletedAt(Instant executionCompletedAt) { this.executionCompletedAt = executionCompletedAt; }
        
        public String getExecutionStatus() { return executionStatus; }
        public void setExecutionStatus(String executionStatus) { this.executionStatus = executionStatus; }
        
        public String getTriggeredBy() { return triggeredBy; }
        public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }
}
