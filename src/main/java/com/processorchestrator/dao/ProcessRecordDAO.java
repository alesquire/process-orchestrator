package com.processorchestrator.dao;

import com.processorchestrator.model.ProcessDetails;
import com.processorchestrator.model.ProcessRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for ProcessRecord operations
 * Handles all database operations for process records with simplified schema
 */
public class ProcessRecordDAO {
    private static final Logger logger = LoggerFactory.getLogger(ProcessRecordDAO.class);
    private final DataSource dataSource;

    public ProcessRecordDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Create a new process record
     */
    public void create(ProcessRecord processRecord) {
        String sql = """
            INSERT INTO process_record (id, type, input_data, schedule, current_status, current_task_index, total_tasks, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            Instant now = Instant.now();
            stmt.setString(1, processRecord.getId());
            stmt.setString(2, processRecord.getType());
            stmt.setString(3, processRecord.getInputData());
            stmt.setString(4, processRecord.getSchedule());
            stmt.setString(5, "PENDING"); // Default status
            stmt.setInt(6, 0); // Default task index
            stmt.setInt(7, 0); // Default total tasks
            stmt.setTimestamp(8, Timestamp.from(now));
            stmt.setTimestamp(9, Timestamp.from(now));
            
            stmt.executeUpdate();
            logger.info("Created process record with ID: {}", processRecord.getId());
            
        } catch (SQLException e) {
            logger.error("Failed to create process record: {}", processRecord.getId(), e);
            throw new RuntimeException("Failed to create process record", e);
        }
    }

    /**
     * Find a process record by ID
     */
    public Optional<ProcessDetails> findById(String id) {
        String sql = """
            SELECT id, type, input_data, schedule, current_status, current_task_index, total_tasks,
                   started_when, completed_when, failed_when, stopped_when, last_error_message, triggered_by,
                   created_at, updated_at
            FROM process_record WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ProcessDetails details = mapResultSetToProcessDetails(rs);
                    logger.debug("Found process record: {}", id);
                    return Optional.of(details);
                } else {
                    logger.debug("Process record not found: {}", id);
                    return Optional.empty();
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to find process record: {}", id, e);
            throw new RuntimeException("Failed to find process record", e);
        }
    }

    /**
     * Update a process record
     */
    public void update(ProcessRecord processRecord) {
        String sql = """
            UPDATE process_record 
            SET type = ?, input_data = ?, schedule = ?, updated_at = ?
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, processRecord.getType());
            stmt.setString(2, processRecord.getInputData());
            stmt.setString(3, processRecord.getSchedule());
            stmt.setTimestamp(4, Timestamp.from(Instant.now()));
            stmt.setString(5, processRecord.getId());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Process record not found: " + processRecord.getId());
            }
            
            logger.info("Updated process record: {}", processRecord.getId());
            
        } catch (SQLException e) {
            logger.error("Failed to update process record: {}", processRecord.getId(), e);
            throw new RuntimeException("Failed to update process record", e);
        }
    }

    /**
     * Delete a process record
     */
    public void delete(String id) {
        String sql = "DELETE FROM process_record WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Process record not found: " + id);
            }
            
            logger.info("Deleted process record: {}", id);
            
        } catch (SQLException e) {
            logger.error("Failed to delete process record: {}", id, e);
            throw new RuntimeException("Failed to delete process record", e);
        }
    }

    /**
     * Find all process records
     */
    public List<ProcessDetails> findAll() {
        String sql = """
            SELECT id, type, input_data, schedule, current_status, current_task_index, total_tasks,
                   started_when, completed_when, failed_when, stopped_when, last_error_message, triggered_by,
                   created_at, updated_at
            FROM process_record ORDER BY created_at DESC
            """;
        
        return executeQuery(sql);
    }

    /**
     * Find process records by status
     */
    public List<ProcessDetails> findByStatus(String status) {
        String sql = """
            SELECT id, type, input_data, schedule, current_status, current_task_index, total_tasks,
                   started_when, completed_when, failed_when, stopped_when, last_error_message, triggered_by,
                   created_at, updated_at
            FROM process_record WHERE current_status = ? ORDER BY created_at DESC
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            return executeQuery(stmt);
            
        } catch (SQLException e) {
            logger.error("Failed to find process records by status: {}", status, e);
            throw new RuntimeException("Failed to find process records by status", e);
        }
    }

    /**
     * Find scheduled process records (non-null schedule)
     */
    public List<ProcessDetails> findScheduled() {
        String sql = """
            SELECT id, type, input_data, schedule, current_status, current_task_index, total_tasks,
                   started_when, completed_when, failed_when, stopped_when, last_error_message, triggered_by,
                   created_at, updated_at
            FROM process_record WHERE schedule IS NOT NULL ORDER BY created_at DESC
            """;
        
        return executeQuery(sql);
    }

    /**
     * Update process status and execution details
     */
    public void updateStatus(String id, String status, Instant executionTime, String errorMessage) {
        StringBuilder sql = new StringBuilder("UPDATE process_record SET current_status = ?, updated_at = ?");
        List<Object> params = new ArrayList<>();
        params.add(status);
        params.add(Timestamp.from(Instant.now()));
        
        // Add execution time fields based on status
        switch (status.toUpperCase()) {
            case "IN_PROGRESS":
                sql.append(", started_when = ?");
                params.add(Timestamp.from(executionTime));
                break;
            case "COMPLETED":
                sql.append(", completed_when = ?");
                params.add(Timestamp.from(executionTime));
                break;
            case "FAILED":
                sql.append(", failed_when = ?");
                params.add(Timestamp.from(executionTime));
                if (errorMessage != null) {
                    sql.append(", last_error_message = ?");
                    params.add(errorMessage);
                }
                break;
            case "STOPPED":
                sql.append(", stopped_when = ?");
                params.add(Timestamp.from(executionTime));
                break;
        }
        
        sql.append(" WHERE id = ?");
        params.add(id);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Process record not found: " + id);
            }
            
            logger.info("Updated process record status: {} -> {}", id, status);
            
        } catch (SQLException e) {
            logger.error("Failed to update process record status: {}", id, e);
            throw new RuntimeException("Failed to update process record status", e);
        }
    }

    /**
     * Update task progress
     */
    public void updateTaskProgress(String id, Integer currentTaskIndex, Integer totalTasks) {
        String sql = """
            UPDATE process_record 
            SET current_task_index = ?, total_tasks = ?, updated_at = ?
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, currentTaskIndex);
            stmt.setInt(2, totalTasks);
            stmt.setTimestamp(3, Timestamp.from(Instant.now()));
            stmt.setString(4, id);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Process record not found: " + id);
            }
            
            logger.debug("Updated task progress for process record: {} -> {}/{}", id, currentTaskIndex, totalTasks);
            
        } catch (SQLException e) {
            logger.error("Failed to update task progress: {}", id, e);
            throw new RuntimeException("Failed to update task progress", e);
        }
    }

    /**
     * Update triggered by information
     */
    public void updateTriggeredBy(String id, String triggeredBy) {
        String sql = """
            UPDATE process_record 
            SET triggered_by = ?, updated_at = ?
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, triggeredBy);
            stmt.setTimestamp(2, Timestamp.from(Instant.now()));
            stmt.setString(3, id);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Process record not found: " + id);
            }
            
            logger.debug("Updated triggered_by for process record: {} -> {}", id, triggeredBy);
            
        } catch (SQLException e) {
            logger.error("Failed to update triggered_by: {}", id, e);
            throw new RuntimeException("Failed to update triggered_by", e);
        }
    }

    /**
     * Execute a query and return list of ProcessDetails
     */
    private List<ProcessDetails> executeQuery(String sql) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            return executeQuery(stmt);
            
        } catch (SQLException e) {
            logger.error("Failed to execute query", e);
            throw new RuntimeException("Failed to execute query", e);
        }
    }

    /**
     * Execute a prepared statement and return list of ProcessDetails
     */
    private List<ProcessDetails> executeQuery(PreparedStatement stmt) throws SQLException {
        List<ProcessDetails> results = new ArrayList<>();
        
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ProcessDetails details = mapResultSetToProcessDetails(rs);
                results.add(details);
            }
        }
        
        return results;
    }

    /**
     * Check if a process record exists
     */
    public boolean exists(String id) {
        String sql = "SELECT COUNT(*) FROM process_record WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
            
        } catch (SQLException e) {
            logger.error("Failed to check if process record exists: {}", id, e);
            throw new RuntimeException("Failed to check if process record exists", e);
        }
    }

    /**
     * Count process records by status
     */
    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM process_record WHERE current_status = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
            
        } catch (SQLException e) {
            logger.error("Failed to count process records by status: {}", status, e);
            throw new RuntimeException("Failed to count process records by status", e);
        }
    }

    /**
     * Map a ResultSet to ProcessDetails object
     */
    private ProcessDetails mapResultSetToProcessDetails(ResultSet rs) throws SQLException {
        ProcessRecord record = new ProcessRecord(
            rs.getString("id"),
            rs.getString("type"),
            rs.getString("input_data"),
            rs.getString("schedule")
        );
        
        return new ProcessDetails(
            record,
            rs.getString("current_status"),
            rs.getInt("current_task_index"),
            rs.getInt("total_tasks"),
            rs.getTimestamp("started_when") != null ? rs.getTimestamp("started_when").toInstant() : null,
            rs.getTimestamp("completed_when") != null ? rs.getTimestamp("completed_when").toInstant() : null,
            rs.getTimestamp("failed_when") != null ? rs.getTimestamp("failed_when").toInstant() : null,
            rs.getTimestamp("stopped_when") != null ? rs.getTimestamp("stopped_when").toInstant() : null,
            rs.getString("last_error_message"),
            rs.getString("triggered_by"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null,
            rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null
        );
    }
}