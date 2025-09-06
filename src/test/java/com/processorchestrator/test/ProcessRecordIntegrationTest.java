package com.processorchestrator.test;

import com.processorchestrator.config.DatabaseConfig;
import com.processorchestrator.config.ProcessType;
import com.processorchestrator.config.ProcessTypeRegistry;
import com.processorchestrator.controller.ProcessController;
import com.processorchestrator.controller.ProcessRecordController;
import com.processorchestrator.dao.ProcessRecordDAO;
import com.processorchestrator.database.DBInitializer;
import com.processorchestrator.model.ProcessDetails;
import com.processorchestrator.service.ProcessOrchestrator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for ProcessDetails manipulation and process execution
 */
public class ProcessRecordIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(ProcessRecordIntegrationTest.class);

    @TempDir
    Path tempDir;
    
    private DataSource dataSource;
    private ProcessRecordDAO processRecordDAO;
    private ProcessRecordController processRecordController;
    private ProcessController processController;
    private ProcessOrchestrator processOrchestrator;
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
        dbInitializer.initializeDatabase();
        
            // Create services
            processTypeRegistry = createProcessTypeRegistry();
            processOrchestrator = new ProcessOrchestrator(dataSource, processTypeRegistry);
            processOrchestrator.start(); // Start the orchestrator

            processRecordDAO = new ProcessRecordDAO(dataSource);
            processRecordController = new ProcessRecordController(processRecordDAO);
            processController = new ProcessController(processRecordDAO, processOrchestrator, processTypeRegistry);
    }

    @AfterEach
    void tearDown() {
        // Stop the ProcessOrchestrator scheduler
        if (processOrchestrator != null) {
            processOrchestrator.stop();
        }
        
        // Clean up test data using DBInitializer
        if (dbInitializer != null) {
            dbInitializer.cleanupTestData();
        }
    }

    private ProcessTypeRegistry createProcessTypeRegistry() {
        ProcessTypeRegistry registry = new ProcessTypeRegistry();
        
        // Register a simple test process type (Windows compatible)
        ProcessType testProcessType = new ProcessType("test-process", "Test process for integration testing")
                .addTask("task1", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"Task 1 executed\"", System.getProperty("java.io.tmpdir"), 30, 2)
                .addTask("task2", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"Task 2 executed\"", System.getProperty("java.io.tmpdir"), 30, 2);
        
        registry.register(testProcessType);
        
        // Register a ping process type (Windows compatible)
        ProcessType pingProcessType = new ProcessType("ping-process", "Process that performs ping operations")
                .addTask("ping-task", "ping -n 5 8.8.8.8", System.getProperty("java.io.tmpdir"), 30, 2);
        
        registry.register(pingProcessType);
        
        return registry;
    }

    @Test
    void testProcessDetailsCRUD() {
        logger.info("Testing ProcessDetails CRUD operations");
        
        // Test Create
        String recordId = "test-record-001";
        String processType = "test-process";
        String inputData = "input_file:/test/input.json;output_dir:/test/output;user_id:test-user";
        String schedule = "0 2 * * *"; // Daily at 2 AM
        
        ProcessRecordController.ProcessRecordResponse createResponse = 
            processRecordController.createProcessRecord(recordId, processType, inputData, schedule);
        
        assertTrue(createResponse.isSuccess(), "Process record creation should succeed");
        assertNotNull(createResponse.getData(), "Process record data should not be null");
        assertEquals(recordId, createResponse.getData().getId(), "Process record ID should match");
        assertEquals(processType, createResponse.getData().getType(), "Process type should match");
        assertEquals(inputData, createResponse.getData().getInputData(), "Input data should match");
        assertEquals(schedule, createResponse.getData().getSchedule(), "Schedule should match");
        assertEquals("PENDING", createResponse.getData().getCurrentStatus(), "Initial status should be PENDING");
        assertTrue(createResponse.getData().isScheduled(), "Should be marked as scheduled");
        
        // Test Read
        ProcessRecordController.ProcessRecordResponse getResponse = 
            processRecordController.getProcessRecord(recordId);
        
        assertTrue(getResponse.isSuccess(), "Process record retrieval should succeed");
        assertNotNull(getResponse.getData(), "Process record data should not be null");
        assertEquals(recordId, getResponse.getData().getId(), "Process record ID should match");
        
        // Test Update
        String newInputData = "input_file:/test/new_input.json;output_dir:/test/new_output;user_id:updated-user";
        ProcessRecordController.ProcessRecordResponse updateResponse = 
            processRecordController.updateProcessRecord(recordId, processType, newInputData, schedule);
        
        assertTrue(updateResponse.isSuccess(), "Process record update should succeed");
        assertEquals(newInputData, updateResponse.getData().getInputData(), "Input data should be updated");
        
        // Test Delete
        ProcessRecordController.ProcessRecordDeleteResponse deleteResponse = 
            processRecordController.deleteProcessRecord(recordId);
        
        assertTrue(deleteResponse.isSuccess(), "Process record deletion should succeed");
        
        // Verify deletion
        ProcessRecordController.ProcessRecordResponse getAfterDeleteResponse = 
            processRecordController.getProcessRecord(recordId);
        
        assertFalse(getAfterDeleteResponse.isSuccess(), "Process record should not exist after deletion");
    }

    @Test
    void testProcessDetailsListOperations() {
        logger.info("Testing ProcessDetails list operations");
        
        // Create multiple process records
        String[] recordIds = {"test-record-001", "test-record-002", "test-record-003"};
        String[] statuses = {"PENDING", "IN_PROGRESS", "COMPLETED"};
        
        for (int i = 0; i < recordIds.length; i++) {
            ProcessRecordController.ProcessRecordResponse createResponse = 
                processRecordController.createProcessRecord(
                    recordIds[i], "test-process", 
                    "input_file:/test/input.json;output_dir:/test/output", 
                    null);
            
            assertTrue(createResponse.isSuccess(), "Process record creation should succeed");
            
            // Update status for testing
            if (!"PENDING".equals(statuses[i])) {
                processRecordDAO.updateStatus(recordIds[i], statuses[i], java.time.Instant.now(), null);
            }
        }
        
        // Test GetAll
        ProcessRecordController.ProcessRecordListResponse allResponse = 
            processRecordController.getAllProcessRecords();
        
        assertTrue(allResponse.isSuccess(), "Get all process records should succeed");
        assertEquals(3, allResponse.getData().size(), "Should have 3 process records");
        
        // Test GetByStatus
        ProcessRecordController.ProcessRecordListResponse pendingResponse = 
            processRecordController.getProcessRecordsByStatus("PENDING");
        
        assertTrue(pendingResponse.isSuccess(), "Get by status should succeed");
        assertEquals(1, pendingResponse.getData().size(), "Should have 1 PENDING record");
        
        // Test GetScheduled
        ProcessRecordController.ProcessRecordListResponse scheduledResponse = 
            processRecordController.getScheduledProcessRecords();
        
        assertTrue(scheduledResponse.isSuccess(), "Get scheduled records should succeed");
        assertEquals(0, scheduledResponse.getData().size(), "Should have 0 scheduled records");
        
        // Test Statistics
        ProcessRecordController.ProcessRecordStatsResponse statsResponse = 
            processRecordController.getStatistics();
        
        assertTrue(statsResponse.isSuccess(), "Get statistics should succeed");
        assertEquals(3, statsResponse.getData().getTotal(), "Should have 3 total records");
        assertEquals(1, statsResponse.getData().getPending(), "Should have 1 PENDING record");
        assertEquals(1, statsResponse.getData().getInProgress(), "Should have 1 IN_PROGRESS record");
        assertEquals(1, statsResponse.getData().getCompleted(), "Should have 1 COMPLETED record");
    }

    @Test
    void testProcessExecutionViaProcessDetails() {
        logger.info("Testing process execution via ProcessDetails");
        
        // Create a process record
        String recordId = "execution-test-record";
        String inputData = "input_file:/test/input.json;output_dir:/test/output;user_id:test-user";
        
        ProcessRecordController.ProcessRecordResponse createResponse = 
            processRecordController.createProcessRecord(recordId, "test-process", inputData, null);
        
        assertTrue(createResponse.isSuccess(), "Process record creation should succeed");
        
        // Test Start Process
        ProcessController.ProcessStartResponse startResponse = 
            processController.startProcess(recordId);
        
        assertTrue(startResponse.isSuccess(), "Process start should succeed");
        assertNotNull(startResponse.getProcessId(), "Process ID should not be null");
        assertNotNull(startResponse.getOrchestratorProcessId(), "Orchestrator process ID should not be null");
        
        // Verify process record status was updated
        ProcessRecordController.ProcessRecordResponse getResponse = 
            processRecordController.getProcessRecord(recordId);
        
        assertTrue(getResponse.isSuccess(), "Process record retrieval should succeed");
        assertEquals("IN_PROGRESS", getResponse.getData().getCurrentStatus(), "Status should be IN_PROGRESS");
        assertNotNull(getResponse.getData().getTriggeredBy(), "Triggered by should be set");
        
        // Test Get Process State
        ProcessController.ProcessStateResponse stateResponse = 
            processController.getProcessState(recordId);
        
        assertTrue(stateResponse.isSuccess(), "Process state retrieval should succeed");
        assertNotNull(stateResponse.getProcessRecord(), "Process record should not be null");
        assertEquals("IN_PROGRESS", stateResponse.getProcessRecord().getCurrentStatus(), "Status should be IN_PROGRESS");
        
        // Test Stop Process
        ProcessController.ProcessStopResponse stopResponse = 
            processController.stopProcess(recordId);
        
        assertTrue(stopResponse.isSuccess(), "Process stop should succeed");
        
        // Verify process record status was updated
        ProcessRecordController.ProcessRecordResponse getAfterStopResponse = 
            processRecordController.getProcessRecord(recordId);
        
        assertTrue(getAfterStopResponse.isSuccess(), "Process record retrieval should succeed");
        assertEquals("STOPPED", getAfterStopResponse.getData().getCurrentStatus(), "Status should be STOPPED");
    }

    @Test
    void testProcessRestartViaProcessDetails() {
        logger.info("Testing process restart via ProcessDetails");
        
        // Create a process record
        String recordId = "restart-test-record";
        String inputData = "input_file:/test/input.json;output_dir:/test/output;user_id:test-user";
        
        ProcessRecordController.ProcessRecordResponse createResponse = 
            processRecordController.createProcessRecord(recordId, "test-process", inputData, null);
        
        assertTrue(createResponse.isSuccess(), "Process record creation should succeed");
        
        // Start process
        ProcessController.ProcessStartResponse startResponse = 
            processController.startProcess(recordId);
        
        assertTrue(startResponse.isSuccess(), "Process start should succeed");
        
        // Stop process
        ProcessController.ProcessStopResponse stopResponse = 
            processController.stopProcess(recordId);
        
        assertTrue(stopResponse.isSuccess(), "Process stop should succeed");
        
        // Restart process
        ProcessController.ProcessStartResponse restartResponse = 
            processController.restartProcess(recordId);
        
        assertTrue(restartResponse.isSuccess(), "Process restart should succeed");
        assertNotNull(restartResponse.getProcessId(), "Process ID should not be null");
        
        // Verify process record status
        ProcessRecordController.ProcessRecordResponse getResponse = 
            processRecordController.getProcessRecord(recordId);
        
        assertTrue(getResponse.isSuccess(), "Process record retrieval should succeed");
        assertEquals("IN_PROGRESS", getResponse.getData().getCurrentStatus(), "Status should be IN_PROGRESS after restart");
    }

    @Test
    void testProcessHistoryViaProcessDetails() {
        logger.info("Testing process history via ProcessDetails");
        
        // Create a process record
        String recordId = "history-test-record";
        String inputData = "input_file:/test/input.json;output_dir:/test/output;user_id:test-user";
        
        ProcessRecordController.ProcessRecordResponse createResponse = 
            processRecordController.createProcessRecord(recordId, "test-process", inputData, null);
        
        assertTrue(createResponse.isSuccess(), "Process record creation should succeed");
        
        // Start process
        ProcessController.ProcessStartResponse startResponse = 
            processController.startProcess(recordId);
        
        assertTrue(startResponse.isSuccess(), "Process start should succeed");
        
        // Stop process
        ProcessController.ProcessStopResponse stopResponse = 
            processController.stopProcess(recordId);
        
        assertTrue(stopResponse.isSuccess(), "Process stop should succeed");
        
        // Get process history
        ProcessController.ProcessHistoryResponse historyResponse = 
            processController.getProcessHistory(recordId);
        
        assertTrue(historyResponse.isSuccess(), "Process history retrieval should succeed");
        assertNotNull(historyResponse.getHistory(), "Process history should not be null");
        assertEquals(recordId, historyResponse.getHistory().getProcessRecordId(), "Process record ID should match");
        assertEquals("STOPPED", historyResponse.getHistory().getCurrentStatus(), "Current status should be STOPPED");
        assertNotNull(historyResponse.getHistory().getStartedWhen(), "Started time should be set");
        assertNotNull(historyResponse.getHistory().getStoppedWhen(), "Stopped time should be set");
    }

    @Test
    void testErrorHandling() {
        logger.info("Testing error handling");
        
        // Test starting non-existent process record
        ProcessController.ProcessStartResponse startResponse = 
            processController.startProcess("non-existent-record");
        
        assertFalse(startResponse.isSuccess(), "Starting non-existent process should fail");
        assertTrue(startResponse.getMessage().contains("not found"), "Error message should indicate not found");
        
        // Test creating duplicate process record
        String recordId = "duplicate-test-record";
        
        ProcessRecordController.ProcessRecordResponse createResponse1 = 
            processRecordController.createProcessRecord(recordId, "test-process", "input_data:test", null);
        
        assertTrue(createResponse1.isSuccess(), "First creation should succeed");
        
        ProcessRecordController.ProcessRecordResponse createResponse2 = 
            processRecordController.createProcessRecord(recordId, "test-process", "input_data:test", null);
        
        assertFalse(createResponse2.isSuccess(), "Duplicate creation should fail");
        assertTrue(createResponse2.getMessage().contains("already exists"), "Error message should indicate already exists");
        
        // Test deleting running process record
        ProcessRecordController.ProcessRecordResponse createResponse3 = 
            processRecordController.createProcessRecord("running-record", "test-process", "input_data:test", null);
        
        assertTrue(createResponse3.isSuccess(), "Process record creation should succeed");
        
        // Start the process
        ProcessController.ProcessStartResponse startResponse2 = 
            processController.startProcess("running-record");
        
        assertTrue(startResponse2.isSuccess(), "Process start should succeed");
        
        // Try to delete running process record
        ProcessRecordController.ProcessRecordDeleteResponse deleteResponse = 
            processRecordController.deleteProcessRecord("running-record");
        
        assertFalse(deleteResponse.isSuccess(), "Deleting running process record should fail");
        assertTrue(deleteResponse.getMessage().contains("running"), "Error message should indicate running");
    }

    @Test
    void testScheduledProcessDetailss() {
        logger.info("Testing scheduled process records");
        
        // Create scheduled process records
        String[] recordIds = {"scheduled-1", "scheduled-2", "scheduled-3"};
        String[] schedules = {"0 2 * * *", "0 6 * * *", "0 12 * * *"};
        
        for (int i = 0; i < recordIds.length; i++) {
            ProcessRecordController.ProcessRecordResponse createResponse = 
                processRecordController.createProcessRecord(
                    recordIds[i], "test-process", 
                    "input_file:/test/input.json;output_dir:/test/output", 
                    schedules[i]);
            
            assertTrue(createResponse.isSuccess(), "Scheduled process record creation should succeed");
            assertTrue(createResponse.getData().isScheduled(), "Should be marked as scheduled");
            assertEquals(schedules[i], createResponse.getData().getSchedule(), "Schedule should match");
        }
        
        // Test GetScheduled
        ProcessRecordController.ProcessRecordListResponse scheduledResponse = 
            processRecordController.getScheduledProcessRecords();
        
        assertTrue(scheduledResponse.isSuccess(), "Get scheduled records should succeed");
        assertEquals(3, scheduledResponse.getData().size(), "Should have 3 scheduled records");
        
        // Verify all returned records are scheduled
        for (ProcessDetails record : scheduledResponse.getData()) {
            assertTrue(record.isScheduled(), "All returned records should be scheduled");
            assertFalse(record.isManual(), "All returned records should not be manual");
        }
    }

    @Test
    void testMultipleProcessExecution() {
        logger.info("Testing multiple process execution");
        
        // Create three different process records
        String[] recordIds = {"multi-test-1", "multi-test-2", "multi-test-3"};
        String[] processTypes = {"test-process", "test-process", "ping-process"};
        String[] inputData = {
            "input_file:/test/input1.json;output_dir:/test/output1",
            "input_file:/test/input2.json;output_dir:/test/output2", 
            "input_file:/test/input3.json;output_dir:/test/output3"
        };
        
        // Create all three process records
        for (int i = 0; i < recordIds.length; i++) {
            ProcessRecordController.ProcessRecordResponse createResponse = 
                processRecordController.createProcessRecord(recordIds[i], processTypes[i], inputData[i], null);
            
            assertTrue(createResponse.isSuccess(), "Process record creation should succeed for " + recordIds[i]);
            assertEquals(recordIds[i], createResponse.getData().getId());
            assertEquals(processTypes[i], createResponse.getData().getType());
            assertEquals("PENDING", createResponse.getData().getCurrentStatus());
        }
        
        // Start all three processes
        ProcessController.ProcessStartResponse[] startResponses = new ProcessController.ProcessStartResponse[3];
        for (int i = 0; i < recordIds.length; i++) {
            startResponses[i] = processController.startProcess(recordIds[i]);
            assertTrue(startResponses[i].isSuccess(), "Process start should succeed for " + recordIds[i]);
            assertNotNull(startResponses[i].getProcessId());
            assertNotNull(startResponses[i].getOrchestratorProcessId());
            logger.info("Started process {} with orchestrator ID: {}", recordIds[i], startResponses[i].getOrchestratorProcessId());
        }
        
        // Verify all processes are running
        for (int i = 0; i < recordIds.length; i++) {
            ProcessController.ProcessStateResponse stateResponse = processController.getProcessState(recordIds[i]);
            assertTrue(stateResponse.isSuccess(), "Process state retrieval should succeed for " + recordIds[i]);
            assertEquals("IN_PROGRESS", stateResponse.getProcessRecord().getCurrentStatus(), 
                       "Process " + recordIds[i] + " should be IN_PROGRESS");
        }
        
        // Wait a bit for processes to execute
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted while waiting for processes");
        }
        
        // Check final states (some may have completed)
        for (int i = 0; i < recordIds.length; i++) {
            ProcessController.ProcessStateResponse finalStateResponse = processController.getProcessState(recordIds[i]);
            assertTrue(finalStateResponse.isSuccess(), "Final state retrieval should succeed for " + recordIds[i]);
            
            String status = finalStateResponse.getProcessRecord().getCurrentStatus();
            assertTrue(status.equals("IN_PROGRESS") || status.equals("COMPLETED"), 
                      "Process " + recordIds[i] + " should be IN_PROGRESS or COMPLETED, but was " + status);
            
            logger.info("Process {} final status: {}", recordIds[i], status);
        }
        
        logger.info("Multiple process execution test completed successfully");
    }

    @Test
    void testConcurrentProcessExecution() {
        logger.info("Testing concurrent execution of 10 processes");
        
        // Create 10 process records with the same process definition (test-process with 2 tasks)
        String[] processIds = new String[10];
        ProcessRecordController.ProcessRecordResponse[] responses = new ProcessRecordController.ProcessRecordResponse[10];
        
        for (int i = 0; i < 10; i++) {
            processIds[i] = "concurrent-test-" + (i + 1);
            String inputData = "input_file:/test/input" + (i + 1) + ".json;output_dir:/test/output" + (i + 1);
            
            // All processes use the same definition: test-process (2 tasks)
            responses[i] = processRecordController.createProcessRecord(
                processIds[i], "test-process", inputData, null);
            
            assertTrue(responses[i].isSuccess(), "Process record " + (i + 1) + " creation should succeed");
            assertEquals(processIds[i], responses[i].getData().getId());
            assertEquals("test-process", responses[i].getData().getType());
            assertEquals("PENDING", responses[i].getData().getCurrentStatus());
        }
        
        // Start all 10 processes concurrently
        ProcessController.ProcessStartResponse[] startResponses = new ProcessController.ProcessStartResponse[10];
        for (int i = 0; i < 10; i++) {
            startResponses[i] = processController.startProcess(processIds[i]);
            assertTrue(startResponses[i].isSuccess(), "Process " + (i + 1) + " start should succeed");
            assertNotNull(startResponses[i].getProcessId());
            assertNotNull(startResponses[i].getOrchestratorProcessId());
            logger.info("Started process {} with orchestrator ID: {}", processIds[i], startResponses[i].getOrchestratorProcessId());
        }
        
        // Wait for processes to complete (test-process has 2 tasks, so should complete relatively quickly)
        try {
            Thread.sleep(20000); // Wait 20 seconds for all processes to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted while waiting for processes");
        }
        
        // Check final status of all processes
        ProcessController.ProcessStateResponse[] stateResponses = new ProcessController.ProcessStateResponse[10];
        for (int i = 0; i < 10; i++) {
            stateResponses[i] = processController.getProcessState(processIds[i]);
            assertTrue(stateResponses[i].isSuccess(), "Process " + (i + 1) + " state check should succeed");
            logger.info("Process {} final status: {}", processIds[i], stateResponses[i].getProcessRecord().getCurrentStatus());
        }
        
        // Count process statuses
        int completedCount = 0;
        int inProgressCount = 0;
        int failedCount = 0;
        
        for (int i = 0; i < 10; i++) {
            String status = stateResponses[i].getProcessRecord().getCurrentStatus();
            switch (status) {
                case "COMPLETED":
                    completedCount++;
                    break;
                case "IN_PROGRESS":
                    inProgressCount++;
                    break;
                case "FAILED":
                    failedCount++;
                    break;
            }
        }
        
        logger.info("Concurrent execution results:");
        logger.info("  Completed: {}", completedCount);
        logger.info("  In Progress: {}", inProgressCount);
        logger.info("  Failed: {}", failedCount);
        
        // Most processes should have completed (test-process with 2 simple tasks)
        assertTrue(completedCount >= 8, "At least 8 out of 10 processes should have completed");
        
        logger.info("Concurrent process execution test completed successfully");
    }
}
