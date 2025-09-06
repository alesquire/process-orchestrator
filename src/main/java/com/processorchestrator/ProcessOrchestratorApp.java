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
}