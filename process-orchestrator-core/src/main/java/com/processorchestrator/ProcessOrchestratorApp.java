package com.processorchestrator;

import com.processorchestrator.config.ProcessType;
import com.processorchestrator.config.ProcessTypeRegistry;
import com.processorchestrator.model.ProcessInputData;
import com.processorchestrator.service.ProcessOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Main application class for Process Orchestrator
 */
public class ProcessOrchestratorApp {
    private static final Logger logger = LoggerFactory.getLogger(ProcessOrchestratorApp.class);
    
    private final DataSource dataSource;
    private final ProcessTypeRegistry processTypeRegistry;
    private final ProcessOrchestrator processOrchestrator;

    public ProcessOrchestratorApp(DataSource dataSource) {
        this.dataSource = dataSource;
        this.processTypeRegistry = new ProcessTypeRegistry();
        this.processOrchestrator = new ProcessOrchestrator(dataSource, processTypeRegistry);
        
        // Register default process types
        registerDefaultProcessTypes();
    }

    private void registerDefaultProcessTypes() {
        // Data Processing Pipeline
        ProcessType dataProcessingPipeline = new ProcessType("data-processing-pipeline", "Data processing pipeline")
                .addTask("validate", "python scripts/validate.py ${input_file}", "/data", 30, 2)
                .addTask("transform", "python scripts/transform.py ${input_file} ${output_dir}", "/data", 60, 3)
                .addTask("load", "python scripts/load.py ${output_dir}", "/data", 45, 2);
        
        processTypeRegistry.register(dataProcessingPipeline);
        
        // Deployment Pipeline
        ProcessType deploymentPipeline = new ProcessType("deployment-pipeline", "Application deployment pipeline")
                .addTask("build", "mvn clean package", "/app", 15, 2)
                .addTask("test", "mvn test", "/app", 20, 3)
                .addTask("deploy", "kubectl apply -f deployment.yaml", "/app", 10, 2);
        
        processTypeRegistry.register(deploymentPipeline);
        
        // Backup Process
        ProcessType backupProcess = new ProcessType("backup-process", "Database backup process")
                .addTask("backup-db", "pg_dump -h localhost -U postgres mydb > ${output_dir}/backup.sql", "/backups", 30, 2)
                .addTask("compress", "gzip ${output_dir}/backup.sql", "/backups", 5, 1)
                .addTask("upload", "aws s3 cp ${output_dir}/backup.sql.gz s3://my-backups/", "/backups", 15, 3);
        
        processTypeRegistry.register(backupProcess);
        
        // ETL Pipeline
        ProcessType etlPipeline = new ProcessType("etl-pipeline", "Extract, Transform, Load pipeline")
                .addTask("extract", "python scripts/extract.py ${input_file}", "/etl", 45, 2)
                .addTask("transform", "python scripts/transform.py ${input_file} ${output_dir}", "/etl", 90, 3)
                .addTask("load", "python scripts/load.py ${output_dir}", "/etl", 30, 2)
                .addTask("notify", "python scripts/notify.py ${output_dir}", "/etl", 10, 1);
        
        processTypeRegistry.register(etlPipeline);
        
        // Complete Data Processing Pipeline - Load, Process, Generate, Analyze
        ProcessType completeDataProcessingPipeline = new ProcessType("data-processing-pipeline", "Complete data processing pipeline")
                .addTask("load", "python scripts/load_data.py ${input_file} ${output_dir}/loaded_data.json", "/data", 30, 2)
                .addTask("process", "python scripts/process_data.py ${output_dir}/loaded_data.json ${output_dir}/processed_data.json", "/data", 60, 3)
                .addTask("generate", "python scripts/generate_report.py ${output_dir}/processed_data.json ${output_dir}/report.html", "/data", 45, 2)
                .addTask("analyze", "python scripts/analyze_results.py ${output_dir}/report.html ${output_dir}/analysis.json", "/data", 30, 2);
        
        processTypeRegistry.register(completeDataProcessingPipeline);
        
        logger.info("Registered {} process types", processTypeRegistry.getAllProcessTypes().size());
    }

    public void start() {
        logger.info("Starting Process Orchestrator Application");
        
        // Initialize database schema
        initializeDatabase();
        
        // Start the orchestrator
        processOrchestrator.start();
        
        logger.info("Process Orchestrator Application started successfully");
    }

    public void stop() {
        logger.info("Stopping Process Orchestrator Application");
        processOrchestrator.stop();
        logger.info("Process Orchestrator Application stopped");
    }

    private void initializeDatabase() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Create processes table for tracking process state (optional - for monitoring)
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
            
            // Create tasks table for tracking individual task state (optional - for monitoring)
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
                    FOREIGN KEY (process_id) REFERENCES processes(process_id)
                )
                """;
            
            statement.execute(createTasksTable);
            
            logger.info("Database schema initialized successfully");
            
        } catch (SQLException e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public ProcessOrchestrator getProcessOrchestrator() {
        return processOrchestrator;
    }

    public ProcessTypeRegistry getProcessTypeRegistry() {
        return processTypeRegistry;
    }

    /**
     * Example method demonstrating how to start a data processing pipeline
     */
    public void runDataProcessingExample() {
        logger.info("Running Data Processing Pipeline Example");
        
        // Create sample input data
        ProcessInputData inputData = new ProcessInputData();
        inputData.setInputFile("/data/sample_input.json");
        inputData.setOutputDir("/data/output");
        inputData.setUserId("user123");
        inputData.addConfig("batch_size", "100");
        inputData.addConfig("parallel_processing", "false");
        inputData.addConfig("quality_threshold", "95.0");
        inputData.addMetadata("source_system", "legacy_database");
        inputData.addMetadata("data_type", "customer_records");
        inputData.addMetadata("priority", "high");
        
        // Start the data processing pipeline
        String processId = processOrchestrator.startProcess("data-processing-pipeline", inputData);
        logger.info("Started data processing pipeline with ID: {}", processId);
        
        // Monitor the process (in a real application, this would be done asynchronously)
        monitorProcess(processId);
        
        // Display results
        displayProcessResults(processId);
    }

    private void monitorProcess(String processId) {
        logger.info("Monitoring process: {}", processId);
        
        int maxWaitTime = 300; // 5 minutes max wait
        int waitTime = 0;
        
        while (waitTime < maxWaitTime) {
            try {
                var process = processOrchestrator.getProcess(processId);
                if (process != null) {
                    logger.info("Process Status: {} (Task {}/{})", 
                              process.getStatus(), 
                              process.getCurrentTaskIndex(), 
                              process.getTotalTasks());
                    
                    if ("COMPLETED".equals(process.getStatus()) || 
                        "FAILED".equals(process.getStatus())) {
                        break;
                    }
                }
                
                Thread.sleep(5000); // Wait 5 seconds
                waitTime += 5;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (waitTime >= maxWaitTime) {
            logger.warn("Process monitoring timed out after {} seconds", maxWaitTime);
        }
    }

    private void displayProcessResults(String processId) {
        logger.info("=== PROCESS RESULTS ===");
        
        // Get process data
        var process = processOrchestrator.getProcess(processId);
        if (process != null) {
            logger.info("Process ID: {}", process.getProcessId());
            logger.info("Process Type: {}", process.getProcessTypeName());
            logger.info("Status: {}", process.getStatus());
            logger.info("Started At: {}", process.getStartedAt());
            logger.info("Completed At: {}", process.getCompletedAt());
            logger.info("Error Message: {}", process.getErrorMessage());
        }
        
        // Get all tasks
        var tasks = processOrchestrator.getProcessTasks(processId);
        logger.info("\n=== TASK RESULTS ===");
        
        for (var task : tasks) {
            logger.info("Task: {} ({})", task.getName(), task.getStatus());
            logger.info("  Command: {}", task.getCommand());
            logger.info("  Started At: {}", task.getStartedAt());
            logger.info("  Completed At: {}", task.getCompletedAt());
            logger.info("  Exit Code: {}", task.getExitCode());
            logger.info("  Retry Count: {}/{}", task.getRetryCount(), task.getMaxRetries());
            
            if (task.getOutput() != null && !task.getOutput().isEmpty()) {
                logger.info("  Output: {}", task.getOutput());
            }
            
            if (task.getErrorMessage() != null && !task.getErrorMessage().isEmpty()) {
                logger.info("  Error: {}", task.getErrorMessage());
            }
            
            logger.info("");
        }
        
        // Display summary
        logger.info("=== SUMMARY ===");
        long completedTasks = tasks.stream()
                .mapToLong(t -> "COMPLETED".equals(t.getStatus()) ? 1 : 0)
                .sum();
        long failedTasks = tasks.stream()
                .mapToLong(t -> "FAILED".equals(t.getStatus()) ? 1 : 0)
                .sum();
        
        logger.info("Total Tasks: {}", tasks.size());
        logger.info("Completed: {}", completedTasks);
        logger.info("Failed: {}", failedTasks);
        logger.info("Success Rate: {}%", 
                  tasks.size() > 0 ? (completedTasks * 100 / tasks.size()) : 0);
    }
}