package com.processorchestrator.test;

import com.processorchestrator.ProcessOrchestratorApp;
import com.processorchestrator.config.ProcessType;
import com.processorchestrator.config.ProcessTypeRegistry;
import com.processorchestrator.model.ProcessInputData;
import com.processorchestrator.service.ProcessOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

/**
 * Integration test for Process Orchestrator
 */
class ProcessOrchestratorIntegrationTest {

    private DataSource dataSource;
    private ProcessOrchestratorApp app;
    private ProcessOrchestrator orchestrator;

    @BeforeEach
    void setUp() throws SQLException {
        // Create in-memory H2 database for testing
        dataSource = new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", username, password);
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

        // Initialize database schema
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Create scheduled_tasks table (db-scheduler requirement)
            String createScheduledTasksTable = """
                CREATE TABLE IF NOT EXISTS scheduled_tasks (
                    task_name VARCHAR(255) NOT NULL,
                    task_instance VARCHAR(255) NOT NULL,
                    task_data BLOB,
                    execution_time TIMESTAMP NOT NULL,
                    picked BOOLEAN NOT NULL DEFAULT FALSE,
                    picked_by VARCHAR(50),
                    last_success TIMESTAMP,
                    last_failure TIMESTAMP,
                    last_heartbeat TIMESTAMP,
                    version BIGINT NOT NULL DEFAULT 0,
                    PRIMARY KEY (task_name, task_instance)
                )
                """;
            
            statement.execute(createScheduledTasksTable);
            
            // Create processes table for monitoring
            String createProcessesTable = """
                CREATE TABLE IF NOT EXISTS processes (
                    process_id VARCHAR(255) PRIMARY KEY,
                    process_type VARCHAR(255) NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    current_task_index INTEGER NOT NULL DEFAULT 0,
                    total_tasks INTEGER NOT NULL,
                    started_at TIMESTAMP,
                    completed_at TIMESTAMP,
                    error_message TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            
            statement.execute(createProcessesTable);
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize test database", e);
        }

        app = new ProcessOrchestratorApp(dataSource);
        orchestrator = app.getProcessOrchestrator();
    }

    @AfterEach
    void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    void testProcessTypeRegistration() {
        ProcessTypeRegistry registry = app.getProcessTypeRegistry();
        
        assertNotNull(registry.getProcessType("data-processing-pipeline"));
        assertNotNull(registry.getProcessType("deployment-pipeline"));
        assertNotNull(registry.getProcessType("backup-process"));
        assertNotNull(registry.getProcessType("etl-pipeline"));
        
        assertEquals(4, registry.getAllProcessTypes().size());
    }

    @Test
    void testStartProcess() {
        app.start();
        
        ProcessInputData inputData = new ProcessInputData("/data/input.csv", "/data/output");
        inputData.addConfig("batch_size", "1000");
        inputData.setUserId("test-user");
        
        String processId = orchestrator.startProcess("data-processing-pipeline", inputData);
        
        assertNotNull(processId);
        assertTrue(processId.startsWith("process-"));
        assertTrue(processId.length() > 20); // Should have timestamp and UUID
    }

    @Test
    void testStartProcessWithCustomId() {
        app.start();
        
        ProcessInputData inputData = new ProcessInputData("/data/input.csv", "/data/output");
        String customId = "custom-process-123";
        
        String processId = orchestrator.startProcess("data-processing-pipeline", inputData, customId);
        
        assertEquals(customId, processId);
    }

    @Test
    void testStartNonExistentProcessType() {
        app.start();
        
        ProcessInputData inputData = new ProcessInputData("/data/input.csv", "/data/output");
        
        assertThrows(IllegalArgumentException.class, () -> {
            orchestrator.startProcess("non-existent-process", inputData);
        });
    }

    @Test
    void testProcessExecutionWithEchoCommands() throws InterruptedException {
        // Create a simple process type with echo commands for testing
        ProcessTypeRegistry registry = app.getProcessTypeRegistry();
        ProcessType testProcess = new ProcessType("test-process", "Test process with echo commands")
                .addTask("echo1", "echo 'Task 1 completed'", "/tmp", 1, 1)
                .addTask("echo2", "echo 'Task 2 completed'", "/tmp", 1, 1)
                .addTask("echo3", "echo 'Task 3 completed'", "/tmp", 1, 1);
        
        registry.register(testProcess);
        
        app.start();
        
        ProcessInputData inputData = new ProcessInputData("/tmp", "/tmp");
        String processId = orchestrator.startProcess("test-process", inputData);
        
        assertNotNull(processId);
        
        // Wait for process to complete (with timeout)
        Thread.sleep(5000); // Wait 5 seconds for process to complete
        
        // Note: In a real test, you would query the database to verify the process completed
        // For now, we just verify that the process was started without errors
    }

    @Test
    void testMultipleProcesses() {
        app.start();
        
        ProcessInputData inputData1 = new ProcessInputData("/data/input1.csv", "/data/output1");
        ProcessInputData inputData2 = new ProcessInputData("/data/input2.csv", "/data/output2");
        
        String processId1 = orchestrator.startProcess("data-processing-pipeline", inputData1);
        String processId2 = orchestrator.startProcess("deployment-pipeline", inputData2);
        
        assertNotNull(processId1);
        assertNotNull(processId2);
        assertNotEquals(processId1, processId2);
    }

    @Test
    void testProcessInputDataContext() {
        ProcessInputData inputData = new ProcessInputData("/data/input.csv", "/data/output");
        inputData.addConfig("batch_size", "1000");
        inputData.addConfig("format", "json");
        inputData.setUserId("test-user");
        inputData.addMetadata("priority", "high");
        inputData.addMetadata("retry_count", 3);
        
        assertEquals("/data/input.csv", inputData.getInputFile());
        assertEquals("/data/output", inputData.getOutputDir());
        assertEquals("test-user", inputData.getUserId());
        assertEquals("1000", inputData.getConfig().get("batch_size"));
        assertEquals("json", inputData.getConfig().get("format"));
        assertEquals("high", inputData.getMetadata().get("priority"));
        assertEquals(3, inputData.getMetadata().get("retry_count"));
    }
}
