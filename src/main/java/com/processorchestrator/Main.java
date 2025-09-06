package com.processorchestrator;

import com.processorchestrator.model.ProcessInputData;
import com.processorchestrator.service.ProcessOrchestrator;
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
                    "jdbc:postgresql://localhost:5432/process_orchestrator",
                    "postgres",
                    "password"
                );
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/process_orchestrator",
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
        
        // Create input data
        ProcessInputData inputData = new ProcessInputData("/data/input.csv", "/data/output");
        inputData.addConfig("batch_size", "1000");
        inputData.addConfig("format", "json");
        inputData.setUserId("user123");
        inputData.addMetadata("priority", "high");
        
        // Start process
        String processId = orchestrator.startProcess("data-processing-pipeline", inputData);
        logger.info("Started data processing pipeline with ID: {}", processId);
    }

    private static void runDeploymentPipelineExample(ProcessOrchestrator orchestrator) {
        logger.info("=== Deployment Pipeline Example ===");
        
        // Create input data
        ProcessInputData inputData = new ProcessInputData("/app", "/app");
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
        ProcessInputData inputData = new ProcessInputData("/backups", "/backups");
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
        ProcessInputData inputData = new ProcessInputData("/etl/source/data.json", "/etl/output");
        inputData.addConfig("batch_size", "5000");
        inputData.addConfig("parallel_workers", "4");
        inputData.setUserId("etl-service");
        inputData.addMetadata("data_source", "api");
        inputData.addMetadata("quality_check", "enabled");
        
        // Start process
        String processId = orchestrator.startProcess("etl-pipeline", inputData);
        logger.info("Started ETL pipeline with ID: {}", processId);
    }
}