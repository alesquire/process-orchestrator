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
 * Handles all database operations for process records
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
            INSERT INTO process_record (id, type, input_data, schedule, current_status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            Instant now = Instant.now();
            stmt.setString(1, processRecord.getId());
            stmt.setString(2, processRecord.getType());
            stmt.setString(3, processRecord.getInputData());
            stmt.setString(4, processRecord.getSchedule());
            stmt.setString(5, "PENDING"); // Default status
            stmt.setTimestamp(6, Timestamp.from(now));
            stmt.setTimestamp(7, Timestamp.from(now));
            
            stmt.executeUpdate();
            logger.info("Created process record with ID: {}", processRecord.getId());
            
        } catch (SQLException e) {
            logger.error("Error creating process record with ID: {}", processRecord.getId(), e);
            throw new RuntimeException("Failed to create process record", e);
        }
    }

    /**
     * Find a process record by ID (returns ProcessDetails with all fields)
     */
    public Optional<ProcessDetails> findById(String id) {
        String sql = "SELECT * FROM process_record WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapRowToProcessDetails(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error retrieving process record with ID: {}", id, e);
            throw new RuntimeException("Failed to retrieve process record", e);
        }
        
        return Optional.empty();
    }

    /**
     * Find all process records (returns ProcessDetails with all fields)
     */
    public List<ProcessDetails> findAll() {
        List<ProcessDetails> records = new ArrayList<>();
        String sql = "SELECT * FROM process_record ORDER BY created_at DESC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                records.add(mapRowToProcessDetails(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error retrieving all process records", e);
            throw new RuntimeException("Failed to retrieve process records", e);
        }
        
        return records;
    }

    /**
     * Find process records by status (returns ProcessDetails with all fields)
     */
    public List<ProcessDetails> findByStatus(String status) {
        List<ProcessDetails> records = new ArrayList<>();
        String sql = "SELECT * FROM process_record WHERE current_status = ? ORDER BY created_at DESC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                records.add(mapRowToProcessDetails(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error retrieving process records by status: {}", status, e);
            throw new RuntimeException("Failed to retrieve process records by status", e);
        }
        
        return records;
    }

    /**
     * Find scheduled process records (returns ProcessDetails with all fields)
     */
    public List<ProcessDetails> findScheduled() {
        List<ProcessDetails> records = new ArrayList<>();
        String sql = "SELECT * FROM process_record WHERE schedule IS NOT NULL AND schedule != '' ORDER BY created_at DESC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                records.add(mapRowToProcessDetails(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error retrieving scheduled process records", e);
            throw new RuntimeException("Failed to retrieve scheduled process records", e);
        }
        
        return records;
    }

    /**
     * Update a process record (user-managed fields only)
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
            
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new RuntimeException("Process record not found: " + processRecord.getId());
            }
            
            logger.info("Updated process record with ID: {}", processRecord.getId());
            
        } catch (SQLException e) {
            logger.error("Error updating process record with ID: {}", processRecord.getId(), e);
            throw new RuntimeException("Failed to update process record", e);
        }
    }

    /**
     * Update process record status and execution statistics
     */
    public void updateStatus(String id, String status, String currentProcessId) {
        updateStatus(id, status, currentProcessId, null, null);
    }

    /**
     * Update process record with execution statistics
     */
    public void updateStatus(String id, String status, String currentProcessId, 
                           Instant executionTime, String errorMessage) {
        String sql = """
            UPDATE process_record 
            SET current_status = ?, current_process_id = ?, updated_at = ?
            """;
        
        // Add execution time fields based on status only if executionTime is provided
        if (executionTime != null) {
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
        }
        
        sql += " WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            stmt.setString(paramIndex++, status);
            stmt.setString(paramIndex++, currentProcessId);
            stmt.setTimestamp(paramIndex++, Timestamp.from(Instant.now()));
            
            // Add execution time based on status only if executionTime is provided
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
            
            stmt.setString(paramIndex, id);
            
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new RuntimeException("Process record not found: " + id);
            }
            
            logger.info("Updated process record {} status to {}", id, status);
            
        } catch (SQLException e) {
            logger.error("Error updating process record status for ID: {}", id, e);
            throw new RuntimeException("Failed to update process record status", e);
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
            int rowsDeleted = stmt.executeUpdate();
            
            if (rowsDeleted == 0) {
                throw new RuntimeException("Process record not found: " + id);
            }
            
            logger.info("Deleted process record with ID: {}", id);
            
        } catch (SQLException e) {
            logger.error("Error deleting process record with ID: {}", id, e);
            throw new RuntimeException("Failed to delete process record", e);
        }
    }

    /**
     * Check if a process record exists
     */
    public boolean exists(String id) {
        String sql = "SELECT COUNT(*) FROM process_record WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            logger.error("Error checking if process record exists with ID: {}", id, e);
            throw new RuntimeException("Failed to check if process record exists", e);
        }
        
        return false;
    }

    /**
     * Count process records by status
     */
    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM process_record WHERE current_status = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            logger.error("Error counting process records by status: {}", status, e);
            throw new RuntimeException("Failed to count process records by status", e);
        }
        
        return 0;
    }

    /**
     * Map database row to ProcessDetails object
     */
    private ProcessDetails mapRowToProcessDetails(ResultSet rs) throws SQLException {
        // Create base ProcessRecord
        ProcessRecord record = new ProcessRecord(
            rs.getString("id"),
            rs.getString("type"),
            rs.getString("input_data"),
            rs.getString("schedule")
        );
        
        // Create ProcessDetails with engine-managed fields
        return new ProcessDetails(
            record,
            rs.getString("current_status"),
            rs.getString("current_process_id"),
            Optional.ofNullable(rs.getTimestamp("started_when")).map(Timestamp::toInstant).orElse(null),
            Optional.ofNullable(rs.getTimestamp("completed_when")).map(Timestamp::toInstant).orElse(null),
            Optional.ofNullable(rs.getTimestamp("failed_when")).map(Timestamp::toInstant).orElse(null),
            Optional.ofNullable(rs.getTimestamp("stopped_when")).map(Timestamp::toInstant).orElse(null),
            rs.getString("last_error_message"),
            Optional.ofNullable(rs.getTimestamp("created_at")).map(Timestamp::toInstant).orElse(null),
            Optional.ofNullable(rs.getTimestamp("updated_at")).map(Timestamp::toInstant).orElse(null)
        );
    }
}
