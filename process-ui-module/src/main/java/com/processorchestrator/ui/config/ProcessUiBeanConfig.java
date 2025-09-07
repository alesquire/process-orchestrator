package com.processorchestrator.ui.config;

import com.processorchestrator.config.ProcessTypeRegistry;
import com.processorchestrator.config.ProcessTypeInitializer;
import com.processorchestrator.controller.ProcessController;
import com.processorchestrator.dao.ProcessRecordDAO;
import com.processorchestrator.service.ProcessOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Configuration class for Process UI module beans.
 * Provides the necessary beans for dependency injection.
 */
@Configuration
public class ProcessUiBeanConfig {

    @Autowired
    private DataSource dataSource;

    /**
     * Create ProcessTypeRegistry bean and register process types
     */
    @Bean
    @Primary
    public ProcessTypeRegistry processTypeRegistry() {
        ProcessTypeRegistry registry = new ProcessTypeRegistry();
        
        // Register the same process types as used in the core module
        ProcessTypeInitializer.registerDefaultProcessTypes(registry);
        
        return registry;
    }

    /**
     * Create ProcessRecordDAO bean
     */
    @Bean
    @Primary
    public ProcessRecordDAO processRecordDAO() {
        return new ProcessRecordDAO(dataSource);
    }

    /**
     * Create ProcessOrchestrator bean
     */
    @Bean
    @Primary
    public ProcessOrchestrator processOrchestrator() {
        return new ProcessOrchestrator(dataSource, processTypeRegistry());
    }

    /**
     * Create ProcessController bean
     */
    @Bean
    @Primary
    public ProcessController processController() {
        return new ProcessController(processRecordDAO(), processOrchestrator(), processTypeRegistry());
    }

    /**
     * Start the ProcessOrchestrator scheduler when the application starts
     */
    @Bean
    public ApplicationRunner schedulerStarter(ProcessOrchestrator processOrchestrator) {
        return args -> {
            System.out.println("Starting ProcessOrchestrator scheduler...");
            try {
                processOrchestrator.start();
                System.out.println("ProcessOrchestrator scheduler started successfully");
            } catch (Exception e) {
                System.err.println("Failed to start ProcessOrchestrator scheduler: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}
