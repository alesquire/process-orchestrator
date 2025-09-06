package com.processorchestrator.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Centralized database initialization class.
 * Handles creation of all database tables and schema for the Process Orchestrator.
 * All database schema is defined in SQL files and executed by this class.
 */
public class DBInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DBInitializer.class);
    private final DataSource dataSource;

    public DBInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Initialize the complete database schema by executing the SQL script from resources/schema.sql.
     */
    public void initializeDatabase() {
        logger.info("Starting database schema initialization from schema.sql");
        
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Load and execute the main schema file
            String schemaSql = loadSchemaFromResource();
            executeSchemaScript(statement, schemaSql);
            
            logger.info("Database schema initialization completed successfully from schema.sql");
            
        } catch (SQLException | IOException e) {
            logger.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Load the schema SQL from the resources/schema.sql file
     */
    private String loadSchemaFromResource() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Execute the schema script, handling PostgreSQL functions and triggers properly.
     * This method parses SQL statements more intelligently to handle dollar-quoted functions.
     */
    private void executeSchemaScript(Statement statement, String schemaSql) throws SQLException {
        // Parse SQL statements more intelligently to handle PostgreSQL functions
        String[] statements = parseSqlStatements(schemaSql);

        for (String sql : statements) {
            sql = sql.trim();
            if (!sql.isEmpty() && !sql.startsWith("--")) { // Ignore empty lines and comments
                try {
                    statement.execute(sql);
                    logger.debug("Executed SQL: {}", sql.substring(0, Math.min(sql.length(), 100)) + "...");
                } catch (SQLException e) {
                    logger.warn("Failed to execute SQL statement: {}", sql.substring(0, Math.min(sql.length(), 100)) + "...", e);
                    // Continue with other statements, but log the warning
                }
            }
        }
    }

    /**
     * Parse SQL statements intelligently to handle PostgreSQL dollar-quoted functions.
     * This method properly handles semicolons within function bodies.
     */
    private String[] parseSqlStatements(String sql) {
        java.util.List<String> statements = new java.util.ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        boolean inDollarQuote = false;
        String dollarTag = null;
        
        String[] lines = sql.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("--")) {
                continue; // Skip empty lines and comments
            }
            
            currentStatement.append(line).append("\n");
            
            // Check for dollar quote start/end
            if (!inDollarQuote) {
                // Look for dollar quote start: $$ or $tag$
                java.util.regex.Pattern dollarStart = java.util.regex.Pattern.compile("\\$([^$]*)\\$");
                java.util.regex.Matcher matcher = dollarStart.matcher(line);
                if (matcher.find()) {
                    inDollarQuote = true;
                    dollarTag = matcher.group(1);
                }
            } else {
                // Look for dollar quote end
                if (line.contains("$" + dollarTag + "$")) {
                    inDollarQuote = false;
                    dollarTag = null;
                }
            }
            
            // If not in dollar quote and line ends with semicolon, it's a complete statement
            if (!inDollarQuote && line.endsWith(";")) {
                String statement = currentStatement.toString().trim();
                if (!statement.isEmpty()) {
                    statements.add(statement);
                }
                currentStatement = new StringBuilder();
            }
        }
        
        // Add any remaining statement
        String finalStatement = currentStatement.toString().trim();
        if (!finalStatement.isEmpty()) {
            statements.add(finalStatement);
        }
        
        return statements.toArray(new String[0]);
    }

    /**
     * Check if the database is properly initialized by verifying key tables exist
     */
    public boolean isDatabaseInitialized() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Check if key tables exist (simplified schema - 3 tables)
            var resultSet = statement.executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN ('process_record', 'tasks', 'scheduled_tasks')"
            );

            if (resultSet.next()) {
                int tableCount = resultSet.getInt(1);
                return tableCount >= 3; // At least 3 key tables should exist
            }

            return false;

        } catch (SQLException e) {
            logger.warn("Failed to check database initialization status", e);
            return false;
        }
    }

    /**
     * Clean up test data by executing the cleanup.sql script
     */
    public void cleanupTestData() {
        logger.info("Cleaning up test data using cleanup.sql");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Load and execute the cleanup script
            String cleanupSql = loadCleanupScript();
            executeSchemaScript(statement, cleanupSql);
            
            logger.info("Test data cleanup completed");
        } catch (SQLException | IOException e) {
            logger.warn("Failed to clean up test data", e);
        }
    }

    /**
     * Drop all tables by executing the drop-all-tables.sql script
     */
    public void dropAllTables() {
        logger.info("Dropping all tables using drop-all-tables.sql");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Load and execute the drop tables script
            String dropSql = loadDropTablesScript();
            executeSchemaScript(statement, dropSql);
            
            logger.info("All tables dropped successfully");
        } catch (SQLException | IOException e) {
            logger.error("Failed to drop all tables", e);
            throw new RuntimeException("Failed to drop all tables", e);
        }
    }

    /**
     * Load the cleanup SQL from the resources/cleanup.sql file
     */
    private String loadCleanupScript() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("cleanup.sql");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Load the drop tables SQL from the resources/drop-all-tables.sql file
     */
    private String loadDropTablesScript() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("drop-all-tables.sql");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}