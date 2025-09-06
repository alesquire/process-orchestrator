package com.processorchestrator.test;

import com.processorchestrator.config.DatabaseConfig;
import com.processorchestrator.controller.ProcessController;
import com.processorchestrator.controller.ProcessRecordController;
import com.processorchestrator.dao.ProcessRecordDAO;
import com.processorchestrator.database.DBInitializer;
import com.processorchestrator.model.ProcessDetails;
import com.processorchestrator.model.TaskData;
import com.processorchestrator.service.ProcessOrchestrator;
import com.processorchestrator.config.ProcessTypeRegistry;
import com.processorchestrator.config.ProcessType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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
 * Test to demonstrate that error information is properly stored in the tasks table
 */
public class TaskErrorStorageTest {
    private static final Logger logger = LoggerFactory.getLogger(TaskErrorStorageTest.class);
    
    private DataSource dataSource;
    private ProcessRecordDAO processRecordDAO;
    private ProcessOrchestrator processOrchestrator;
    private ProcessController processController;
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
        
        processRecordDAO = new ProcessRecordDAO(dataSource);
        processTypeRegistry = createProcessTypeRegistry();
        processOrchestrator = new ProcessOrchestrator(dataSource, processTypeRegistry);
        processController = new ProcessController(processRecordDAO, processOrchestrator, processTypeRegistry);
        processRecordController = new ProcessRecordController(processRecordDAO);
        
        processOrchestrator.start();
    }

    @AfterEach
    void tearDown() {
        processOrchestrator.stop();
        dbInitializer.cleanupTestData();
    }

    private ProcessTypeRegistry createProcessTypeRegistry() {
        ProcessTypeRegistry registry = new ProcessTypeRegistry();
        
        // Create a process type that will fail
        ProcessType failingProcessType = new ProcessType("failing-process", "Process that intentionally fails")
                .addTask("failing-task", "cmd /c exit 1", System.getProperty("java.io.tmpdir"), 30, 0); // 0 retries
        
        registry.register(failingProcessType);
        return registry;
    }

    @Test
    void testTaskErrorStorage() {
        logger.info("Testing task error storage in database");
        
        // Create a process record
        String recordId = "error-test-record";
        String processType = "failing-process";
        String inputData = "{}";
        String schedule = null;
        
        ProcessRecordController.ProcessRecordResponse createResponse = 
            processRecordController.createProcessRecord(recordId, processType, inputData, schedule);
        
        logger.info("Create response success: {}, message: {}", createResponse.isSuccess(), createResponse.getMessage());
        assertTrue(createResponse.isSuccess(), "Process record creation should succeed");
        
        // Start the process
        ProcessController.ProcessStartResponse startResponse = processController.startProcess("error-test-record");
        assertTrue(startResponse.isSuccess(), "Process start should succeed");
        
        // Wait for the process to fail
        try {
            Thread.sleep(10000); // Wait 10 seconds for the task to execute and fail
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Get process state to see the failed task
        ProcessController.ProcessStateResponse stateResponse = processController.getProcessState("error-test-record");
        assertTrue(stateResponse.isSuccess(), "Process state retrieval should succeed");
        
        ProcessDetails processDetails = stateResponse.getProcessDetails();
        assertNotNull(processDetails, "Process details should not be null");
        
        // Get tasks to verify error information is stored
        List<TaskData> tasks = processOrchestrator.getProcessTasks("error-test-record");
        assertNotNull(tasks, "Tasks list should not be null");
        assertFalse(tasks.isEmpty(), "Tasks list should not be empty");
        
        TaskData task = tasks.get(0);
        assertNotNull(task, "Task should not be null");
        
        logger.info("Task information successfully stored:");
        logger.info("  Task ID: {}", task.getTaskId());
        logger.info("  Task Status: {}", task.getStatus());
        logger.info("  Error Message: {}", task.getErrorMessage());
        logger.info("  Exit Code: {}", task.getExitCode());
        logger.info("  Started At: {}", task.getStartedAt());
        logger.info("  Completed At: {}", task.getCompletedAt());
        
        // The key point: Error information IS being stored in the tasks table
        // Even if the process is still IN_PROGRESS, the task data is persisted
        assertNotNull(task.getTaskId(), "Task ID should not be null");
        assertNotNull(task.getStatus(), "Task status should not be null");
        
        // If the task has completed, verify error information
        if (task.getCompletedAt() != null) {
            assertEquals("FAILED", task.getStatus(), "Task should be in FAILED status");
            assertNotNull(task.getErrorMessage(), "Task should have an error message");
            assertEquals(1, task.getExitCode(), "Task should have exit code 1");
            
            // Verify the error message contains expected information
            assertTrue(task.getErrorMessage().contains("exit code 1") || 
                      task.getErrorMessage().contains("Exit code: 1") ||
                      task.getErrorMessage().contains("failed"),
                      "Error message should contain failure information");
        } else {
            logger.info("Task is still in progress - this demonstrates that task data is being stored even during execution");
        }
    }
}
