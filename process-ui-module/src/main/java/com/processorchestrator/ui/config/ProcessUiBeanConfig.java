package com.processorchestrator.ui.config;

import com.processorchestrator.config.ProcessTypeRegistry;
import com.processorchestrator.dao.ProcessRecordDAO;
import com.processorchestrator.service.ProcessOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
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
     * Create ProcessTypeRegistry bean
     */
    @Bean
    @Primary
    public ProcessTypeRegistry processTypeRegistry() {
        ProcessTypeRegistry registry = new ProcessTypeRegistry();
        // Register default process types if needed
        // This could be expanded to load from configuration
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
}
