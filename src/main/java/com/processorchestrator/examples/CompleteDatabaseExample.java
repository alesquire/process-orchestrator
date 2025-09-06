package com.processorchestrator.examples;

import com.processorchestrator.api.ProcessApiService;
import com.processorchestrator.controller.ProcessController;
import com.processorchestrator.service.ProcessManager;
import com.processorchestrator.service.ProcessOrchestrator;
import com.processorchestrator.service.ProcessResultService;
import com.processorchestrator.config.ProcessTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Complete example showing how to work with the Process Orchestrator using database-driven approach
 * 
 * This example demonstrates:
 * 1. Creating processes in the database
 * 2. Starting processes via API
 * 3. Monitoring process execution
 * 4. Managing process lifecycle
 * 5. Scheduled vs manual processes
 */
public class CompleteDatabaseExample {
    private static final Logger logger = LoggerFactory.getLogger(CompleteDatabaseExample.class);

    public static void main(String[] args) {
        logger.info("=== Complete Database-Driven Process Orchestrator Example ===");
        
        try {
            // Setup
            DataSource dataSource = createDataSource();
            initializeDatabase(dataSource);
            
            ProcessTypeRegistry registry = createProcessTypeRegistry();
            ProcessOrchestrator orchestrator = new ProcessOrchestrator(dataSource, registry);
            ProcessResultService resultService = new ProcessResultService(dataSource);
            ProcessManager processManager = new ProcessManager(dataSource);
            ProcessController controller = new ProcessController(processManager, orchestrator, resultService);
            ProcessApiService apiService = new ProcessApiService(controller, processManager);
            
            // Start the orchestrator
            orchestrator.start();
            
            // Run comprehensive examples
            runCompleteExamples(apiService, controller, processManager);
            
            // Stop the orchestrator
            orchestrator.stop();
            
        } catch (Exception e) {
            logger.error("Error running complete database example", e);
        }
    }

    private static DataSource createDataSource() {
        return new DataSource() {
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
    }

    private static void initializeDatabase(DataSource dataSource) {
        logger.info("Initializing database schema...");
        
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Create all necessary tables
            String[] createStatements = {
                """
                CREATE TABLE IF NOT EXISTS processes (
                    id VARCHAR(255) PRIMARY KEY,
                    type VARCHAR(255) NOT NULL,
                    input_data TEXT NOT NULL,
                    schedule VARCHAR(255),
                    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                    current_task_index INTEGER NOT NULL DEFAULT 0,
                    total_tasks INTEGER NOT NULL,
                    started_at TIMESTAMP,
                    completed_at TIMESTAMP,
                    error_message TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS tasks (
                    task_id VARCHAR(255) PRIMARY KEY,
                    process_id VARCHAR(255) NOT NULL,
                    task_index INTEGER NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    command TEXT NOT NULL,
                    working_directory VARCHAR(500),
                    timeout_minutes INTEGER NOT NULL DEFAULT 60,
                    max_retries INTEGER NOT NULL DEFAULT 3,
                    retry_count INTEGER NOT NULL DEFAULT 0,
                    status VARCHAR(50) NOT NULL,
                    started_at TIMESTAMP,
                    completed_at TIMESTAMP,
                    error_message TEXT,
                    exit_code INTEGER,
                    output TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (process_id) REFERENCES processes(id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS process_executions (
                    execution_id VARCHAR(255) PRIMARY KEY,
                    process_id VARCHAR(255) NOT NULL,
                    execution_started_at TIMESTAMP NOT NULL,
                    execution_completed_at TIMESTAMP,
                    execution_status VARCHAR(50) NOT NULL,
                    triggered_by VARCHAR(50) NOT NULL,
                    error_message TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (process_id) REFERENCES processes(id) ON DELETE CASCADE
                )
                """
            };
            
            for (String sql : createStatements) {
                statement.execute(sql);
            }
            
            logger.info("Database schema initialized successfully");
            
        } catch (SQLException e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private static ProcessTypeRegistry createProcessTypeRegistry() {
        ProcessTypeRegistry registry = new ProcessTypeRegistry();
        
        // Register data processing pipeline
        registry.register(new com.processorchestrator.config.ProcessType("data-processing-pipeline", "Complete data processing pipeline")
                .addTask("load", "python scripts/load_data.py ${input_file} ${output_dir}/loaded_data.json", "/data", 30, 2)
                .addTask("process", "python scripts/process_data.py ${output_dir}/loaded_data.json ${output_dir}/processed_data.json", "/data", 60, 3)
                .addTask("generate", "python scripts/generate_report.py ${output_dir}/processed_data.json ${output_dir}/report.html", "/data", 45, 2)
                .addTask("analyze", "python scripts/analyze_results.py ${output_dir}/report.html ${output_dir}/analysis.json", "/data", 30, 2));
        
        return registry;
    }

    private static void runCompleteExamples(ProcessApiService apiService, ProcessController controller, ProcessManager processManager) {
        logger.info("Running complete examples...");
        
        // Example 1: Create and manage multiple processes
        demonstrateProcessManagement(apiService);
        
        // Example 2: Scheduled vs manual processes
        demonstrateScheduledProcesses(apiService);
        
        // Example 3: Process lifecycle management
        demonstrateProcessLifecycle(apiService, controller);
        
        // Example 4: Error handling and recovery
        demonstrateErrorHandling(apiService);
        
        // Example 5: Process monitoring and reporting
        demonstrateMonitoring(apiService, processManager);
    }

    private static void demonstrateProcessManagement(ProcessApiService apiService) {
        logger.info("\n=== Example 1: Process Management ===");
        
        // Create multiple processes
        String[] processIds = {
            "daily-report-process",
            "weekly-backup-process", 
            "monthly-cleanup-process"
        };
        
        String[] schedules = {
            "0 6 * * *",    // Daily at 6 AM
            "0 2 * * 0",    // Weekly on Sunday at 2 AM
            "0 1 1 * *"     // Monthly on 1st at 1 AM
        };
        
        for (int i = 0; i < processIds.length; i++) {
            String processId = processIds[i];
            String schedule = schedules[i];
            String inputData = String.format("input_file:/data/%s.json;output_dir:/data/%s_output;user_id:system", 
                                            processId, processId);
            
            // Create process via API
            ProcessApiService.ApiResponse<ProcessManager.ProcessRecord> createResponse = 
                apiService.createProcess(processId, "data-processing-pipeline", inputData, schedule);
            
            if (createResponse.isSuccess()) {
                logger.info("Created process: {} with schedule: {}", processId, schedule);
            } else {
                logger.error("Failed to create process: {} - {}", processId, createResponse.getMessage());
            }
        }
        
        // List all processes
        ProcessApiService.ApiResponse<List<ProcessManager.ProcessRecord>> allProcessesResponse = 
            apiService.getAllProcesses();
        
        if (allProcessesResponse.isSuccess()) {
            logger.info("Total processes created: {}", allProcessesResponse.getData().size());
            for (ProcessManager.ProcessRecord process : allProcessesResponse.getData()) {
                logger.info("  - {}: {} (schedule: {})", process.getId(), process.getStatus(), process.getSchedule());
            }
        }
    }

    private static void demonstrateScheduledProcesses(ProcessApiService apiService) {
        logger.info("\n=== Example 2: Scheduled Processes ===");
        
        // Get scheduled processes
        ProcessApiService.ApiResponse<List<ProcessManager.ProcessRecord>> scheduledResponse = 
            apiService.getScheduledProcesses();
        
        if (scheduledResponse.isSuccess()) {
            logger.info("Scheduled processes: {}", scheduledResponse.getData().size());
            for (ProcessManager.ProcessRecord process : scheduledResponse.getData()) {
                logger.info("  - {}: schedule={}", process.getId(), process.getSchedule());
            }
        }
        
        // Create a manual-only process
        ProcessApiService.ApiResponse<ProcessManager.ProcessRecord> manualResponse = 
            apiService.createProcess("manual-test-process", "data-processing-pipeline", 
                                   "input_file:/data/manual.json;output_dir:/data/manual_output", null);
        
        if (manualResponse.isSuccess()) {
            logger.info("Created manual process: {}", manualResponse.getData().getId());
        }
    }

    private static void demonstrateProcessLifecycle(ProcessApiService apiService, ProcessController controller) {
        logger.info("\n=== Example 3: Process Lifecycle ===");
        
        String processId = "lifecycle-demo-process";
        
        // Create process
        ProcessApiService.ApiResponse<ProcessManager.ProcessRecord> createResponse = 
            apiService.createProcess(processId, "data-processing-pipeline", 
                                   "input_file:/data/lifecycle.json;output_dir:/data/lifecycle_output", null);
        
        if (!createResponse.isSuccess()) {
            logger.error("Failed to create process: {}", createResponse.getMessage());
            return;
        }
        
        // Start process
        ProcessApiService.ApiResponse<ProcessController.ProcessStartResponse> startResponse = 
            apiService.startProcess(processId);
        
        if (startResponse.isSuccess()) {
            logger.info("Process started: {}", startResponse.getData().getMessage());
            
            // Monitor for a short time
            monitorProcessBriefly(apiService, processId);
            
            // Get process state
            ProcessApiService.ApiResponse<ProcessController.ProcessStateResponse> stateResponse = 
                apiService.getProcessState(processId);
            
            if (stateResponse.isSuccess()) {
                ProcessManager.ProcessRecord process = stateResponse.getData().getProcessRecord();
                logger.info("Process state: {} (task {}/{})", 
                          process.getStatus(), process.getCurrentTaskIndex(), process.getTotalTasks());
            }
        } else {
            logger.error("Failed to start process: {}", startResponse.getMessage());
        }
    }

    private static void demonstrateErrorHandling(ProcessApiService apiService) {
        logger.info("\n=== Example 4: Error Handling ===");
        
        // Try to start non-existent process
        ProcessApiService.ApiResponse<ProcessController.ProcessStartResponse> errorResponse = 
            apiService.startProcess("non-existent-process");
        
        if (!errorResponse.isSuccess()) {
            logger.info("Expected error when starting non-existent process: {}", errorResponse.getMessage());
        }
        
        // Try to create duplicate process
        String duplicateId = "duplicate-process";
        apiService.createProcess(duplicateId, "data-processing-pipeline", "input_data:test", null);
        
        ProcessApiService.ApiResponse<ProcessManager.ProcessRecord> duplicateResponse = 
            apiService.createProcess(duplicateId, "data-processing-pipeline", "input_data:test", null);
        
        if (!duplicateResponse.isSuccess()) {
            logger.info("Expected error when creating duplicate process: {}", duplicateResponse.getMessage());
        }
    }

    private static void demonstrateMonitoring(ProcessApiService apiService, ProcessManager processManager) {
        logger.info("\n=== Example 5: Monitoring and Reporting ===");
        
        // Get processes by status
        String[] statuses = {"PENDING", "IN_PROGRESS", "COMPLETED", "FAILED"};
        
        for (String status : statuses) {
            ProcessApiService.ApiResponse<List<ProcessManager.ProcessRecord>> statusResponse = 
                apiService.getProcessesByStatus(status);
            
            if (statusResponse.isSuccess()) {
                logger.info("Processes with status '{}': {}", status, statusResponse.getData().size());
            }
        }
        
        // Get execution history for a process (if any exist)
        ProcessApiService.ApiResponse<List<ProcessManager.ProcessRecord>> allProcessesResponse = 
            apiService.getAllProcesses();
        
        if (allProcessesResponse.isSuccess() && !allProcessesResponse.getData().isEmpty()) {
            String firstProcessId = allProcessesResponse.getData().get(0).getId();
            
            ProcessApiService.ApiResponse<List<ProcessManager.ExecutionRecord>> historyResponse = 
                apiService.getExecutionHistory(firstProcessId);
            
            if (historyResponse.isSuccess()) {
                logger.info("Execution history for {}: {} executions", 
                          firstProcessId, historyResponse.getData().size());
                
                for (ProcessManager.ExecutionRecord execution : historyResponse.getData()) {
                    logger.info("  - {}: {} (triggered by {})", 
                              execution.getExecutionStartedAt(), 
                              execution.getExecutionStatus(), 
                              execution.getTriggeredBy());
                }
            }
        }
    }

    private static void monitorProcessBriefly(ProcessApiService apiService, String processId) {
        logger.info("Briefly monitoring process: {}", processId);
        
        try {
            Thread.sleep(3000); // Wait 3 seconds
            
            ProcessApiService.ApiResponse<ProcessController.ProcessStateResponse> stateResponse = 
                apiService.getProcessState(processId);
            
            if (stateResponse.isSuccess()) {
                ProcessManager.ProcessRecord process = stateResponse.getData().getProcessRecord();
                logger.info("Process status after 3 seconds: {}", process.getStatus());
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
