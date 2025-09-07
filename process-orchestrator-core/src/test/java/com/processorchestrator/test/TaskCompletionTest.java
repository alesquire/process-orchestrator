package com.processorchestrator.test;

import com.processorchestrator.config.DatabaseConfig;
import com.processorchestrator.config.ProcessTypeRegistry;
import com.processorchestrator.config.ProcessTypeInitializer;
import com.processorchestrator.controller.ProcessController;
import com.processorchestrator.controller.ProcessRecordController;
import com.processorchestrator.dao.ProcessRecordDAO;
import com.processorchestrator.database.DBInitializer;
import com.processorchestrator.model.ProcessInputData;
import com.processorchestrator.service.ProcessOrchestrator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that ProcessOrchestrator waits for task completion
 */
public class TaskCompletionTest {
    private static final Logger logger = LoggerFactory.getLogger(TaskCompletionTest.class);

    private DataSource dataSource;
    private ProcessRecordDAO processRecordDAO;
    private ProcessRecordController processRecordController;
    private ProcessController processController;
    private ProcessTypeRegistry processTypeRegistry;
    private ProcessOrchestrator processOrchestrator;
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
        
        // Initialize process type registry and orchestrator
        processTypeRegistry = new ProcessTypeRegistry();
        ProcessTypeInitializer.registerDefaultProcessTypes(processTypeRegistry);
        processOrchestrator = new ProcessOrchestrator(dataSource, processTypeRegistry);
        processController = new ProcessController(processRecordDAO, processOrchestrator, processTypeRegistry);
        processRecordController = new ProcessRecordController(processRecordDAO, processTypeRegistry);
        
        // Start the ProcessOrchestrator scheduler
        processOrchestrator.start();
    }

    @AfterEach
    void tearDown() {
        // Stop the orchestrator
        if (processOrchestrator != null) {
            processOrchestrator.stop();
        }
        
        // Clean up test data using DBInitializer
        if (dbInitializer != null) {
            dbInitializer.cleanupTestData();
        }
    }

    @Test
    void testSingleTaskProcessCompletion() {
        logger.info("=== Testing Single Task Process Completion ===");
        
        // Clean up any existing test data
        dbInitializer.cleanupTestData();
        
        // Create a process record
        String processId = "test-single-task-completion-" + System.currentTimeMillis();
        String inputData = "{\"input_file\": \"" + System.getProperty("java.io.tmpdir") + "/test.txt\"}";
        
        logger.info("Creating process record: {}", processId);
        ProcessRecordController.ProcessRecordResponse createResponse = processRecordController.createProcessRecord(
            processId, processTypeRegistry.getProcessType("single-task-process"), inputData, null);
        
        assertTrue(createResponse.isSuccess(), "Process record creation should succeed");
        assertEquals(processId, createResponse.getData().getId());
        assertEquals("single-task-process", createResponse.getData().getType());
        assertEquals("PENDING", createResponse.getData().getCurrentStatus());
        logger.info("✓ Created ProcessRecord: {}", processId);

        // Start the process
        logger.info("Starting process: {}", processId);
        ProcessController.ProcessStartResponse startResponse = processController.startProcess(processId);
        assertTrue(startResponse.isSuccess(), "Process should start successfully");
        logger.info("✓ Started Process: {} (orchestrator ID: {})", processId, startResponse.getOrchestratorProcessId());

        // Wait for process to complete (with timeout)
        logger.info("Waiting for process to complete...");
        int maxWaitTime = 30; // 30 seconds timeout
        int waitTime = 0;
        boolean processCompleted = false;
        
        while (waitTime < maxWaitTime && !processCompleted) {
            try {
                Thread.sleep(1000); // Wait 1 second
                waitTime++;
                
                // Check process status
                ProcessController.ProcessStateResponse stateResponse = processController.getProcessState(processId);
                assertTrue(stateResponse.isSuccess(), "Should be able to get process state");
                
                String currentStatus = stateResponse.getProcessDetails().getCurrentStatus();
                logger.info("Process status after {} seconds: {}", waitTime, currentStatus);
                
                if ("COMPLETED".equals(currentStatus)) {
                    processCompleted = true;
                    logger.info("✓ Process completed successfully after {} seconds", waitTime);
                } else if ("FAILED".equals(currentStatus)) {
                    logger.error("Process failed: {}", stateResponse.getProcessDetails().getLastErrorMessage());
                    fail("Process should not fail");
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Test interrupted");
            }
        }
        
        // Verify process completed
        assertTrue(processCompleted, "Process should complete within " + maxWaitTime + " seconds");
        
        // Verify final state
        ProcessController.ProcessStateResponse finalState = processController.getProcessState(processId);
        assertTrue(finalState.isSuccess(), "Should be able to get final process state");
        
        var processDetails = finalState.getProcessDetails();
        assertEquals("COMPLETED", processDetails.getCurrentStatus(), "Process should be completed");
        assertNotNull(processDetails.getCompletedWhen(), "Process should have completion timestamp");
        assertEquals(1, processDetails.getCurrentTaskIndex(), "Should be on task 1 (completed)");
        assertEquals(1, processDetails.getTotalTasks(), "Should have 1 total task");
        
        logger.info("✓ Process completed successfully:");
        logger.info("  - Status: {}", processDetails.getCurrentStatus());
        logger.info("  - Completed: {}", processDetails.getCompletedWhen());
        logger.info("  - Task Progress: {}/{}", processDetails.getCurrentTaskIndex(), processDetails.getTotalTasks());
        
        logger.info("=== Single Task Process Completion Test Passed ===");
    }

    @Test
    void testTwoTaskProcessCompletion() {
        logger.info("=== Testing Two Task Process Completion ===");
        
        // Clean up any existing test data
        dbInitializer.cleanupTestData();
        
        // Create a process record
        String processId = "test-two-task-completion-" + System.currentTimeMillis();
        String inputData = "{\"input_file\": \"" + System.getProperty("java.io.tmpdir") + "/test.txt\", \"output_dir\": \"" + System.getProperty("java.io.tmpdir") + "/output\"}";
        
        logger.info("Creating process record: {}", processId);
        ProcessRecordController.ProcessRecordResponse createResponse = processRecordController.createProcessRecord(
            processId, processTypeRegistry.getProcessType("two-task-process"), inputData, null);
        
        assertTrue(createResponse.isSuccess(), "Process record creation should succeed");
        assertEquals(processId, createResponse.getData().getId());
        assertEquals("two-task-process", createResponse.getData().getType());
        assertEquals("PENDING", createResponse.getData().getCurrentStatus());
        logger.info("✓ Created ProcessRecord: {}", processId);

        // Start the process
        logger.info("Starting process: {}", processId);
        ProcessController.ProcessStartResponse startResponse = processController.startProcess(processId);
        assertTrue(startResponse.isSuccess(), "Process should start successfully");
        logger.info("✓ Started Process: {} (orchestrator ID: {})", processId, startResponse.getOrchestratorProcessId());

        // Wait for process to complete (with timeout)
        logger.info("Waiting for process to complete...");
        int maxWaitTime = 60; // 60 seconds timeout for 2 tasks
        int waitTime = 0;
        boolean processCompleted = false;
        
        while (waitTime < maxWaitTime && !processCompleted) {
            try {
                Thread.sleep(1000); // Wait 1 second
                waitTime++;
                
                // Check process status
                ProcessController.ProcessStateResponse stateResponse = processController.getProcessState(processId);
                assertTrue(stateResponse.isSuccess(), "Should be able to get process state");
                
                String currentStatus = stateResponse.getProcessDetails().getCurrentStatus();
                int currentTaskIndex = stateResponse.getProcessDetails().getCurrentTaskIndex();
                int totalTasks = stateResponse.getProcessDetails().getTotalTasks();
                
                logger.info("Process status after {} seconds: {} (task {}/{})", waitTime, currentStatus, currentTaskIndex, totalTasks);
                
                if ("COMPLETED".equals(currentStatus)) {
                    processCompleted = true;
                    logger.info("✓ Process completed successfully after {} seconds", waitTime);
                } else if ("FAILED".equals(currentStatus)) {
                    logger.error("Process failed: {}", stateResponse.getProcessDetails().getLastErrorMessage());
                    fail("Process should not fail");
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Test interrupted");
            }
        }
        
        // Verify process completed
        assertTrue(processCompleted, "Process should complete within " + maxWaitTime + " seconds");
        
        // Verify final state
        ProcessController.ProcessStateResponse finalState = processController.getProcessState(processId);
        assertTrue(finalState.isSuccess(), "Should be able to get final process state");
        
        var processDetails = finalState.getProcessDetails();
        assertEquals("COMPLETED", processDetails.getCurrentStatus(), "Process should be completed");
        assertNotNull(processDetails.getCompletedWhen(), "Process should have completion timestamp");
        assertEquals(2, processDetails.getCurrentTaskIndex(), "Should be on task 2 (completed)");
        assertEquals(2, processDetails.getTotalTasks(), "Should have 2 total tasks");
        
        logger.info("✓ Process completed successfully:");
        logger.info("  - Status: {}", processDetails.getCurrentStatus());
        logger.info("  - Completed: {}", processDetails.getCompletedWhen());
        logger.info("  - Task Progress: {}/{}", processDetails.getCurrentTaskIndex(), processDetails.getTotalTasks());
        
        logger.info("=== Two Task Process Completion Test Passed ===");
    }
}
