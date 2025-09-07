package com.processorchestrator;

import com.processorchestrator.config.ProcessTypeRegistry;
import com.processorchestrator.config.ProcessTypeInitializer;
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
        
        // Register default process types using the dedicated initializer
        ProcessTypeInitializer.registerDefaultProcessTypes(processTypeRegistry);
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