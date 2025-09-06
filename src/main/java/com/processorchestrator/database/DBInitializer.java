package com.processorchestrator.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Centralized database initialization class.
 * Handles creation of all database tables and schema for the Process Orchestrator.
 */
public class DBInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DBInitializer.class);

    private final DataSource dataSource;

    public DBInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Initialize the complete database schema.
     * This method creates all required tables for the Process Orchestrator.
     */
    public void initializeDatabase() {
        logger.info("Starting database schema initialization");
        
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Load and execute the main schema file
            String schemaSql = loadSchemaFromResource();
            executeSchemaScript(statement, schemaSql);
            
            logger.info("Database schema initialization completed successfully");
            
        } catch (SQLException | IOException e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Initialize only the essential tables for testing (including db-scheduler tables).
     * This is useful for unit tests that need the full functionality.
     */
    public void initializeEssentialTables() {
        logger.info("Starting essential tables initialization");
        
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Create the core process orchestrator tables
            createProcessRecordTable(statement);
            createProcessesTable(statement);
            createTasksTable(statement);
            createProcessExecutionsTable(statement);
            
            // Create scheduled_tasks table for db-scheduler (required for ProcessOrchestrator)
            createScheduledTasksTable(statement);
            
            logger.info("Essential tables initialization completed successfully");
            
        } catch (SQLException e) {
            logger.error("Failed to initialize essential tables", e);
            throw new RuntimeException("Essential tables initialization failed", e);
        }
    }

    /**
     * Load the schema SQL from the resources/schema.sql file
     */
    private String loadSchemaFromResource() throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    getClass().getClassLoader().getResourceAsStream("schema.sql"),
                    StandardCharsets.UTF_8))) {
            
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Execute the schema script, splitting by semicolons and executing each statement
     */
    private void executeSchemaScript(Statement statement, String schemaSql) throws SQLException {
        // Split by semicolon and execute each statement
        String[] statements = schemaSql.split(";");
        
        for (String sql : statements) {
            sql = sql.trim();
            if (!sql.isEmpty() && !sql.startsWith("--")) {
                try {
                    statement.execute(sql);
                    logger.debug("Executed SQL: {}", sql.substring(0, Math.min(sql.length(), 100)) + "...");
                } catch (SQLException e) {
                    logger.warn("Failed to execute SQL statement: {}", sql.substring(0, Math.min(sql.length(), 100)) + "...", e);
                    // Continue with other statements
                }
            }
        }
    }

    /**
     * Create the process_record table
     */
    private void createProcessRecordTable(Statement statement) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS process_record (
                id VARCHAR(255) PRIMARY KEY,
                type VARCHAR(255) NOT NULL,
                input_data TEXT NOT NULL,
                schedule VARCHAR(255),
                current_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                current_process_id VARCHAR(255),
                started_when TIMESTAMP WITH TIME ZONE,
                completed_when TIMESTAMP WITH TIME ZONE,
                failed_when TIMESTAMP WITH TIME ZONE,
                stopped_when TIMESTAMP WITH TIME ZONE,
                last_error_message TEXT,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            )
            """;
        statement.execute(sql);
        
        // Create indexes
        statement.execute("CREATE INDEX IF NOT EXISTS idx_process_record_status ON process_record (current_status)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_process_record_type ON process_record (type)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_process_record_schedule ON process_record (schedule)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_process_record_created_at ON process_record (created_at)");
    }

    /**
     * Create the processes table
     */
    private void createProcessesTable(Statement statement) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS processes (
                id VARCHAR(255) PRIMARY KEY,
                process_record_id VARCHAR(255) NOT NULL,
                type VARCHAR(255) NOT NULL,
                input_data TEXT NOT NULL,
                status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                current_task_index INTEGER NOT NULL DEFAULT 0,
                total_tasks INTEGER NOT NULL,
                started_at TIMESTAMP WITH TIME ZONE,
                completed_at TIMESTAMP WITH TIME ZONE,
                error_message TEXT,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (process_record_id) REFERENCES process_record(id) ON DELETE CASCADE
            )
            """;
        statement.execute(sql);
        
        // Create indexes
        statement.execute("CREATE INDEX IF NOT EXISTS idx_processes_status ON processes (status)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_processes_type ON processes (type)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_processes_process_record_id ON processes (process_record_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_processes_created_at ON processes (created_at)");
    }

    /**
     * Create the tasks table
     */
    private void createTasksTable(Statement statement) throws SQLException {
        String sql = """
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
                started_at TIMESTAMP WITH TIME ZONE,
                completed_at TIMESTAMP WITH TIME ZONE,
                error_message TEXT,
                exit_code INTEGER,
                output TEXT,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (process_id) REFERENCES processes(id) ON DELETE CASCADE
            )
            """;
        statement.execute(sql);
        
        // Create indexes
        statement.execute("CREATE INDEX IF NOT EXISTS idx_tasks_process_id ON tasks (process_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks (status)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_tasks_task_index ON tasks (task_index)");
    }

    /**
     * Create the process_executions table
     */
    private void createProcessExecutionsTable(Statement statement) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS process_executions (
                execution_id VARCHAR(255) PRIMARY KEY,
                process_id VARCHAR(255) NOT NULL,
                execution_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
                execution_completed_at TIMESTAMP WITH TIME ZONE,
                execution_status VARCHAR(50) NOT NULL,
                triggered_by VARCHAR(50) NOT NULL,
                error_message TEXT,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (process_id) REFERENCES processes(id) ON DELETE CASCADE
            )
            """;
        statement.execute(sql);
        
        // Create indexes
        statement.execute("CREATE INDEX IF NOT EXISTS idx_executions_process_id ON process_executions (process_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_executions_status ON process_executions (execution_status)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_executions_started_at ON process_executions (execution_started_at)");
    }

    /**
     * Create the scheduled_tasks table for db-scheduler
     */
    private void createScheduledTasksTable(Statement statement) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS scheduled_tasks (
                task_name VARCHAR(255) NOT NULL,
                task_instance VARCHAR(255) NOT NULL,
                task_data BYTEA,
                execution_time TIMESTAMP WITH TIME ZONE NOT NULL,
                picked BOOLEAN NOT NULL DEFAULT FALSE,
                picked_by VARCHAR(50),
                last_success TIMESTAMP WITH TIME ZONE,
                last_failure TIMESTAMP WITH TIME ZONE,
                consecutive_failures INT,
                last_heartbeat TIMESTAMP WITH TIME ZONE,
                version BIGINT NOT NULL DEFAULT 0,
                PRIMARY KEY (task_name, task_instance)
            )
            """;
        statement.execute(sql);
        
        // Create indexes for db-scheduler performance
        statement.execute("CREATE INDEX IF NOT EXISTS execution_time_idx ON scheduled_tasks (execution_time)");
        statement.execute("CREATE INDEX IF NOT EXISTS last_heartbeat_idx ON scheduled_tasks (last_heartbeat)");
    }

    /**
     * Check if the database is properly initialized by verifying key tables exist
     */
    public boolean isDatabaseInitialized() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Check if key tables exist
            var resultSet = statement.executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN ('process_record', 'processes', 'tasks', 'scheduled_tasks')"
            );
            
            if (resultSet.next()) {
                int tableCount = resultSet.getInt(1);
                return tableCount >= 4; // At least 4 key tables should exist
            }
            
            return false;
            
        } catch (SQLException e) {
            logger.warn("Failed to check database initialization status", e);
            return false;
        }
    }

    /**
     * Clean up test data (useful for test isolation)
     */
    public void cleanupTestData() {
        logger.info("Cleaning up test data");
        
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Delete test records (those starting with test-)
            statement.executeUpdate("DELETE FROM process_record WHERE id LIKE 'test-%'");
            statement.executeUpdate("DELETE FROM process_record WHERE id LIKE 'all-%'");
            statement.executeUpdate("DELETE FROM process_record WHERE id LIKE 'status-test-%'");
            statement.executeUpdate("DELETE FROM process_record WHERE id LIKE 'db-test-%'");
            statement.executeUpdate("DELETE FROM process_record WHERE id LIKE 'crud-test-%'");
            
            logger.info("Test data cleanup completed");
            
        } catch (SQLException e) {
            logger.warn("Failed to clean up test data", e);
        }
    }
}
