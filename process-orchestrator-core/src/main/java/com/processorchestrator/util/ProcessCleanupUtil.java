package com.processorchestrator.util;

import com.processorchestrator.config.DatabaseConfig;
import com.processorchestrator.dao.ProcessRecordDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Utility class to clean up stuck processes and tasks
 */
public class ProcessCleanupUtil {
    private static final Logger logger = LoggerFactory.getLogger(ProcessCleanupUtil.class);
    
    private final DataSource dataSource;
    private final ProcessRecordDAO processRecordDAO;
    
    public ProcessCleanupUtil() {
        this.dataSource = new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return DriverManager.getConnection(
                    DatabaseConfig.getDatabaseUrl(),
                    DatabaseConfig.getDatabaseUsername(),
                    DatabaseConfig.getDatabasePassword()
                );
            }
            
            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return DriverManager.getConnection(
                    DatabaseConfig.getDatabaseUrl(),
                    username,
                    password
                );
            }
            
            @Override
            public java.io.PrintWriter getLogWriter() throws SQLException { return null; }
            @Override
            public void setLogWriter(java.io.PrintWriter out) throws SQLException {}
            @Override
            public void setLoginTimeout(int seconds) throws SQLException {}
            @Override
            public int getLoginTimeout() throws SQLException { return 0; }
            @Override
            public java.util.logging.Logger getParentLogger() { return null; }
            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
        };
        
        this.processRecordDAO = new ProcessRecordDAO(dataSource);
    }
    
    /**
     * Clean up all stuck processes (IN_PROGRESS status)
     */
    public void cleanupStuckProcesses() {
        logger.info("Starting cleanup of stuck processes...");
        
        try (Connection conn = dataSource.getConnection()) {
            // Get list of stuck processes
            List<String> stuckProcessIds = processRecordDAO.findByStatus("IN_PROGRESS")
                .stream()
                .map(record -> record.getId())
                .toList();
            
            if (stuckProcessIds.isEmpty()) {
                logger.info("No stuck processes found");
                return;
            }
            
            logger.info("Found {} stuck processes: {}", stuckProcessIds.size(), stuckProcessIds);
            
            // Clean up scheduled_tasks
            String deleteScheduledTasks = "DELETE FROM scheduled_tasks WHERE task_instance LIKE ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteScheduledTasks)) {
                for (String processId : stuckProcessIds) {
                    stmt.setString(1, processId + "%");
                    int deleted = stmt.executeUpdate();
                    logger.info("Deleted {} scheduled tasks for process {}", deleted, processId);
                }
            }
            
            // Clean up tasks
            String deleteTasks = "DELETE FROM tasks WHERE process_record_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteTasks)) {
                for (String processId : stuckProcessIds) {
                    stmt.setString(1, processId);
                    int deleted = stmt.executeUpdate();
                    logger.info("Deleted {} tasks for process {}", deleted, processId);
                }
            }
            
            // Clean up process records
            String deleteProcessRecords = "DELETE FROM process_record WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteProcessRecords)) {
                for (String processId : stuckProcessIds) {
                    stmt.setString(1, processId);
                    int deleted = stmt.executeUpdate();
                    logger.info("Deleted process record {}", processId);
                }
            }
            
            logger.info("Cleanup completed successfully");
            
        } catch (SQLException e) {
            logger.error("Error during cleanup", e);
            throw new RuntimeException("Failed to cleanup stuck processes", e);
        }
    }
    
    /**
     * Main method for command-line usage
     */
    public static void main(String[] args) {
        ProcessCleanupUtil cleanupUtil = new ProcessCleanupUtil();
        cleanupUtil.cleanupStuckProcesses();
    }
}
