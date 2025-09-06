package com.processorchestrator.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration utility for loading database properties
 */
public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    
    private static final String MAIN_PROPERTIES_FILE = "application.properties";
    private static final String TEST_PROPERTIES_FILE = "test.properties";
    
    private static Properties properties;
    
    static {
        loadProperties();
    }
    
    /**
     * Load properties from the appropriate file based on the environment
     */
    private static void loadProperties() {
        properties = new Properties();
        
        // Try to load test properties first (for test environment)
        try (InputStream testStream = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream(TEST_PROPERTIES_FILE)) {
            if (testStream != null) {
                properties.load(testStream);
                logger.info("Loaded test database configuration from {}", TEST_PROPERTIES_FILE);
                return;
            }
        } catch (IOException e) {
            logger.warn("Could not load test properties file: {}", e.getMessage());
        }
        
        // Fall back to main properties file
        try (InputStream mainStream = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream(MAIN_PROPERTIES_FILE)) {
            if (mainStream != null) {
                properties.load(mainStream);
                logger.info("Loaded main database configuration from {}", MAIN_PROPERTIES_FILE);
            } else {
                logger.warn("Could not find properties file, using default values");
                setDefaultProperties();
            }
        } catch (IOException e) {
            logger.error("Error loading properties file: {}", e.getMessage());
            setDefaultProperties();
        }
    }
    
    /**
     * Set default properties if no properties file is found
     */
    private static void setDefaultProperties() {
        properties.setProperty("db.url", "jdbc:postgresql://localhost:5432/process_orchestrator");
        properties.setProperty("db.username", "postgres");
        properties.setProperty("db.password", "password");
        logger.info("Using default database configuration");
    }
    
    /**
     * Get database URL
     */
    public static String getDatabaseUrl() {
        return properties.getProperty("test.db.url", properties.getProperty("db.url"));
    }
    
    /**
     * Get database username
     */
    public static String getDatabaseUsername() {
        return properties.getProperty("test.db.username", properties.getProperty("db.username"));
    }
    
    /**
     * Get database password
     */
    public static String getDatabasePassword() {
        return properties.getProperty("test.db.password", properties.getProperty("db.password"));
    }
    
    /**
     * Get connection pool initial size
     */
    public static int getConnectionPoolInitialSize() {
        return Integer.parseInt(properties.getProperty("test.db.pool.initial.size", 
                properties.getProperty("db.pool.initial.size", "5")));
    }
    
    /**
     * Get connection pool max size
     */
    public static int getConnectionPoolMaxSize() {
        return Integer.parseInt(properties.getProperty("test.db.pool.max.size", 
                properties.getProperty("db.pool.max.size", "20")));
    }
    
    /**
     * Get connection pool min idle
     */
    public static int getConnectionPoolMinIdle() {
        return Integer.parseInt(properties.getProperty("test.db.pool.min.idle", 
                properties.getProperty("db.pool.min.idle", "5")));
    }
    
    /**
     * Get connection timeout
     */
    public static int getConnectionTimeout() {
        return Integer.parseInt(properties.getProperty("test.db.connection.timeout", 
                properties.getProperty("db.connection.timeout", "30000")));
    }
    
    /**
     * Get socket timeout
     */
    public static int getSocketTimeout() {
        return Integer.parseInt(properties.getProperty("test.db.socket.timeout", 
                properties.getProperty("db.socket.timeout", "60000")));
    }
    
    /**
     * Reload properties (useful for testing)
     */
    public static void reloadProperties() {
        loadProperties();
    }
    
    /**
     * Get all properties (for debugging)
     */
    public static Properties getAllProperties() {
        return new Properties(properties);
    }
}
