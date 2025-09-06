package com.processorchestrator.test;

import com.processorchestrator.config.DatabaseConfig;
import com.processorchestrator.controller.ProcessRecordController;
import com.processorchestrator.dao.ProcessRecordDAO;
import com.processorchestrator.database.DBInitializer;
import com.processorchestrator.model.ProcessDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple CRUD test for ProcessRecord operations
 */
public class ProcessRecordCRUDTest {
    private static final Logger logger = LoggerFactory.getLogger(ProcessRecordCRUDTest.class);

    private DataSource dataSource;
    private ProcessRecordDAO processRecordDAO;
    private ProcessRecordController processRecordController;
    private DBInitializer dbInitializer;

    @BeforeEach
    void setUp() {
        // Create PostgreSQL database connection using DatabaseConfig
        dataSource = new DataSource() {
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
        
        // Initialize database schema using DBInitializer
        dbInitializer = new DBInitializer(dataSource);
        dbInitializer.initializeEssentialTables();
        
        // Create services
        processRecordDAO = new ProcessRecordDAO(dataSource);
        processRecordController = new ProcessRecordController(processRecordDAO);
    }

    @AfterEach
    void tearDown() {
        // Clean up test data using DBInitializer
        if (dbInitializer != null) {
            dbInitializer.cleanupTestData();
        }
    }

    @Test
    void testDatabaseConnectionAndSchema() {
        logger.info("Testing database connection and schema creation");
        
        try (Connection connection = dataSource.getConnection()) {
            // Test basic connection
            assertNotNull(connection, "Database connection should not be null");
            assertFalse(connection.isClosed(), "Database connection should be open");
            
            logger.info("Database connection successful");
            
            // Test database initialization status using DBInitializer
            assertTrue(dbInitializer.isDatabaseInitialized(), "Database should be properly initialized");
            logger.info("Database initialization verified");
            
            // Test table existence
            try (Statement statement = connection.createStatement()) {
                // Check if process_record table exists (PostgreSQL syntax)
                var resultSet = statement.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'process_record'"
                );
                
                assertTrue(resultSet.next(), "Should have result for table check");
                int tableCount = resultSet.getInt(1);
                assertEquals(1, tableCount, "process_record table should exist");
                
                logger.info("process_record table exists");
                
                // Test table structure by checking columns (PostgreSQL syntax)
                var columnsResultSet = statement.executeQuery(
                    "SELECT column_name FROM information_schema.columns WHERE table_name = 'process_record' ORDER BY column_name"
                );
                
                java.util.Set<String> columns = new java.util.HashSet<>();
                while (columnsResultSet.next()) {
                    columns.add(columnsResultSet.getString("column_name"));
                }
                
                // Verify essential columns exist (PostgreSQL returns lowercase column names)
                assertTrue(columns.contains("id"), "ID column should exist");
                assertTrue(columns.contains("type"), "TYPE column should exist");
                assertTrue(columns.contains("input_data"), "INPUT_DATA column should exist");
                assertTrue(columns.contains("schedule"), "SCHEDULE column should exist");
                assertTrue(columns.contains("current_status"), "CURRENT_STATUS column should exist");
                assertTrue(columns.contains("current_process_id"), "CURRENT_PROCESS_ID column should exist");
                assertTrue(columns.contains("started_when"), "STARTED_WHEN column should exist");
                assertTrue(columns.contains("completed_when"), "COMPLETED_WHEN column should exist");
                assertTrue(columns.contains("failed_when"), "FAILED_WHEN column should exist");
                assertTrue(columns.contains("stopped_when"), "STOPPED_WHEN column should exist");
                assertTrue(columns.contains("last_error_message"), "LAST_ERROR_MESSAGE column should exist");
                assertTrue(columns.contains("created_at"), "CREATED_AT column should exist");
                assertTrue(columns.contains("updated_at"), "UPDATED_AT column should exist");
                
                logger.info("All required columns exist in process_record table");
                
                // Test basic insert/select to verify table works
                String testId = "db-test-" + System.currentTimeMillis();
                int insertResult = statement.executeUpdate(
                    "INSERT INTO process_record (id, type, input_data, current_status) VALUES ('" + testId + "', 'test', 'test data', 'PENDING')"
                );
                assertEquals(1, insertResult, "Insert should affect 1 row");
                
                var selectResult = statement.executeQuery(
                    "SELECT id, type, input_data, current_status FROM process_record WHERE id = '" + testId + "'"
                );
                
                assertTrue(selectResult.next(), "Select should return the inserted row");
                assertEquals(testId, selectResult.getString("id"), "ID should match");
                assertEquals("test", selectResult.getString("type"), "Type should match");
                assertEquals("test data", selectResult.getString("input_data"), "Input data should match");
                assertEquals("PENDING", selectResult.getString("current_status"), "Status should match");
                
                logger.info("Basic insert/select operations work correctly");
                
                // Clean up test data
                int deleteResult = statement.executeUpdate("DELETE FROM process_record WHERE id = '" + testId + "'");
                assertEquals(1, deleteResult, "Delete should affect 1 row");
                
                logger.info("Database connection and schema test passed");
                
            }
            
        } catch (SQLException e) {
            logger.error("Database connection or schema test failed", e);
            fail("Database connection or schema test failed: " + e.getMessage());
        }
    }

    @Test
    void testCreateProcessRecord() {
        logger.info("Testing CREATE operation");
        
        // Create a process record
        String id = "test-crud-001";
        String type = "test-process";
        String inputData = "test input data";
        String schedule = "0 2 * * *";
        
        ProcessRecordController.ProcessRecordResponse response = 
            processRecordController.createProcessRecord(id, type, inputData, schedule);
        
        assertTrue(response.isSuccess(), "Create should succeed");
        assertNotNull(response.getData(), "Response should contain process record");
        assertEquals(id, response.getData().getId(), "ID should match");
        assertEquals(type, response.getData().getType(), "Type should match");
        assertEquals(inputData, response.getData().getInputData(), "Input data should match");
        assertEquals(schedule, response.getData().getSchedule(), "Schedule should match");
        
        logger.info("CREATE operation test passed");
    }

    @Test
    void testReadProcessRecord() {
        logger.info("Testing READ operation");
        
        // First create a process record
        String id = "test-crud-002";
        String type = "test-process";
        String inputData = "test input data for read";
        String schedule = null; // Manual execution
        
        ProcessRecordController.ProcessRecordResponse createResponse = 
            processRecordController.createProcessRecord(id, type, inputData, schedule);
        
        assertTrue(createResponse.isSuccess(), "Create should succeed");
        
        // Now read it back
        ProcessRecordController.ProcessRecordResponse readResponse = 
            processRecordController.getProcessRecord(id);
        
        assertTrue(readResponse.isSuccess(), "Read should succeed");
        assertNotNull(readResponse.getData(), "Response should contain process record");
        
        ProcessDetails retrieved = readResponse.getData();
        assertEquals(id, retrieved.getId(), "ID should match");
        assertEquals(type, retrieved.getType(), "Type should match");
        assertEquals(inputData, retrieved.getInputData(), "Input data should match");
        assertEquals(schedule, retrieved.getSchedule(), "Schedule should match");
        assertEquals("PENDING", retrieved.getCurrentStatus(), "Status should be PENDING");
        
        logger.info("READ operation test passed");
    }

    @Test
    void testUpdateProcessRecord() {
        logger.info("Testing UPDATE operation");
        
        // First create a process record
        String id = "test-crud-003";
        String originalType = "original-process";
        String originalInputData = "original input data";
        String originalSchedule = "0 2 * * *";
        
        ProcessRecordController.ProcessRecordResponse createResponse = 
            processRecordController.createProcessRecord(id, originalType, originalInputData, originalSchedule);
        
        assertTrue(createResponse.isSuccess(), "Create should succeed");
        
        // Now update it
        String updatedType = "updated-process";
        String updatedInputData = "updated input data";
        String updatedSchedule = "0 6 * * *";
        
        ProcessRecordController.ProcessRecordResponse updateResponse = 
            processRecordController.updateProcessRecord(id, updatedType, updatedInputData, updatedSchedule);
        
        assertTrue(updateResponse.isSuccess(), "Update should succeed");
        assertNotNull(updateResponse.getData(), "Response should contain process record");
        
        ProcessDetails updated = updateResponse.getData();
        assertEquals(id, updated.getId(), "ID should remain the same");
        assertEquals(updatedType, updated.getType(), "Type should be updated");
        assertEquals(updatedInputData, updated.getInputData(), "Input data should be updated");
        assertEquals(updatedSchedule, updated.getSchedule(), "Schedule should be updated");
        
        // Verify the update by reading it back
        ProcessRecordController.ProcessRecordResponse readResponse = 
            processRecordController.getProcessRecord(id);
        
        assertTrue(readResponse.isSuccess(), "Read after update should succeed");
        ProcessDetails retrieved = readResponse.getData();
        assertEquals(updatedType, retrieved.getType(), "Updated type should persist");
        assertEquals(updatedInputData, retrieved.getInputData(), "Updated input data should persist");
        assertEquals(updatedSchedule, retrieved.getSchedule(), "Updated schedule should persist");
        
        logger.info("UPDATE operation test passed");
    }

    @Test
    void testDeleteProcessRecord() {
        logger.info("Testing DELETE operation");
        
        // First create a process record
        String id = "test-crud-004";
        String type = "test-process";
        String inputData = "test input data for delete";
        String schedule = "0 12 * * *";
        
        ProcessRecordController.ProcessRecordResponse createResponse = 
            processRecordController.createProcessRecord(id, type, inputData, schedule);
        
        assertTrue(createResponse.isSuccess(), "Create should succeed");
        
        // Verify it exists
        ProcessRecordController.ProcessRecordResponse readResponse = 
            processRecordController.getProcessRecord(id);
        
        assertTrue(readResponse.isSuccess(), "Read before delete should succeed");
        assertNotNull(readResponse.getData(), "Process record should exist");
        
        // Now delete it
        ProcessRecordController.ProcessRecordDeleteResponse deleteResponse = 
            processRecordController.deleteProcessRecord(id);
        
        assertTrue(deleteResponse.isSuccess(), "Delete should succeed");
        
        // Verify it's gone
        ProcessRecordController.ProcessRecordResponse readAfterDeleteResponse = 
            processRecordController.getProcessRecord(id);
        
        assertFalse(readAfterDeleteResponse.isSuccess(), "Read after delete should fail");
        assertNull(readAfterDeleteResponse.getData(), "Process record should not exist");
        
        logger.info("DELETE operation test passed");
    }

    @Test
    void testListProcessRecords() {
        logger.info("Testing LIST operations");
        
        // Create multiple process records
        String[] ids = {"test-crud-list-001", "test-crud-list-002", "test-crud-list-003"};
        String[] types = {"type-1", "type-2", "type-1"};
        String[] schedules = {"0 2 * * *", null, "0 6 * * *"};
        
        for (int i = 0; i < ids.length; i++) {
            ProcessRecordController.ProcessRecordResponse response = 
                processRecordController.createProcessRecord(ids[i], types[i], "input-" + i, schedules[i]);
            assertTrue(response.isSuccess(), "Create " + ids[i] + " should succeed");
        }
        
        // Test getAllProcessRecords
        ProcessRecordController.ProcessRecordListResponse allResponse = 
            processRecordController.getAllProcessRecords();
        
        assertTrue(allResponse.isSuccess(), "Get all should succeed");
        assertNotNull(allResponse.getData(), "Response should contain process records");
        assertTrue(allResponse.getData().size() >= 3, "Should have at least 3 records");
        
        // Test getProcessRecordsByStatus
        ProcessRecordController.ProcessRecordListResponse pendingResponse = 
            processRecordController.getProcessRecordsByStatus("PENDING");
        
        assertTrue(pendingResponse.isSuccess(), "Get by status should succeed");
        assertNotNull(pendingResponse.getData(), "Response should contain process records");
        assertTrue(pendingResponse.getData().size() >= 3, "Should have at least 3 pending records");
        
        // Test getScheduledProcessRecords
        ProcessRecordController.ProcessRecordListResponse scheduledResponse = 
            processRecordController.getScheduledProcessRecords();
        
        assertTrue(scheduledResponse.isSuccess(), "Get scheduled should succeed");
        assertNotNull(scheduledResponse.getData(), "Response should contain process records");
        assertEquals(2, scheduledResponse.getData().size(), "Should have exactly 2 scheduled records");
        
        logger.info("LIST operations test passed");
    }

    @Test
    void testErrorHandling() {
        logger.info("Testing error handling");
        
        // Test creating duplicate ID
        String id = "test-crud-error-001";
        String type = "test-process";
        String inputData = "test input data";
        String schedule = null;
        
        // Create first record
        ProcessRecordController.ProcessRecordResponse createResponse1 = 
            processRecordController.createProcessRecord(id, type, inputData, schedule);
        
        assertTrue(createResponse1.isSuccess(), "First create should succeed");
        
        // Try to create duplicate
        ProcessRecordController.ProcessRecordResponse createResponse2 = 
            processRecordController.createProcessRecord(id, type, inputData, schedule);
        
        assertFalse(createResponse2.isSuccess(), "Duplicate create should fail");
        assertNotNull(createResponse2.getMessage(), "Error message should be provided");
        
        // Test reading non-existent record
        ProcessRecordController.ProcessRecordResponse readResponse = 
            processRecordController.getProcessRecord("non-existent-id");
        
        assertFalse(readResponse.isSuccess(), "Read non-existent should fail");
        assertNull(readResponse.getData(), "No process record should be returned");
        
        // Test updating non-existent record
        ProcessRecordController.ProcessRecordResponse updateResponse = 
            processRecordController.updateProcessRecord("non-existent-id", type, inputData, schedule);
        
        assertFalse(updateResponse.isSuccess(), "Update non-existent should fail");
        
        // Test deleting non-existent record
        ProcessRecordController.ProcessRecordDeleteResponse deleteResponse = 
            processRecordController.deleteProcessRecord("non-existent-id");
        
        assertFalse(deleteResponse.isSuccess(), "Delete non-existent should fail");
        
        logger.info("Error handling test passed");
    }
}