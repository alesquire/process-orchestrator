package com.processorchestrator;

import com.processorchestrator.config.DatabaseConfig;
import com.processorchestrator.model.ProcessInputData;
import com.processorchestrator.model.ProcessData;
import com.processorchestrator.model.TaskData;
import com.processorchestrator.model.ProcessStatus;
import com.processorchestrator.model.TaskStatus;
import com.processorchestrator.service.ProcessOrchestrator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * Main class with examples of how to use the Process Orchestrator
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        logger.info("Starting Process Orchestrator Examples");
        
        try {
            // Create data source
            DataSource dataSource = createDataSource();
            
            // Create application
            ProcessOrchestratorApp app = new ProcessOrchestratorApp(dataSource);
            
            // Start the application
            app.start();
            
            // Run examples
            runExamples(app.getProcessOrchestrator());
            
            // Keep running for a while to see processes execute
            logger.info("Waiting for processes to complete...");
            TimeUnit.MINUTES.sleep(5);
            
            // Stop the application
            app.stop();
            
        } catch (Exception e) {
            logger.error("Error running Process Orchestrator", e);
        }
    }

    private static DataSource createDataSource() {
        return new DataSource() {
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
            public java.io.PrintWriter getLogWriter() throws SQLException {
                return null;
            }

            @Override
            public void setLogWriter(java.io.PrintWriter out) throws SQLException {
            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {
            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return 0;
            }

            @Override
            public java.util.logging.Logger getParentLogger() {
                return null;
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                return null;
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return false;
            }
        };
    }

    private static void runExamples(ProcessOrchestrator orchestrator) {
        logger.info("Running Process Orchestrator Examples");
        
        // Example 1: Data Processing Pipeline
        runDataProcessingPipelineExample(orchestrator);
        
        // Example 2: Deployment Pipeline
        runDeploymentPipelineExample(orchestrator);
        
        // Example 3: Backup Process
        runBackupProcessExample(orchestrator);
        
        // Example 4: ETL Pipeline
        runETLPipelineExample(orchestrator);
    }

    private static void runDataProcessingPipelineExample(ProcessOrchestrator orchestrator) {
        logger.info("=== Data Processing Pipeline Example ===");
        
        // Create input data for the complete pipeline (load, process, generate, analyze)
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
        
        // Start process
        String processId = orchestrator.startProcess("data-processing-pipeline", inputData);
        logger.info("Started data processing pipeline with ID: {}", processId);
        
        // Monitor the process
        monitorProcess(orchestrator, processId);
        
        // Display results
        displayProcessResults(orchestrator, processId);
    }

    private static void runDeploymentPipelineExample(ProcessOrchestrator orchestrator) {
        logger.info("=== Deployment Pipeline Example ===");
        
        // Create input data
        ProcessInputData inputData = new ProcessInputData();
        inputData.setInputFile("/app");
        inputData.setOutputDir("/app");
        inputData.addConfig("environment", "production");
        inputData.addConfig("version", "1.2.3");
        inputData.setUserId("deployer");
        inputData.addMetadata("deployment_type", "rolling");
        
        // Start process
        String processId = orchestrator.startProcess("deployment-pipeline", inputData);
        logger.info("Started deployment pipeline with ID: {}", processId);
    }

    private static void runBackupProcessExample(ProcessOrchestrator orchestrator) {
        logger.info("=== Backup Process Example ===");
        
        // Create input data
        ProcessInputData inputData = new ProcessInputData();
        inputData.setInputFile("/backups");
        inputData.setOutputDir("/backups");
        inputData.addConfig("database", "mydb");
        inputData.addConfig("retention_days", "30");
        inputData.setUserId("backup-service");
        inputData.addMetadata("backup_type", "full");
        
        // Start process
        String processId = orchestrator.startProcess("backup-process", inputData);
        logger.info("Started backup process with ID: {}", processId);
    }

    private static void runETLPipelineExample(ProcessOrchestrator orchestrator) {
        logger.info("=== ETL Pipeline Example ===");
        
        // Create input data
        ProcessInputData inputData = new ProcessInputData();
        inputData.setInputFile("/etl/source/data.json");
        inputData.setOutputDir("/etl/output");
        inputData.addConfig("batch_size", "5000");
        inputData.addConfig("parallel_workers", "4");
        inputData.setUserId("etl-service");
        inputData.addMetadata("data_source", "api");
        inputData.addMetadata("quality_check", "enabled");
        
        // Start process
        String processId = orchestrator.startProcess("etl-pipeline", inputData);
        logger.info("Started ETL pipeline with ID: {}", processId);
    }

    private static void monitorProcess(ProcessOrchestrator orchestrator, String processId) {
        logger.info("Monitoring process: {}", processId);
        
        int maxWaitTime = 300; // 5 minutes max wait
        int waitTime = 0;
        
        while (waitTime < maxWaitTime) {
            try {
                ProcessData process = orchestrator.getProcess(processId);
                if (process != null) {
                    logger.info("Process Status: {} (Task {}/{})", 
                              process.getStatus(), 
                              process.getCurrentTaskIndex(), 
                              process.getTotalTasks());
                    
                    if (process.getStatus() == ProcessStatus.COMPLETED || 
                        process.getStatus() == ProcessStatus.FAILED) {
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

    private static void displayProcessResults(ProcessOrchestrator orchestrator, String processId) {
        logger.info("=== PROCESS RESULTS ===");
        
        // Get process data
        ProcessData process = orchestrator.getProcess(processId);
        if (process != null) {
            logger.info("Process ID: {}", process.getProcessId());
            logger.info("Process Type: {}", process.getProcessType());
            logger.info("Status: {}", process.getStatus());
            logger.info("Started At: {}", process.getStartedAt());
            logger.info("Completed At: {}", process.getCompletedAt());
            logger.info("Error Message: {}", process.getErrorMessage());
        }
        
        // Get all tasks
        List<TaskData> tasks = orchestrator.getProcessTasks(processId);
        logger.info("\n=== TASK RESULTS ===");
        
        for (TaskData task : tasks) {
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
                .mapToLong(t -> t.getStatus() == TaskStatus.COMPLETED ? 1 : 0)
                .sum();
        long failedTasks = tasks.stream()
                .mapToLong(t -> t.getStatus() == TaskStatus.FAILED ? 1 : 0)
                .sum();
        
        logger.info("Total Tasks: {}", tasks.size());
        logger.info("Completed: {}", completedTasks);
        logger.info("Failed: {}", failedTasks);
        logger.info("Success Rate: {}%", 
                  tasks.size() > 0 ? (completedTasks * 100 / tasks.size()) : 0);
    }
}