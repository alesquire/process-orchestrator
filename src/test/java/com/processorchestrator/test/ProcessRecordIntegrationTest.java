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
        DBInitializer dbInitializer = new DBInitializer(dataSource);
        dbInitializer.initializeDatabase();
        
            // Create services
            ProcessTypeRegistry registry = createProcessTypeRegistry();
            processOrchestrator = new ProcessOrchestrator(dataSource, registry);

            processRecordDAO = new ProcessRecordDAO(dataSource);
            processRecordController = new ProcessRecordController(processRecordDAO);
            processController = new ProcessController(processRecordDAO, processOrchestrator);
    }

    private ProcessTypeRegistry createProcessTypeRegistry() {
        ProcessTypeRegistry registry = new ProcessTypeRegistry();
        
        // Register a simple test process type
        ProcessType testProcessType = new ProcessType("test-process", "Test process for integration testing")
                .addTask("task1", "echo 'Task 1 executed'", System.getProperty("java.io.tmpdir"), 30, 2)
                .addTask("task2", "echo 'Task 2 executed'", System.getProperty("java.io.tmpdir"), 30, 2);
        
        registry.register(testProcessType);
        
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
}
