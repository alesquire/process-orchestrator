package com.processorchestrator.examples;

import com.processorchestrator.config.ProcessType;
import com.processorchestrator.config.ProcessTypeRegistry;
import com.processorchestrator.controller.ProcessController;
import com.processorchestrator.model.ProcessInputData;
import com.processorchestrator.model.TaskData;
import com.processorchestrator.service.ProcessManager;
import com.processorchestrator.service.ProcessOrchestrator;
import com.processorchestrator.service.ProcessResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Example demonstrating database-driven process management
 */
public class DatabaseDrivenProcessExample {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseDrivenProcessExample.class);

    public static void main(String[] args) {
        logger.info("Starting Database-Driven Process Example");
        
        try {
            // Create data source
            DataSource dataSource = createDataSource();
            
            // Initialize database schema
            initializeDatabase(dataSource);
            
            // Create services
            ProcessTypeRegistry registry = createProcessTypeRegistry();
            ProcessOrchestrator orchestrator = new ProcessOrchestrator(dataSource, registry);
            ProcessResultService resultService = new ProcessResultService(dataSource);
            ProcessManager processManager = new ProcessManager(dataSource);
            ProcessController controller = new ProcessController(processManager, orchestrator, resultService);
            
            // Start the orchestrator
            orchestrator.start();
            
            // Run examples
            runDatabaseDrivenExamples(controller, processManager);
            
            // Stop the orchestrator
            orchestrator.stop();
            
        } catch (Exception e) {
            logger.error("Error running database-driven process example", e);
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
        logger.info("Initializing database schema");
        
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Create process definitions table (user-managed)
            String createProcessDefinitionsTable = """
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
                """;
            
            statement.execute(createProcessDefinitionsTable);
            
            // Create processes table (engine-managed)
            String createProcessesTable = """
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
                """;
            
            statement.execute(createProcessesTable);
            
            // Create tasks table
            String createTasksTable = """
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
                """;
            
            statement.execute(createTasksTable);
            
            logger.info("Database schema initialized successfully");
            
        } catch (SQLException e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private static ProcessTypeRegistry createProcessTypeRegistry() {
        ProcessTypeRegistry registry = new ProcessTypeRegistry();
        
        // Register the data processing pipeline
        ProcessType dataProcessingPipeline = new ProcessType("data-processing-pipeline", "Complete data processing pipeline")
                .addTask("load", "python scripts/load_data.py ${input_file} ${output_dir}/loaded_data.json", "/data", 30, 2)
                .addTask("process", "python scripts/process_data.py ${output_dir}/loaded_data.json ${output_dir}/processed_data.json", "/data", 60, 3)
                .addTask("generate", "python scripts/generate_report.py ${output_dir}/processed_data.json ${output_dir}/report.html", "/data", 45, 2)
                .addTask("analyze", "python scripts/analyze_results.py ${output_dir}/report.html ${output_dir}/analysis.json", "/data", 30, 2);
        
        registry.register(dataProcessingPipeline);
        
        return registry;
    }

    private static void runDatabaseDrivenExamples(ProcessController controller, ProcessManager processManager) {
        logger.info("=== Database-Driven Process Examples ===");
        
        // Example 1: Create a manual process
        logger.info("\n--- Example 1: Manual Process ---");
        createAndRunManualProcess(controller);
        
        // Example 2: Create a scheduled process
        logger.info("\n--- Example 2: Scheduled Process ---");
        createScheduledProcess(controller, processManager);
        
        // Example 3: Process management operations
        logger.info("\n--- Example 3: Process Management ---");
        demonstrateProcessManagement(controller, processManager);
        
        // Example 4: Restart completed process
        logger.info("\n--- Example 4: Restart Completed Process ---");
        restartCompletedProcess(controller);
    }

    private static void createAndRunManualProcess(ProcessController controller) {
        logger.info("Creating manual process definition...");
        
        // Create process definition with input data
        String definitionId = "manual-data-process-001";
        String processType = "data-processing-pipeline";
        String inputData = "input_file:/data/sample_input.json;output_dir:/data/output;user_id:user123;batch_size:100";
        String schedule = null; // Manual execution only
        
        // Create the process definition
        ProcessManager.ProcessDefinition definition = controller.createProcessDefinition(definitionId, processType, inputData, schedule);
        logger.info("Created process definition: {}", definition.getId());
        
        // Start the process
        ProcessController.ProcessStartResponse startResponse = controller.startProcess(definitionId);
        logger.info("Start response: success={}, message={}", startResponse.isSuccess(), startResponse.getMessage());
        
        // Monitor the process
        monitorProcess(controller, definitionId);
        
        // Get final state
        ProcessController.ProcessStateResponse stateResponse = controller.getProcessState(definitionId);
        logger.info("Final process state: {}", stateResponse.getDefinition().getCurrentStatus());
    }

    private static void createScheduledProcess(ProcessController controller, ProcessManager processManager) {
        logger.info("Creating scheduled process definition...");
        
        // Create process definition with cron schedule
        String definitionId = "scheduled-backup-process-001";
        String processType = "data-processing-pipeline";
        String inputData = "input_file:/backup/source.json;output_dir:/backup/output;user_id:backup-service";
        String schedule = "0 2 * * *"; // Daily at 2 AM
        
        // Create the process definition
        ProcessManager.ProcessDefinition definition = controller.createProcessDefinition(definitionId, processType, inputData, schedule);
        logger.info("Created scheduled process definition: {} with schedule: {}", definition.getId(), definition.getSchedule());
        
        // List scheduled process definitions
        List<ProcessManager.ProcessDefinition> scheduledDefinitions = processManager.getScheduledProcessDefinitions();
        logger.info("Total scheduled process definitions: {}", scheduledDefinitions.size());
    }

    private static void demonstrateProcessManagement(ProcessController controller, ProcessManager processManager) {
        logger.info("Demonstrating process management operations...");
        
        // Get all process definitions
        List<ProcessManager.ProcessDefinition> allDefinitions = controller.getAllProcessDefinitions();
        logger.info("Total process definitions: {}", allDefinitions.size());
        
        // Get process definitions by status
        List<ProcessManager.ProcessDefinition> pendingDefinitions = controller.getProcessDefinitionsByStatus("PENDING");
        logger.info("Pending process definitions: {}", pendingDefinitions.size());
        
        // Get process definitions by status
        List<ProcessManager.ProcessDefinition> completedDefinitions = controller.getProcessDefinitionsByStatus("COMPLETED");
        logger.info("Completed process definitions: {}", completedDefinitions.size());
        
        // Show process definition details
        for (ProcessManager.ProcessDefinition definition : allDefinitions) {
            logger.info("Process Definition: id={}, type={}, status={}, schedule={}", 
                      definition.getId(), definition.getType(), definition.getCurrentStatus(), definition.getSchedule());
        }
    }

    private static void restartCompletedProcess(ProcessController controller) {
        logger.info("Demonstrating restart of completed process definition...");
        
        // First, create and complete a process definition
        String definitionId = "restart-demo-process-001";
        String processType = "data-processing-pipeline";
        String inputData = "input_file:/data/demo_input.json;output_dir:/data/demo_output;user_id:demo-user";
        
        // Create the process definition
        ProcessManager.ProcessDefinition definition = controller.createProcessDefinition(definitionId, processType, inputData, null);
        logger.info("Created process definition for restart demo: {}", definition.getId());
        
        // Start the process
        ProcessController.ProcessStartResponse startResponse = controller.startProcess(definitionId);
        logger.info("First start: success={}", startResponse.isSuccess());
        
        // Wait a bit and check status
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Get process state
        ProcessController.ProcessStateResponse stateResponse = controller.getProcessState(definitionId);
        logger.info("Process status after start: {}", stateResponse.getDefinition().getCurrentStatus());
        
        // Try to restart (this should work even if completed)
        ProcessController.ProcessStartResponse restartResponse = controller.startProcess(definitionId);
        logger.info("Restart attempt: success={}, message={}", restartResponse.isSuccess(), restartResponse.getMessage());
    }

    private static void monitorProcess(ProcessController controller, String definitionId) {
        logger.info("Monitoring process definition: {}", definitionId);
        
        int maxWaitTime = 30; // 30 seconds max wait for demo
        int waitTime = 0;
        
        while (waitTime < maxWaitTime) {
            try {
                ProcessController.ProcessStateResponse stateResponse = controller.getProcessState(definitionId);
                ProcessManager.ProcessDefinition definition = stateResponse.getDefinition();
                
                logger.info("Process Status: {} (Task {}/{})", 
                          definition.getCurrentStatus(), 
                          definition.getCurrentProcessId() != null ? "Active" : "None", 
                          "N/A");
                
                if ("COMPLETED".equals(definition.getCurrentStatus()) || 
                    "FAILED".equals(definition.getCurrentStatus()) ||
                    "STOPPED".equals(definition.getCurrentStatus())) {
                    break;
                }
                
                Thread.sleep(2000); // Wait 2 seconds
                waitTime += 2;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (waitTime >= maxWaitTime) {
            logger.warn("Process monitoring timed out after {} seconds", maxWaitTime);
        }
    }
}
