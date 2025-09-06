package com.processorchestrator.examples;

import com.processorchestrator.config.DatabaseConfig;
import com.processorchestrator.config.ProcessType;
import com.processorchestrator.config.ProcessTypeRegistry;
import com.processorchestrator.controller.ProcessController;
import com.processorchestrator.controller.ProcessRecordController;
import com.processorchestrator.dao.ProcessRecordDAO;
import com.processorchestrator.database.DBInitializer;
import com.processorchestrator.model.ProcessDetails;
import com.processorchestrator.service.ProcessOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test demonstrating a single task process that prints input text three times
 * and verifies correct state transitions during execution
 */
public class SingleTaskProcessTest {
    private static final Logger logger = LoggerFactory.getLogger(SingleTaskProcessTest.class);

    private DataSource dataSource;
    private DBInitializer dbInitializer;
    private ProcessRecordDAO processRecordDAO;
    private ProcessRecordController processRecordController;
    private ProcessController processController;
    private ProcessTypeRegistry processTypeRegistry;
    private ProcessOrchestrator processOrchestrator;

    @BeforeEach
    void setUp() {
        // Create PostgreSQL database connection
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

        // Initialize database schema
        dbInitializer = new DBInitializer(dataSource);
        dbInitializer.initializeDatabase();

        // Create services
        processRecordDAO = new ProcessRecordDAO(dataSource);
        processRecordController = new ProcessRecordController(processRecordDAO);
        
        // Initialize process type registry and orchestrator
        processTypeRegistry = new ProcessTypeRegistry();
        processOrchestrator = new ProcessOrchestrator(dataSource, processTypeRegistry);
        processController = new ProcessController(processRecordDAO, processOrchestrator);
        
        // Register the single task process type
        registerSingleTaskProcessType();
        
        // Start the ProcessOrchestrator scheduler
        processOrchestrator.start();
    }
    
    @AfterEach
    void tearDown() {
        // Stop the ProcessOrchestrator scheduler
        if (processOrchestrator != null) {
            processOrchestrator.stop();
        }
    }

    /**
     * Register a process type with one task that prints input text three times
     */
    private void registerSingleTaskProcessType() {
        logger.info("Registering single task process type...");

        // Create a process type with one task that prints special message and input text three times
        ProcessType singleTaskProcess = new ProcessType("single-print-task", "Process that prints special message and input text three times")
                .addTask("print-text", "python -c \"print('!!!THIS IS A TEST MESSAGE!!!'); import sys; text='${input_text}'; [print(f'Print {i+1}: {text}') for i in range(3)]\"", System.getProperty("java.io.tmpdir"), 30, 2);
        
        processTypeRegistry.register(singleTaskProcess);
        logger.info("Registered single-print-task with {} tasks", singleTaskProcess.getTaskCount());
    }

    @Test
    void testSingleTaskProcessExecution() {
        logger.info("=== Starting Single Task Process Execution Test ===");

        // Clean up any existing test data
        dbInitializer.cleanupTestData();

        // ==================== STEP 1: GENERATE RANDOM INPUT TEXT ====================
        
        String randomText = generateRandomText();
        logger.info("Generated random input text: '{}'", randomText);

        // ==================== STEP 2: CREATE PROCESS RECORD ====================
        
        String processId = "single-task-test-" + System.currentTimeMillis();
        String inputData = String.format("{\"input_text\": \"%s\"}", randomText);
        
        logger.info("Step 1: Creating ProcessRecord with random input text");
        ProcessRecordController.ProcessRecordResponse createResponse = processRecordController.createProcessRecord(
            processId, "single-print-task", inputData, null);
        
        assertTrue(createResponse.isSuccess(), "Process record creation should succeed");
        assertEquals(processId, createResponse.getData().getId());
        assertEquals("single-print-task", createResponse.getData().getType());
        assertEquals(inputData, createResponse.getData().getInputData());
        assertEquals("PENDING", createResponse.getData().getCurrentStatus());
        logger.info("✓ Created ProcessRecord: {} with input: '{}'", processId, randomText);

        // ==================== STEP 3: VERIFY INITIAL STATE ====================
        
        logger.info("Step 2: Verifying initial process state");
        ProcessController.ProcessStateResponse initialState = processController.getProcessState(processId);
        assertTrue(initialState.isSuccess(), "Should be able to get initial state");
        
        ProcessDetails initialDetails = initialState.getProcessDetails();
        assertEquals("PENDING", initialDetails.getCurrentStatus(), "Initial status should be PENDING");
        assertEquals(0, initialDetails.getCurrentTaskIndex(), "Initial task index should be 0");
        assertEquals(0, initialDetails.getTotalTasks(), "Initial total tasks should be 0");
        assertNull(initialDetails.getStartedWhen(), "Process should not be started yet");
        assertNull(initialDetails.getCompletedWhen(), "Process should not be completed yet");
        logger.info("✓ Initial state verified: PENDING, task 0/0, not started");

        // ==================== STEP 4: START PROCESS ====================
        
        logger.info("Step 3: Starting process execution");
        ProcessController.ProcessStartResponse startResponse = processController.startProcess(processId);
        assertTrue(startResponse.isSuccess(), "Process should start successfully");
        logger.info("✓ Process started successfully with orchestrator ID: {}", startResponse.getOrchestratorProcessId());

        // ==================== STEP 5: MONITOR STATE TRANSITIONS ====================
        
        logger.info("Step 4: Monitoring state transitions during execution");
        
        // Wait a moment for the process to start
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }

        // Check state after starting
        ProcessController.ProcessStateResponse runningState = processController.getProcessState(processId);
        assertTrue(runningState.isSuccess(), "Should be able to get running state");
        
        ProcessDetails runningDetails = runningState.getProcessDetails();
        assertEquals("IN_PROGRESS", runningDetails.getCurrentStatus(), "Status should be IN_PROGRESS");
        assertNotNull(runningDetails.getStartedWhen(), "Process should have started timestamp");
        logger.info("✓ Process is running: IN_PROGRESS, started at {}", runningDetails.getStartedWhen());

        // ==================== STEP 6: WAIT FOR COMPLETION ====================
        
        logger.info("Step 5: Waiting for process completion");
        
        // Wait for the process to complete (with timeout)
        int maxWaitTime = 30; // 30 seconds max
        int waitTime = 0;
        boolean completed = false;
        
        while (waitTime < maxWaitTime && !completed) {
            try {
                Thread.sleep(1000); // Wait 1 second
                waitTime++;
                
                ProcessController.ProcessStateResponse currentState = processController.getProcessState(processId);
                if (currentState.isSuccess()) {
                    ProcessDetails currentDetails = currentState.getProcessDetails();
                    String status = currentDetails.getCurrentStatus();
                    
                    logger.info("Status check {}: {}", waitTime, status);
                    
                    if ("COMPLETED".equals(status)) {
                        completed = true;
                        logger.info("✓ Process completed successfully!");
                    } else if ("FAILED".equals(status)) {
                        fail("Process failed: " + currentDetails.getLastErrorMessage());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Test interrupted while waiting for completion");
            }
        }
        
        assertTrue(completed, "Process should complete within " + maxWaitTime + " seconds");

        // ==================== STEP 7: VERIFY FINAL STATE ====================
        
        logger.info("Step 6: Verifying final process state");
        ProcessController.ProcessStateResponse finalState = processController.getProcessState(processId);
        assertTrue(finalState.isSuccess(), "Should be able to get final state");
        
        ProcessDetails finalDetails = finalState.getProcessDetails();
        assertEquals("COMPLETED", finalDetails.getCurrentStatus(), "Final status should be COMPLETED");
        assertNotNull(finalDetails.getStartedWhen(), "Process should have started timestamp");
        assertNotNull(finalDetails.getCompletedWhen(), "Process should have completed timestamp");
        assertNull(finalDetails.getLastErrorMessage(), "Process should not have error message");
        logger.info("✓ Final state verified: COMPLETED, started at {}, completed at {}", 
                   finalDetails.getStartedWhen(), finalDetails.getCompletedWhen());

        // ==================== STEP 8: VERIFY EXECUTION TIME ====================
        
        logger.info("Step 7: Verifying execution time");
        long executionTimeMs = finalDetails.getCompletedWhen().toEpochMilli() - 
                              finalDetails.getStartedWhen().toEpochMilli();
        long executionTimeSeconds = executionTimeMs / 1000;
        
        assertTrue(executionTimeSeconds > 0, "Execution time should be positive");
        assertTrue(executionTimeSeconds < 30, "Execution should complete within 30 seconds");
        logger.info("✓ Execution completed in {} seconds", executionTimeSeconds);

        // ==================== STEP 9: CLEANUP ====================
        
        logger.info("Step 8: Cleaning up test data");
        dbInitializer.cleanupTestData();
        
        logger.info("=== Single Task Process Execution Test Completed Successfully ===");
        logger.info("Process '{}' successfully printed '{}' three times", processId, randomText);
    }

    @Test
    void testProcessStateTransitions() {
        logger.info("=== Testing Process State Transitions ===");

        // Clean up any existing test data
        dbInitializer.cleanupTestData();

        String randomText = generateRandomText();
        String processId = "state-transition-test-" + System.currentTimeMillis();
        String inputData = String.format("{\"input_text\": \"%s\"}", randomText);

        // Create process record
        ProcessRecordController.ProcessRecordResponse createResponse = processRecordController.createProcessRecord(
            processId, "single-print-task", inputData, null);
        assertTrue(createResponse.isSuccess(), "Process record creation should succeed");

        // Test state transitions
        testStateTransition(processId, "PENDING", "Initial state should be PENDING");

        // Start process
        ProcessController.ProcessStartResponse startResponse = processController.startProcess(processId);
        assertTrue(startResponse.isSuccess(), "Process should start successfully");

        // Wait for state change
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }

        testStateTransition(processId, "IN_PROGRESS", "State should transition to IN_PROGRESS");

        // Wait for completion
        waitForCompletion(processId, 30);

        testStateTransition(processId, "COMPLETED", "Final state should be COMPLETED");

        // Cleanup
        dbInitializer.cleanupTestData();
        logger.info("=== Process State Transitions Test Completed Successfully ===");
    }

    /**
     * Test a specific state transition
     */
    private void testStateTransition(String processId, String expectedStatus, String message) {
        ProcessController.ProcessStateResponse stateResponse = processController.getProcessState(processId);
        assertTrue(stateResponse.isSuccess(), "Should be able to get process state");
        
        ProcessDetails details = stateResponse.getProcessDetails();
        assertEquals(expectedStatus, details.getCurrentStatus(), message);
        logger.info("✓ State transition verified: {} - {}", expectedStatus, message);
    }

    /**
     * Wait for process completion with timeout
     */
    private void waitForCompletion(String processId, int maxWaitSeconds) {
        int waitTime = 0;
        while (waitTime < maxWaitSeconds) {
            try {
                Thread.sleep(1000);
                waitTime++;
                
                ProcessController.ProcessStateResponse stateResponse = processController.getProcessState(processId);
                if (stateResponse.isSuccess()) {
                    ProcessDetails details = stateResponse.getProcessDetails();
                    String status = details.getCurrentStatus();
                    
                    if ("COMPLETED".equals(status)) {
                        logger.info("✓ Process completed after {} seconds", waitTime);
                        return;
                    } else if ("FAILED".equals(status)) {
                        fail("Process failed: " + details.getLastErrorMessage());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Test interrupted while waiting for completion");
            }
        }
        fail("Process did not complete within " + maxWaitSeconds + " seconds");
    }

    /**
     * Generate random text for testing
     */
    private String generateRandomText() {
        String[] words = {"Hello", "World", "Test", "Process", "Orchestrator", "Task", "Execution", "Random", "Text", "Example"};
        Random random = new Random();
        
        StringBuilder text = new StringBuilder();
        int wordCount = random.nextInt(3) + 2; // 2-4 words
        
        for (int i = 0; i < wordCount; i++) {
            if (i > 0) text.append(" ");
            text.append(words[random.nextInt(words.length)]);
        }
        
        return text.toString();
    }
}