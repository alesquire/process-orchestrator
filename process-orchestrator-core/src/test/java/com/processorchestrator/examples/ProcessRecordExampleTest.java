package com.processorchestrator.examples;

import com.processorchestrator.config.DatabaseConfig;
import com.processorchestrator.config.ProcessType;
import com.processorchestrator.config.ProcessTypeRegistry;
import com.processorchestrator.config.ProcessTypeInitializer;
import com.processorchestrator.controller.ProcessController;
import com.processorchestrator.controller.ProcessRecordController;
import com.processorchestrator.dao.ProcessRecordDAO;
import com.processorchestrator.database.DBInitializer;
import com.processorchestrator.model.ProcessDetails;
import com.processorchestrator.service.ProcessOrchestrator;
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
 * Example demonstrating ProcessRecord creation and process execution
 * 
 * This example shows:
 * 1. Creating three ProcessRecords with different task counts (1, 2, 3 tasks)
 * 2. Each process has different input data
 * 3. Starting all processes and monitoring their execution
 */
public class ProcessRecordExampleTest {
    private static final Logger logger = LoggerFactory.getLogger(ProcessRecordExampleTest.class);

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
        
        // Initialize process type registry and orchestrator
        processTypeRegistry = new ProcessTypeRegistry();
        processOrchestrator = new ProcessOrchestrator(dataSource, processTypeRegistry);
        processController = new ProcessController(processRecordDAO, processOrchestrator, processTypeRegistry);
        processRecordController = new ProcessRecordController(processRecordDAO, processTypeRegistry);
        
        // Register process types
        ProcessTypeInitializer.registerDefaultProcessTypes(processTypeRegistry);
    }

    @Test
    void testProcessRecordCreationAndExecution() {
        logger.info("=== Starting ProcessRecord Creation and Execution Example ===");

        // Clean up any existing test data
        dbInitializer.cleanupTestData();

        // ==================== STEP 1: CREATE PROCESS RECORDS ====================
        
        logger.info("Step 1: Creating ProcessRecords with different task counts...");

        // Create ProcessRecord 1: Single Task Process
        String processId1 = "example-single-task-" + System.currentTimeMillis();
        String inputData1 = "{\"input_file\": \"" + System.getProperty("java.io.tmpdir") + "/sample.csv\", \"validation_rules\": \"strict\"}";
        
        ProcessRecordController.ProcessRecordResponse response1 = processRecordController.createProcessRecord(
            processId1, "single-task-process", inputData1, null);
        
        assertTrue(response1.isSuccess(), "Single task process record creation should succeed");
        assertEquals(processId1, response1.getData().getId());
        assertEquals("single-task-process", response1.getData().getType());
        assertEquals(inputData1, response1.getData().getInputData());
        assertEquals("PENDING", response1.getData().getCurrentStatus());
        logger.info("✓ Created ProcessRecord 1: {} (1 task)", processId1);

        // Create ProcessRecord 2: Two Task Process
        String processId2 = "example-two-task-" + System.currentTimeMillis();
        String inputData2 = "{\"input_file\": \"" + System.getProperty("java.io.tmpdir") + "/raw_data.json\", \"output_dir\": \"" + System.getProperty("java.io.tmpdir") + "/output\", \"format\": \"json\"}";
        
        ProcessRecordController.ProcessRecordResponse response2 = processRecordController.createProcessRecord(
            processId2, "two-task-process", inputData2, "0 2 * * *"); // Scheduled for 2 AM daily
        
        assertTrue(response2.isSuccess(), "Two task process record creation should succeed");
        assertEquals(processId2, response2.getData().getId());
        assertEquals("two-task-process", response2.getData().getType());
        assertEquals(inputData2, response2.getData().getInputData());
        assertEquals("0 2 * * *", response2.getData().getSchedule());
        assertEquals("PENDING", response2.getData().getCurrentStatus());
        logger.info("✓ Created ProcessRecord 2: {} (2 tasks)", processId2);

        // Create ProcessRecord 3: Three Task Process
        String processId3 = "example-three-task-" + System.currentTimeMillis();
        String inputData3 = "{\"input_file\": \"" + System.getProperty("java.io.tmpdir") + "/large_dataset.csv\", \"output_dir\": \"" + System.getProperty("java.io.tmpdir") + "/results\", \"analysis_type\": \"comprehensive\"}";
        
        ProcessRecordController.ProcessRecordResponse response3 = processRecordController.createProcessRecord(
            processId3, "three-task-process", inputData3, null);
        
        assertTrue(response3.isSuccess(), "Three task process record creation should succeed");
        assertEquals(processId3, response3.getData().getId());
        assertEquals("three-task-process", response3.getData().getType());
        assertEquals(inputData3, response3.getData().getInputData());
        assertEquals("PENDING", response3.getData().getCurrentStatus());
        logger.info("✓ Created ProcessRecord 3: {} (3 tasks)", processId3);

        // ==================== STEP 2: VERIFY PROCESS RECORDS ====================
        
        logger.info("Step 2: Verifying ProcessRecords in database...");

        // Verify all three records exist
        Optional<ProcessDetails> record1 = processRecordDAO.findById(processId1);
        Optional<ProcessDetails> record2 = processRecordDAO.findById(processId2);
        Optional<ProcessDetails> record3 = processRecordDAO.findById(processId3);

        assertTrue(record1.isPresent(), "ProcessRecord 1 should exist in database");
        assertTrue(record2.isPresent(), "ProcessRecord 2 should exist in database");
        assertTrue(record3.isPresent(), "ProcessRecord 3 should exist in database");

        // Verify process types and task counts
        ProcessType type1 = processTypeRegistry.getProcessType("single-task-process");
        ProcessType type2 = processTypeRegistry.getProcessType("two-task-process");
        ProcessType type3 = processTypeRegistry.getProcessType("three-task-process");

        assertEquals(1, type1.getTaskCount(), "Single task process should have 1 task");
        assertEquals(2, type2.getTaskCount(), "Two task process should have 2 tasks");
        assertEquals(3, type3.getTaskCount(), "Three task process should have 3 tasks");

        logger.info("✓ Verified ProcessRecords: 1 task, 2 tasks, 3 tasks");

        // ==================== STEP 3: START PROCESSES ====================
        
        logger.info("Step 3: Starting all processes...");

        // Start Process 1: Single Task
        ProcessController.ProcessStartResponse startResponse1 = processController.startProcess(processId1);
        assertTrue(startResponse1.isSuccess(), "Single task process should start successfully");
        logger.info("✓ Started Process 1: {} (orchestrator ID: {})", processId1, startResponse1.getOrchestratorProcessId());

        // Start Process 2: Two Tasks
        ProcessController.ProcessStartResponse startResponse2 = processController.startProcess(processId2);
        assertTrue(startResponse2.isSuccess(), "Two task process should start successfully");
        logger.info("✓ Started Process 2: {} (orchestrator ID: {})", processId2, startResponse2.getOrchestratorProcessId());

        // Start Process 3: Three Tasks
        ProcessController.ProcessStartResponse startResponse3 = processController.startProcess(processId3);
        assertTrue(startResponse3.isSuccess(), "Three task process should start successfully");
        logger.info("✓ Started Process 3: {} (orchestrator ID: {})", processId3, startResponse3.getOrchestratorProcessId());

        // ==================== STEP 4: MONITOR PROCESS STATUS ====================
        
        logger.info("Step 4: Monitoring process status...");

        // Check status of all processes
        ProcessController.ProcessStateResponse state1 = processController.getProcessState(processId1);
        ProcessController.ProcessStateResponse state2 = processController.getProcessState(processId2);
        ProcessController.ProcessStateResponse state3 = processController.getProcessState(processId3);

        assertTrue(state1.isSuccess(), "Should be able to get state for process 1");
        assertTrue(state2.isSuccess(), "Should be able to get state for process 2");
        assertTrue(state3.isSuccess(), "Should be able to get state for process 3");

        logger.info("✓ Process 1 Status: {}", state1.getProcessDetails().getCurrentStatus());
        logger.info("✓ Process 2 Status: {}", state2.getProcessDetails().getCurrentStatus());
        logger.info("✓ Process 3 Status: {}", state3.getProcessDetails().getCurrentStatus());

        // ==================== STEP 5: DISPLAY PROCESS DETAILS ====================
        
        logger.info("Step 5: Displaying process details...");

        // Display detailed information for each process
        displayProcessDetails(processId1, "Single Task Process");
        displayProcessDetails(processId2, "Two Task Process");
        displayProcessDetails(processId3, "Three Task Process");

        // ==================== STEP 6: CLEANUP ====================
        
        logger.info("Step 6: Cleaning up test data...");
        dbInitializer.cleanupTestData();
        
        logger.info("=== ProcessRecord Creation and Execution Example Completed Successfully ===");
    }

    /**
     * Display detailed information about a process
     */
    private void displayProcessDetails(String processId, String description) {
        logger.info("--- {} Details ---", description);
        
        // Get process record
        Optional<ProcessDetails> record = processRecordDAO.findById(processId);
        if (record.isPresent()) {
            ProcessDetails details = record.get();
            logger.info("Process ID: {}", details.getId());
            logger.info("Type: {}", details.getType());
            logger.info("Status: {}", details.getCurrentStatus());
            logger.info("Input Data: {}", details.getInputData());
            logger.info("Schedule: {}", details.getSchedule() != null ? details.getSchedule() : "Manual only");
            logger.info("Created: {}", details.getCreatedAt());
            logger.info("Updated: {}", details.getUpdatedAt());
            
            if (details.getStartedWhen() != null) {
                logger.info("Started: {}", details.getStartedWhen());
            }
            if (details.getCompletedWhen() != null) {
                logger.info("Completed: {}", details.getCompletedWhen());
            }
            if (details.getFailedWhen() != null) {
                logger.info("Failed: {}", details.getFailedWhen());
                logger.info("Error: {}", details.getLastErrorMessage());
            }
        }
        
        // Get process state
        ProcessController.ProcessStateResponse state = processController.getProcessState(processId);
        if (state.isSuccess()) {
            ProcessDetails stateDetails = state.getProcessDetails();
            logger.info("Current Task Index: {}/{}", stateDetails.getCurrentTaskIndex(), stateDetails.getTotalTasks());
            logger.info("Triggered By: {}", stateDetails.getTriggeredBy() != null ? stateDetails.getTriggeredBy() : "Manual");
        }
        
        logger.info("--- End {} Details ---", description);
    }
}