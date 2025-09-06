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
                CREATE TABLE IF NOT EXISTS process_definitions (
                    id VARCHAR(255) PRIMARY KEY,
                    type VARCHAR(255) NOT NULL,
                    input_data TEXT NOT NULL,
                    schedule VARCHAR(255),
                    current_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                    current_process_id VARCHAR(255),
                    started_when TIMESTAMP,
                    completed_when TIMESTAMP,
                    failed_when TIMESTAMP,
                    stopped_when TIMESTAMP,
                    last_error_message TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS processes (
                    id VARCHAR(255) PRIMARY KEY,
                    definition_id VARCHAR(255) NOT NULL,
                    type VARCHAR(255) NOT NULL,
                    input_data TEXT NOT NULL,
                    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                    current_task_index INTEGER NOT NULL DEFAULT 0,
                    total_tasks INTEGER NOT NULL,
                    started_at TIMESTAMP,
                    completed_at TIMESTAMP,
                    error_message TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (definition_id) REFERENCES process_definitions(id) ON DELETE CASCADE
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
        
        // Create multiple process definitions
        String[] definitionIds = {
            "daily-report-process",
            "weekly-backup-process", 
            "monthly-cleanup-process"
        };
        
        String[] schedules = {
            "0 6 * * *",    // Daily at 6 AM
            "0 2 * * 0",    // Weekly on Sunday at 2 AM
            "0 1 1 * *"     // Monthly on 1st at 1 AM
        };
        
        for (int i = 0; i < definitionIds.length; i++) {
            String definitionId = definitionIds[i];
            String schedule = schedules[i];
            String inputData = String.format("input_file:/data/%s.json;output_dir:/data/%s_output;user_id:system", 
                                            definitionId, definitionId);
            
            // Create process definition via API
            ProcessApiService.ApiResponse<ProcessManager.ProcessDefinition> createResponse = 
                apiService.createProcessDefinition(definitionId, "data-processing-pipeline", inputData, schedule);
            
            if (createResponse.isSuccess()) {
                logger.info("Created process definition: {} with schedule: {}", definitionId, schedule);
            } else {
                logger.error("Failed to create process definition: {} - {}", definitionId, createResponse.getMessage());
            }
        }
        
        // List all process definitions
        ProcessApiService.ApiResponse<List<ProcessManager.ProcessDefinition>> allDefinitionsResponse = 
            apiService.getAllProcessDefinitions();
        
        if (allDefinitionsResponse.isSuccess()) {
            logger.info("Total process definitions created: {}", allDefinitionsResponse.getData().size());
            for (ProcessManager.ProcessDefinition definition : allDefinitionsResponse.getData()) {
                logger.info("  - {}: {} (schedule: {})", definition.getId(), definition.getCurrentStatus(), definition.getSchedule());
            }
        }
    }

    private static void demonstrateScheduledProcesses(ProcessApiService apiService) {
        logger.info("\n=== Example 2: Scheduled Processes ===");
        
        // Get scheduled process definitions
        ProcessApiService.ApiResponse<List<ProcessManager.ProcessDefinition>> scheduledResponse = 
            apiService.getScheduledProcessDefinitions();
        
        if (scheduledResponse.isSuccess()) {
            logger.info("Scheduled process definitions: {}", scheduledResponse.getData().size());
            for (ProcessManager.ProcessDefinition definition : scheduledResponse.getData()) {
                logger.info("  - {}: schedule={}", definition.getId(), definition.getSchedule());
            }
        }
        
        // Create a manual-only process definition
        ProcessApiService.ApiResponse<ProcessManager.ProcessDefinition> manualResponse = 
            apiService.createProcessDefinition("manual-test-process", "data-processing-pipeline", 
                                             "input_file:/data/manual.json;output_dir:/data/manual_output", null);
        
        if (manualResponse.isSuccess()) {
            logger.info("Created manual process definition: {}", manualResponse.getData().getId());
        }
    }

    private static void demonstrateProcessLifecycle(ProcessApiService apiService, ProcessController controller) {
        logger.info("\n=== Example 3: Process Lifecycle ===");
        
        String definitionId = "lifecycle-demo-process";
        
        // Create process definition
        ProcessApiService.ApiResponse<ProcessManager.ProcessDefinition> createResponse = 
            apiService.createProcessDefinition(definitionId, "data-processing-pipeline", 
                                             "input_file:/data/lifecycle.json;output_dir:/data/lifecycle_output", null);
        
        if (!createResponse.isSuccess()) {
            logger.error("Failed to create process definition: {}", createResponse.getMessage());
            return;
        }
        
        // Start process
        ProcessApiService.ApiResponse<ProcessController.ProcessStartResponse> startResponse = 
            apiService.startProcess(definitionId);
        
        if (startResponse.isSuccess()) {
            logger.info("Process started: {}", startResponse.getData().getMessage());
            
            // Monitor for a short time
            monitorProcessBriefly(apiService, definitionId);
            
            // Get process state
            ProcessApiService.ApiResponse<ProcessController.ProcessStateResponse> stateResponse = 
                apiService.getProcessState(definitionId);
            
            if (stateResponse.isSuccess()) {
                ProcessManager.ProcessDefinition definition = stateResponse.getData().getDefinition();
                logger.info("Process state: {} (active process: {})", 
                          definition.getCurrentStatus(), definition.getCurrentProcessId());
            }
        } else {
            logger.error("Failed to start process: {}", startResponse.getMessage());
        }
    }

    private static void demonstrateErrorHandling(ProcessApiService apiService) {
        logger.info("\n=== Example 4: Error Handling ===");
        
        // Try to start non-existent process definition
        ProcessApiService.ApiResponse<ProcessController.ProcessStartResponse> errorResponse = 
            apiService.startProcess("non-existent-process");
        
        if (!errorResponse.isSuccess()) {
            logger.info("Expected error when starting non-existent process: {}", errorResponse.getMessage());
        }
        
        // Try to create duplicate process definition
        String duplicateId = "duplicate-process";
        apiService.createProcessDefinition(duplicateId, "data-processing-pipeline", "input_data:test", null);
        
        ProcessApiService.ApiResponse<ProcessManager.ProcessDefinition> duplicateResponse = 
            apiService.createProcessDefinition(duplicateId, "data-processing-pipeline", "input_data:test", null);
        
        if (!duplicateResponse.isSuccess()) {
            logger.info("Expected error when creating duplicate process definition: {}", duplicateResponse.getMessage());
        }
    }

    private static void demonstrateMonitoring(ProcessApiService apiService, ProcessManager processManager) {
        logger.info("\n=== Example 5: Monitoring and Reporting ===");
        
        // Get process definitions by status
        String[] statuses = {"PENDING", "IN_PROGRESS", "COMPLETED", "FAILED"};
        
        for (String status : statuses) {
            ProcessApiService.ApiResponse<List<ProcessManager.ProcessDefinition>> statusResponse = 
                apiService.getProcessDefinitionsByStatus(status);
            
            if (statusResponse.isSuccess()) {
                logger.info("Process definitions with status '{}': {}", status, statusResponse.getData().size());
            }
        }
        
        // Get all process definitions
        ProcessApiService.ApiResponse<List<ProcessManager.ProcessDefinition>> allDefinitionsResponse = 
            apiService.getAllProcessDefinitions();
        
        if (allDefinitionsResponse.isSuccess() && !allDefinitionsResponse.getData().isEmpty()) {
            String firstDefinitionId = allDefinitionsResponse.getData().get(0).getId();
            
            // Get process state for first definition
            ProcessApiService.ApiResponse<ProcessController.ProcessStateResponse> stateResponse = 
                apiService.getProcessState(firstDefinitionId);
            
            if (stateResponse.isSuccess()) {
                ProcessManager.ProcessDefinition definition = stateResponse.getData().getDefinition();
                logger.info("Process definition {} status: {} (active process: {})", 
                          firstDefinitionId, definition.getCurrentStatus(), definition.getCurrentProcessId());
                
                if (definition.getStartedWhen() != null) {
                    logger.info("  Started: {}", definition.getStartedWhen());
                }
                if (definition.getCompletedWhen() != null) {
                    logger.info("  Completed: {}", definition.getCompletedWhen());
                }
                if (definition.getFailedWhen() != null) {
                    logger.info("  Failed: {}", definition.getFailedWhen());
                }
            }
        }
    }

    private static void monitorProcessBriefly(ProcessApiService apiService, String definitionId) {
        logger.info("Briefly monitoring process definition: {}", definitionId);
        
        try {
            Thread.sleep(3000); // Wait 3 seconds
            
            ProcessApiService.ApiResponse<ProcessController.ProcessStateResponse> stateResponse = 
                apiService.getProcessState(definitionId);
            
            if (stateResponse.isSuccess()) {
                ProcessManager.ProcessDefinition definition = stateResponse.getData().getDefinition();
                logger.info("Process definition status after 3 seconds: {}", definition.getCurrentStatus());
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
