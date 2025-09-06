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
 * Service for managing process definitions and active processes
 * 
 * Process Definitions: User-managed templates with execution history
 * Processes: Engine-managed active execution instances
 */
public class ProcessManager {
    private static final Logger logger = LoggerFactory.getLogger(ProcessManager.class);
    private final DataSource dataSource;

    public ProcessManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ==================== PROCESS DEFINITIONS MANAGEMENT ====================

    /**
     * Create a new process definition (user-managed)
     */
    public void createProcessDefinition(String id, String type, String inputData, String schedule) {
        String sql = """
            INSERT INTO process_definitions (id, type, input_data, schedule, current_status, created_at, updated_at)
            VALUES (?, ?, ?, ?, 'PENDING', ?, ?)
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
            logger.info("Created process definition with ID: {}", id);
            
        } catch (SQLException e) {
            logger.error("Error creating process definition with ID: {}", id, e);
            throw new RuntimeException("Failed to create process definition", e);
        }
    }

    /**
     * Get a process definition by ID
     */
    public Optional<ProcessDefinition> getProcessDefinition(String id) {
        String sql = "SELECT * FROM process_definitions WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapRowToProcessDefinition(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error retrieving process definition with ID: {}", id, e);
            throw new RuntimeException("Failed to retrieve process definition", e);
        }
        
        return Optional.empty();
    }

    /**
     * Get all process definitions
     */
    public List<ProcessDefinition> getAllProcessDefinitions() {
        List<ProcessDefinition> definitions = new ArrayList<>();
        String sql = "SELECT * FROM process_definitions ORDER BY created_at DESC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                definitions.add(mapRowToProcessDefinition(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error retrieving all process definitions", e);
            throw new RuntimeException("Failed to retrieve process definitions", e);
        }
        
        return definitions;
    }

    /**
     * Get process definitions by status
     */
    public List<ProcessDefinition> getProcessDefinitionsByStatus(String status) {
        List<ProcessDefinition> definitions = new ArrayList<>();
        String sql = "SELECT * FROM process_definitions WHERE current_status = ? ORDER BY created_at DESC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                definitions.add(mapRowToProcessDefinition(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error retrieving process definitions by status: {}", status, e);
            throw new RuntimeException("Failed to retrieve process definitions by status", e);
        }
        
        return definitions;
    }

    /**
     * Get scheduled process definitions
     */
    public List<ProcessDefinition> getScheduledProcessDefinitions() {
        List<ProcessDefinition> definitions = new ArrayList<>();
        String sql = "SELECT * FROM process_definitions WHERE schedule IS NOT NULL AND schedule != '' ORDER BY created_at DESC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                definitions.add(mapRowToProcessDefinition(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error retrieving scheduled process definitions", e);
            throw new RuntimeException("Failed to retrieve scheduled process definitions", e);
        }
        
        return definitions;
    }

    /**
     * Update process definition status and execution statistics
     */
    public void updateProcessDefinitionStatus(String definitionId, String status, String currentProcessId) {
        updateProcessDefinitionStatus(definitionId, status, currentProcessId, null, null);
    }

    /**
     * Update process definition with execution statistics
     */
    public void updateProcessDefinitionStatus(String definitionId, String status, String currentProcessId, 
                                            Instant executionTime, String errorMessage) {
        String sql = """
            UPDATE process_definitions 
            SET current_status = ?, current_process_id = ?, updated_at = ?
            """;
        
        // Add execution time fields based on status
        switch (status.toUpperCase()) {
            case "IN_PROGRESS":
                sql += ", started_when = ?";
                break;
            case "COMPLETED":
                sql += ", completed_when = ?";
                break;
            case "FAILED":
                sql += ", failed_when = ?, last_error_message = ?";
                break;
            case "STOPPED":
                sql += ", stopped_when = ?";
                break;
        }
        
        sql += " WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            stmt.setString(paramIndex++, status);
            stmt.setString(paramIndex++, currentProcessId);
            stmt.setTimestamp(paramIndex++, Timestamp.from(Instant.now()));
            
            // Add execution time based on status
            if (executionTime != null) {
                switch (status.toUpperCase()) {
                    case "IN_PROGRESS":
                        stmt.setTimestamp(paramIndex++, Timestamp.from(executionTime));
                        break;
                    case "COMPLETED":
                        stmt.setTimestamp(paramIndex++, Timestamp.from(executionTime));
                        break;
                    case "FAILED":
                        stmt.setTimestamp(paramIndex++, Timestamp.from(executionTime));
                        stmt.setString(paramIndex++, errorMessage);
                        break;
                    case "STOPPED":
                        stmt.setTimestamp(paramIndex++, Timestamp.from(executionTime));
                        break;
                }
            }
            
            stmt.setString(paramIndex, definitionId);
            
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new RuntimeException("Process definition not found: " + definitionId);
            }
            
            logger.info("Updated process definition {} status to {}", definitionId, status);
            
        } catch (SQLException e) {
            logger.error("Error updating process definition status for ID: {}", definitionId, e);
            throw new RuntimeException("Failed to update process definition status", e);
        }
    }

    /**
     * Delete a process definition (manual deletion only)
     */
    public void deleteProcessDefinition(String id) {
        String sql = "DELETE FROM process_definitions WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            int rowsDeleted = stmt.executeUpdate();
            
            if (rowsDeleted == 0) {
                throw new RuntimeException("Process definition not found: " + id);
            }
            
            logger.info("Deleted process definition with ID: {}", id);
            
        } catch (SQLException e) {
            logger.error("Error deleting process definition with ID: {}", id, e);
            throw new RuntimeException("Failed to delete process definition", e);
        }
    }

    // ==================== ACTIVE PROCESSES MANAGEMENT ====================

    /**
     * Create an active process instance (engine-managed)
     */
    public void createActiveProcess(String processId, String definitionId, String type, String inputData) {
        String sql = """
            INSERT INTO processes (id, definition_id, type, input_data, status, total_tasks, created_at, updated_at)
            VALUES (?, ?, ?, ?, 'PENDING', 0, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            Instant now = Instant.now();
            stmt.setString(1, processId);
            stmt.setString(2, definitionId);
            stmt.setString(3, type);
            stmt.setString(4, inputData);
            stmt.setTimestamp(5, Timestamp.from(now));
            stmt.setTimestamp(6, Timestamp.from(now));
            
            stmt.executeUpdate();
            logger.info("Created active process with ID: {}", processId);
            
        } catch (SQLException e) {
            logger.error("Error creating active process with ID: {}", processId, e);
            throw new RuntimeException("Failed to create active process", e);
        }
    }

    /**
     * Get an active process by ID
     */
    public Optional<ActiveProcess> getActiveProcess(String processId) {
        String sql = "SELECT * FROM processes WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, processId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapRowToActiveProcess(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error retrieving active process with ID: {}", processId, e);
            throw new RuntimeException("Failed to retrieve active process", e);
        }
        
        return Optional.empty();
    }

    /**
     * Update active process status
     */
    public void updateActiveProcessStatus(String processId, String status) {
        updateActiveProcessStatus(processId, status, null);
    }

    /**
     * Update active process status with error message
     */
    public void updateActiveProcessStatus(String processId, String status, String errorMessage) {
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
            stmt.setString(4, processId);
            
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new RuntimeException("Active process not found: " + processId);
            }
            
            logger.info("Updated active process {} status to {}", processId, status);
            
        } catch (SQLException e) {
            logger.error("Error updating active process status for ID: {}", processId, e);
            throw new RuntimeException("Failed to update active process status", e);
        }
    }

    /**
     * Update active process execution details
     */
    public void updateActiveProcessExecution(String processId, int currentTaskIndex, int totalTasks, 
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
            stmt.setString(6, processId);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Error updating active process execution for ID: {}", processId, e);
            throw new RuntimeException("Failed to update active process execution", e);
        }
    }

    /**
     * Delete an active process (engine cleanup)
     */
    public void deleteActiveProcess(String processId) {
        String sql = "DELETE FROM processes WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, processId);
            int rowsDeleted = stmt.executeUpdate();
            
            if (rowsDeleted == 0) {
                throw new RuntimeException("Active process not found: " + processId);
            }
            
            logger.info("Deleted active process with ID: {}", processId);
            
        } catch (SQLException e) {
            logger.error("Error deleting active process with ID: {}", processId, e);
            throw new RuntimeException("Failed to delete active process", e);
        }
    }

    // ==================== HELPER METHODS ====================

    private ProcessDefinition mapRowToProcessDefinition(ResultSet rs) throws SQLException {
        ProcessDefinition definition = new ProcessDefinition();
        definition.setId(rs.getString("id"));
        definition.setType(rs.getString("type"));
        definition.setInputData(rs.getString("input_data"));
        definition.setSchedule(rs.getString("schedule"));
        definition.setCurrentStatus(rs.getString("current_status"));
        definition.setCurrentProcessId(rs.getString("current_process_id"));
        definition.setStartedWhen(Optional.ofNullable(rs.getTimestamp("started_when")).map(Timestamp::toInstant).orElse(null));
        definition.setCompletedWhen(Optional.ofNullable(rs.getTimestamp("completed_when")).map(Timestamp::toInstant).orElse(null));
        definition.setFailedWhen(Optional.ofNullable(rs.getTimestamp("failed_when")).map(Timestamp::toInstant).orElse(null));
        definition.setStoppedWhen(Optional.ofNullable(rs.getTimestamp("stopped_when")).map(Timestamp::toInstant).orElse(null));
        definition.setLastErrorMessage(rs.getString("last_error_message"));
        definition.setCreatedAt(Optional.ofNullable(rs.getTimestamp("created_at")).map(Timestamp::toInstant).orElse(null));
        definition.setUpdatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toInstant).orElse(null));
        return definition;
    }

    private ActiveProcess mapRowToActiveProcess(ResultSet rs) throws SQLException {
        ActiveProcess process = new ActiveProcess();
        process.setId(rs.getString("id"));
        process.setDefinitionId(rs.getString("definition_id"));
        process.setType(rs.getString("type"));
        process.setInputData(rs.getString("input_data"));
        process.setStatus(rs.getString("status"));
        process.setCurrentTaskIndex(rs.getInt("current_task_index"));
        process.setTotalTasks(rs.getInt("total_tasks"));
        process.setStartedAt(Optional.ofNullable(rs.getTimestamp("started_at")).map(Timestamp::toInstant).orElse(null));
        process.setCompletedAt(Optional.ofNullable(rs.getTimestamp("completed_at")).map(Timestamp::toInstant).orElse(null));
        process.setErrorMessage(rs.getString("error_message"));
        process.setCreatedAt(Optional.ofNullable(rs.getTimestamp("created_at")).map(Timestamp::toInstant).orElse(null));
        process.setUpdatedAt(Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toInstant).orElse(null));
        return process;
    }

    // ==================== DATA CLASSES ====================

    /**
     * Process Definition - User-managed template with execution history
     */
    public static class ProcessDefinition {
        private String id;
        private String type;
        private String inputData;
        private String schedule;
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
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getInputData() { return inputData; }
        public void setInputData(String inputData) { this.inputData = inputData; }
        
        public String getSchedule() { return schedule; }
        public void setSchedule(String schedule) { this.schedule = schedule; }
        
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

    /**
     * Active Process - Engine-managed execution instance
     */
    public static class ActiveProcess {
        private String id;
        private String definitionId;
        private String type;
        private String inputData;
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
        
        public String getDefinitionId() { return definitionId; }
        public void setDefinitionId(String definitionId) { this.definitionId = definitionId; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getInputData() { return inputData; }
        public void setInputData(String inputData) { this.inputData = inputData; }
        
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
}