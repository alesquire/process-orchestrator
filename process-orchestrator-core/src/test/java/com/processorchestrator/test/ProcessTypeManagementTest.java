package com.processorchestrator.test;

import com.processorchestrator.config.DatabaseConfig;
import com.processorchestrator.config.ProcessType;
import com.processorchestrator.config.ProcessTypeRegistry;
import com.processorchestrator.config.ProcessTypeInitializer;
import com.processorchestrator.controller.ProcessRecordController;
import com.processorchestrator.dao.ProcessRecordDAO;
import com.processorchestrator.database.DBInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ProcessRecordController process type management functionality
 * Tests that process types and task types are returned correctly
 */
public class ProcessTypeManagementTest {
    private static final Logger logger = LoggerFactory.getLogger(ProcessTypeManagementTest.class);

    private DataSource dataSource;
    private ProcessRecordDAO processRecordDAO;
    private ProcessRecordController processRecordController;
    private ProcessTypeRegistry processTypeRegistry;
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
            public void setLoginTimeout(int seconds) throws SQLException { }
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
        dbInitializer.initializeDatabase();
        
        // Create services
        processRecordDAO = new ProcessRecordDAO(dataSource);
        
        // Initialize process type registry
        processTypeRegistry = new ProcessTypeRegistry();
        ProcessTypeInitializer.registerDefaultProcessTypes(processTypeRegistry);
        
        processRecordController = new ProcessRecordController(processRecordDAO, processTypeRegistry);
    }

    @AfterEach
    void tearDown() {
        // Clean up test data using DBInitializer
        if (dbInitializer != null) {
            dbInitializer.cleanupTestData();
        }
    }

    @Test
    void testGetAvailableProcessTypes() {
        logger.info("=== Testing getAvailableProcessTypes() ===");
        
        // Get all available process types
        List<ProcessType> availableTypes = processRecordController.getAvailableProcessTypes();
        
        // Verify we have exactly 4 process types
        assertEquals(4, availableTypes.size(), "Should have exactly 4 process types");
        
        // Verify specific process types exist
        List<String> typeNames = availableTypes.stream()
                .map(ProcessType::getName)
                .collect(java.util.stream.Collectors.toList());
        
        assertTrue(typeNames.contains("single-task-process"), "Should contain single-task-process");
        assertTrue(typeNames.contains("two-task-process"), "Should contain two-task-process");
        assertTrue(typeNames.contains("three-task-process"), "Should contain three-task-process");
        assertTrue(typeNames.contains("failing-process"), "Should contain failing-process");
        
        logger.info("✓ Found {} process types: {}", availableTypes.size(), typeNames);
        
        // Verify task counts for each process type
        for (ProcessType processType : availableTypes) {
            logger.info("Process Type: {} - {} tasks", processType.getName(), processType.getTaskCount());
            
            switch (processType.getName()) {
                case "single-task-process":
                    assertEquals(1, processType.getTaskCount(), "Single task process should have 1 task");
                    assertEquals("validate", processType.getTask(0).getName(), "First task should be 'validate'");
                    break;
                case "two-task-process":
                    assertEquals(2, processType.getTaskCount(), "Two task process should have 2 tasks");
                    assertEquals("extract", processType.getTask(0).getName(), "First task should be 'extract'");
                    assertEquals("transform", processType.getTask(1).getName(), "Second task should be 'transform'");
                    break;
                case "three-task-process":
                    assertEquals(3, processType.getTaskCount(), "Three task process should have 3 tasks");
                    assertEquals("load", processType.getTask(0).getName(), "First task should be 'load'");
                    assertEquals("process", processType.getTask(1).getName(), "Second task should be 'process'");
                    assertEquals("analyze", processType.getTask(2).getName(), "Third task should be 'analyze'");
                    break;
                case "failing-process":
                    assertEquals(1, processType.getTaskCount(), "Failing process should have 1 task");
                    assertEquals("failing-task", processType.getTask(0).getName(), "Task should be 'failing-task'");
                    break;
                default:
                    fail("Unexpected process type: " + processType.getName());
            }
        }
        
        logger.info("✓ All process types have correct task configurations");
    }

    @Test
    void testGetProcessType() {
        logger.info("=== Testing getProcessType() ===");
        
        // Test getting each process type
        ProcessType singleTask = processRecordController.getProcessType("single-task-process");
        assertNotNull(singleTask, "Should return single-task-process");
        assertEquals("single-task-process", singleTask.getName());
        assertEquals("Process with one task", singleTask.getDescription());
        assertEquals(1, singleTask.getTaskCount());
        
        ProcessType twoTask = processRecordController.getProcessType("two-task-process");
        assertNotNull(twoTask, "Should return two-task-process");
        assertEquals("two-task-process", twoTask.getName());
        assertEquals("Process with two tasks", twoTask.getDescription());
        assertEquals(2, twoTask.getTaskCount());
        
        ProcessType threeTask = processRecordController.getProcessType("three-task-process");
        assertNotNull(threeTask, "Should return three-task-process");
        assertEquals("three-task-process", threeTask.getName());
        assertEquals("Process with three tasks", threeTask.getDescription());
        assertEquals(3, threeTask.getTaskCount());
        
        ProcessType failing = processRecordController.getProcessType("failing-process");
        assertNotNull(failing, "Should return failing-process");
        assertEquals("failing-process", failing.getName());
        assertEquals("Process that intentionally fails", failing.getDescription());
        assertEquals(1, failing.getTaskCount());
        
        // Test getting non-existent process type
        ProcessType nonExistent = processRecordController.getProcessType("non-existent-process");
        assertNull(nonExistent, "Should return null for non-existent process type");
        
        logger.info("✓ getProcessType() works correctly for all process types");
    }

    @Test
    void testIsValidProcessType() {
        logger.info("=== Testing isValidProcessType() ===");
        
        // Test valid process types
        assertTrue(processRecordController.isValidProcessType("single-task-process"), 
                "single-task-process should be valid");
        assertTrue(processRecordController.isValidProcessType("two-task-process"), 
                "two-task-process should be valid");
        assertTrue(processRecordController.isValidProcessType("three-task-process"), 
                "three-task-process should be valid");
        assertTrue(processRecordController.isValidProcessType("failing-process"), 
                "failing-process should be valid");
        
        // Test invalid process types
        assertFalse(processRecordController.isValidProcessType("invalid-process"), 
                "invalid-process should not be valid");
        assertFalse(processRecordController.isValidProcessType("non-existent"), 
                "non-existent should not be valid");
        assertFalse(processRecordController.isValidProcessType(""), 
                "empty string should not be valid");
        assertFalse(processRecordController.isValidProcessType(null), 
                "null should not be valid");
        
        logger.info("✓ isValidProcessType() correctly validates process types");
    }

    @Test
    void testGetProcessTypeNames() {
        logger.info("=== Testing getProcessTypeNames() ===");
        
        // Get process type names
        List<String> typeNames = processRecordController.getProcessTypeNames();
        
        // Verify we have exactly 4 process type names
        assertEquals(4, typeNames.size(), "Should have exactly 4 process type names");
        
        // Verify specific names exist
        assertTrue(typeNames.contains("single-task-process"), "Should contain single-task-process");
        assertTrue(typeNames.contains("two-task-process"), "Should contain two-task-process");
        assertTrue(typeNames.contains("three-task-process"), "Should contain three-task-process");
        assertTrue(typeNames.contains("failing-process"), "Should contain failing-process");
        
        logger.info("✓ Process type names: {}", typeNames);
    }

    @Test
    void testCreateProcessRecordWithValidTypes() {
        logger.info("=== Testing createProcessRecord with valid process types ===");
        
        // Test creating process records with each valid process type
        String[] validTypes = {"single-task-process", "two-task-process", "three-task-process", "failing-process"};
        
        for (String processType : validTypes) {
            String processId = "test-" + processType + "-" + System.currentTimeMillis();
            String inputData = "{\"test\": \"data\"}";
            
            logger.info("Creating process record with type: {}", processType);
            
            ProcessRecordController.ProcessRecordResponse response = 
                processRecordController.createProcessRecord(processId, processType, inputData, null);
            
            assertTrue(response.isSuccess(), 
                "Creating process record with " + processType + " should succeed");
            assertEquals(processId, response.getData().getId());
            assertEquals(processType, response.getData().getType());
            assertEquals(inputData, response.getData().getInputData());
            assertEquals("PENDING", response.getData().getCurrentStatus());
            
            logger.info("✓ Successfully created process record with type: {}", processType);
        }
    }

    @Test
    void testCreateProcessRecordWithInvalidTypes() {
        logger.info("=== Testing createProcessRecord with invalid process types ===");
        
        // Test creating process records with invalid process types
        String[] invalidTypes = {"invalid-process", "non-existent", "test-process", "ping-process"};
        
        for (String invalidType : invalidTypes) {
            String processId = "test-invalid-" + System.currentTimeMillis();
            String inputData = "{\"test\": \"data\"}";
            
            logger.info("Attempting to create process record with invalid type: {}", invalidType);
            
            ProcessRecordController.ProcessRecordResponse response = 
                processRecordController.createProcessRecord(processId, invalidType, inputData, null);
            
            assertFalse(response.isSuccess(), 
                "Creating process record with " + invalidType + " should fail");
            assertTrue(response.getMessage().contains("Invalid process type"), 
                "Error message should mention invalid process type");
            assertTrue(response.getMessage().contains(invalidType), 
                "Error message should contain the invalid type name");
            
            logger.info("✓ Correctly rejected invalid process type: {} - Error: {}", invalidType, response.getMessage());
        }
    }

    @Test
    void testProcessTypeTaskDetails() {
        logger.info("=== Testing detailed task information for each process type ===");
        
        List<ProcessType> processTypes = processRecordController.getAvailableProcessTypes();
        
        for (ProcessType processType : processTypes) {
            logger.info("--- Process Type: {} ---", processType.getName());
            logger.info("Description: {}", processType.getDescription());
            logger.info("Task Count: {}", processType.getTaskCount());
            
            // Verify task details
            for (int i = 0; i < processType.getTaskCount(); i++) {
                var task = processType.getTask(i);
                logger.info("  Task {}: {} - Command: {}", i + 1, task.getName(), task.getCommand());
                logger.info("    Working Directory: {}", task.getWorkingDirectory());
                logger.info("    Timeout: {} minutes", task.getTimeoutMinutes());
                logger.info("    Max Retries: {}", task.getMaxRetries());
                
                // Verify task properties are not null/empty
                assertNotNull(task.getName(), "Task name should not be null");
                assertNotNull(task.getCommand(), "Task command should not be null");
                assertNotNull(task.getWorkingDirectory(), "Task working directory should not be null");
                assertTrue(task.getTimeoutMinutes() > 0, "Task timeout should be positive");
                assertTrue(task.getMaxRetries() >= 0, "Task max retries should be non-negative");
            }
            
            logger.info("--- End Process Type: {} ---", processType.getName());
        }
        
        logger.info("✓ All process types have valid task configurations");
    }
}
