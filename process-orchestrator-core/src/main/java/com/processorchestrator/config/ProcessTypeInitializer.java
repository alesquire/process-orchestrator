package com.processorchestrator.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated class for initializing and registering process types.
 * This separates the process type registration logic from the main application class.
 */
public class ProcessTypeInitializer {
    private static final Logger logger = LoggerFactory.getLogger(ProcessTypeInitializer.class);

    /**
     * Register all default process types in the given registry
     */
    public static void registerDefaultProcessTypes(ProcessTypeRegistry registry) {
        logger.info("Registering default process types...");

        // Single Task Process
        ProcessType singleTaskProcess = new ProcessType("single-task-process", "Process with one task")
                .addTask("test-1-1", "java -cp ../process-orchestrator-core/target/classes com.processorchestrator.util.MessagePrinter \"Validating data from ${input_file}\"", System.getProperty("user.dir"), 30, 2);
        registry.register(singleTaskProcess);
        logger.debug("Registered single-task-process with {} tasks", singleTaskProcess.getTaskCount());

        // Two Task Process  
        ProcessType twoTaskProcess = new ProcessType("two-task-process", "Process with two tasks")
                .addTask("test-2-1", "java -cp ../process-orchestrator-core/target/classes com.processorchestrator.util.MessagePrinter \"Extracting data from ${input_file} to ${output_dir}\"", System.getProperty("user.dir"), 45, 2)
                .addTask("test-2-2", "java -cp ../process-orchestrator-core/target/classes com.processorchestrator.util.MessagePrinter \"Transforming data from ${output_dir}/extracted.json to ${output_dir}/transformed.json\"", System.getProperty("user.dir"), 60, 3);
        registry.register(twoTaskProcess);
        logger.debug("Registered two-task-process with {} tasks", twoTaskProcess.getTaskCount());

        // Three Task Process
        ProcessType threeTaskProcess = new ProcessType("three-task-process", "Process with three tasks")
                .addTask("test-3-1", "java -cp ../process-orchestrator-core/target/classes com.processorchestrator.util.MessagePrinter \"Loading data from ${input_file} to ${output_dir}/loaded.json\"", System.getProperty("user.dir"), 30, 2)
                .addTask("test-3-2", "java -cp ../process-orchestrator-core/target/classes com.processorchestrator.util.MessagePrinter \"Processing data from ${output_dir}/loaded.json to ${output_dir}/processed.json\"", System.getProperty("user.dir"), 60, 3)
                .addTask("test-3-3", "java -cp ../process-orchestrator-core/target/classes com.processorchestrator.util.MessagePrinter \"Analyzing data from ${output_dir}/processed.json to ${output_dir}/analysis.json\"", System.getProperty("user.dir"), 45, 2);
        registry.register(threeTaskProcess);
        logger.debug("Registered three-task-process with {} tasks", threeTaskProcess.getTaskCount());

        // Failing Process (for testing error handling)
        ProcessType failingProcess = new ProcessType("failing-process", "Process that intentionally fails")
                .addTask("test-failing", "cmd /c exit 1", System.getProperty("java.io.tmpdir"), 30, 2);
        registry.register(failingProcess);
        logger.debug("Registered failing-process with {} tasks", failingProcess.getTaskCount());
        
        logger.info("Successfully registered {} process types", registry.getAllProcessTypes().size());
    }

    /**
     * Get a summary of all registered process types
     */
    public static String getProcessTypesSummary(ProcessTypeRegistry registry) {
        StringBuilder summary = new StringBuilder();
        summary.append("Registered Process Types:\n");
        
        registry.getAllProcessTypes().values().forEach(processType -> {
            summary.append(String.format("  - %s: %s (%d tasks)\n", 
                processType.getName(), 
                processType.getDescription(), 
                processType.getTaskCount()));
        });
        
        return summary.toString();
    }
}
