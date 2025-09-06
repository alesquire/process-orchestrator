package com.processorchestrator.examples;

import com.processorchestrator.model.ProcessInputData;
import com.processorchestrator.model.ProcessData;
import com.processorchestrator.model.TaskData;
import com.processorchestrator.model.ProcessStatus;
import com.processorchestrator.model.TaskStatus;
import com.processorchestrator.service.ProcessOrchestrator;
import com.processorchestrator.config.ProcessTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating the complete data processing pipeline
 * with sequential task execution and result persistence
 */
public class DataProcessingPipelineExample {
    private static final Logger logger = LoggerFactory.getLogger(DataProcessingPipelineExample.class);

    public static void main(String[] args) {
        logger.info("Starting Data Processing Pipeline Example");
        
        try {
            // Setup
            DataSource dataSource = createDataSource();
            ProcessTypeRegistry registry = new ProcessTypeRegistry();
            ProcessOrchestrator orchestrator = new ProcessOrchestrator(dataSource, registry);
            
            // Start the orchestrator
            orchestrator.start();
            
            // Create sample input data
            ProcessInputData inputData = createSampleInputData();
            
            // Start the data processing pipeline
            String processId = orchestrator.startProcess("data-processing-pipeline", inputData);
            logger.info("Started data processing pipeline with ID: {}", processId);
            
            // Monitor the process
            monitorProcess(orchestrator, processId);
            
            // Display results
            displayResults(orchestrator, processId);
            
            // Stop the orchestrator
            orchestrator.stop();
            
        } catch (Exception e) {
            logger.error("Error running data processing pipeline example", e);
        }
    }

    private static DataSource createDataSource() {
        // Create H2 in-memory database for this example
        return new javax.sql.DataSource() {
            @Override
            public java.sql.Connection getConnection() throws java.sql.SQLException {
                return java.sql.DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
            }

            @Override
            public java.sql.Connection getConnection(String username, String password) throws java.sql.SQLException {
                return java.sql.DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", username, password);
            }

            @Override
            public java.io.PrintWriter getLogWriter() throws java.sql.SQLException { return null; }
            @Override
            public void setLogWriter(java.io.PrintWriter out) throws java.sql.SQLException {}
            @Override
            public void setLoginTimeout(int seconds) throws java.sql.SQLException {}
            @Override
            public int getLoginTimeout() throws java.sql.SQLException { return 0; }
            @Override
            public java.util.logging.Logger getParentLogger() { return null; }
            @Override
            public <T> T unwrap(Class<T> iface) throws java.sql.SQLException { return null; }
            @Override
            public boolean isWrapperFor(Class<?> iface) throws java.sql.SQLException { return false; }
        };
    }

    private static ProcessInputData createSampleInputData() {
        ProcessInputData inputData = new ProcessInputData();
        inputData.setInputFile("/data/sample_input.json");
        inputData.setOutputDir("/data/output");
        inputData.setUserId("user123");
        
        // Add configuration
        inputData.addConfig("batch_size", "100");
        inputData.addConfig("parallel_processing", "false");
        inputData.addConfig("quality_threshold", "95.0");
        
        // Add metadata
        inputData.addMetadata("source_system", "legacy_database");
        inputData.addMetadata("data_type", "customer_records");
        inputData.addMetadata("priority", "high");
        
        logger.info("Created sample input data: {}", inputData);
        return inputData;
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

    private static void displayResults(ProcessOrchestrator orchestrator, String processId) {
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
