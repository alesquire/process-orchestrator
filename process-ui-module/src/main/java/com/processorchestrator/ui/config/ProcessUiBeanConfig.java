package com.processorchestrator.ui.config;

import com.processorchestrator.config.ProcessType;
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
     * Create ProcessTypeRegistry bean and register process types
     */
    @Bean
    @Primary
    public ProcessTypeRegistry processTypeRegistry() {
        ProcessTypeRegistry registry = new ProcessTypeRegistry();
        
        // Register the same process types as used in the core module
        registerProcessTypes(registry);
        
        return registry;
    }

    /**
     * Register process types in the registry
     */
    private void registerProcessTypes(ProcessTypeRegistry registry) {
        // Process Type 1: Single Task Process
        ProcessType singleTaskProcess = new ProcessType("single-task-process", "Process with one task")
                .addTask("validate", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"Validating data from ${input_file}\"", System.getProperty("java.io.tmpdir"), 30, 2);
        registry.register(singleTaskProcess);

        // Process Type 2: Two Task Process  
        ProcessType twoTaskProcess = new ProcessType("two-task-process", "Process with two tasks")
                .addTask("extract", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"Extracting data from ${input_file} to ${output_dir}\"", System.getProperty("java.io.tmpdir"), 45, 2)
                .addTask("transform", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"Transforming data from ${output_dir}/extracted.json to ${output_dir}/transformed.json\"", System.getProperty("java.io.tmpdir"), 60, 3);
        registry.register(twoTaskProcess);

        // Process Type 3: Three Task Process
        ProcessType threeTaskProcess = new ProcessType("three-task-process", "Process with three tasks")
                .addTask("load", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"Loading data from ${input_file} to ${output_dir}/loaded.json\"", System.getProperty("java.io.tmpdir"), 30, 2)
                .addTask("process", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"Processing data from ${output_dir}/loaded.json to ${output_dir}/processed.json\"", System.getProperty("java.io.tmpdir"), 60, 3)
                .addTask("analyze", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"Analyzing data from ${output_dir}/processed.json to ${output_dir}/analysis.json\"", System.getProperty("java.io.tmpdir"), 45, 2);
        registry.register(threeTaskProcess);
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
