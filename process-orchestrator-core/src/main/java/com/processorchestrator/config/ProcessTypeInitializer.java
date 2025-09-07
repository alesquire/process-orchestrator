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

        // Data Processing Pipeline
        ProcessType dataProcessingPipeline = new ProcessType("data-processing-pipeline", "Data processing pipeline")
                .addTask("validate", "python scripts/validate.py ${input_file}", "/data", 30, 2)
                .addTask("transform", "python scripts/transform.py ${input_file} ${output_dir}", "/data", 60, 3)
                .addTask("load", "python scripts/load.py ${output_dir}", "/data", 45, 2);
        
        registry.register(dataProcessingPipeline);
        logger.debug("Registered data-processing-pipeline with {} tasks", dataProcessingPipeline.getTaskCount());
        
        // Deployment Pipeline
        ProcessType deploymentPipeline = new ProcessType("deployment-pipeline", "Application deployment pipeline")
                .addTask("build", "mvn clean package", "/app", 15, 2)
                .addTask("test", "mvn test", "/app", 20, 3)
                .addTask("deploy", "kubectl apply -f deployment.yaml", "/app", 10, 2);
        
        registry.register(deploymentPipeline);
        logger.debug("Registered deployment-pipeline with {} tasks", deploymentPipeline.getTaskCount());
        
        // Backup Process
        ProcessType backupProcess = new ProcessType("backup-process", "Database backup process")
                .addTask("backup-db", "pg_dump -h localhost -U postgres mydb > ${output_dir}/backup.sql", "/backups", 30, 2)
                .addTask("compress", "gzip ${output_dir}/backup.sql", "/backups", 5, 1)
                .addTask("upload", "aws s3 cp ${output_dir}/backup.sql.gz s3://my-backups/", "/backups", 15, 3);
        
        registry.register(backupProcess);
        logger.debug("Registered backup-process with {} tasks", backupProcess.getTaskCount());
        
        // ETL Pipeline
        ProcessType etlPipeline = new ProcessType("etl-pipeline", "Extract, Transform, Load pipeline")
                .addTask("extract", "python scripts/extract.py ${input_file}", "/etl", 45, 2)
                .addTask("transform", "python scripts/transform.py ${input_file} ${output_dir}", "/etl", 90, 3)
                .addTask("load", "python scripts/load.py ${output_dir}", "/etl", 30, 2)
                .addTask("notify", "python scripts/notify.py ${output_dir}", "/etl", 10, 1);
        
        registry.register(etlPipeline);
        logger.debug("Registered etl-pipeline with {} tasks", etlPipeline.getTaskCount());
        
        // Complete Data Processing Pipeline - Load, Process, Generate, Analyze
        ProcessType completeDataProcessingPipeline = new ProcessType("complete-data-processing-pipeline", "Complete data processing pipeline")
                .addTask("load", "python scripts/load_data.py ${input_file} ${output_dir}/loaded_data.json", "/data", 30, 2)
                .addTask("process", "python scripts/process_data.py ${output_dir}/loaded_data.json ${output_dir}/processed_data.json", "/data", 60, 3)
                .addTask("generate", "python scripts/generate_report.py ${output_dir}/processed_data.json ${output_dir}/report.html", "/data", 45, 2)
                .addTask("analyze", "python scripts/analyze_results.py ${output_dir}/report.html ${output_dir}/analysis.json", "/data", 30, 2);
        
        registry.register(completeDataProcessingPipeline);
        logger.debug("Registered complete-data-processing-pipeline with {} tasks", completeDataProcessingPipeline.getTaskCount());

        // Test Process Types (for testing purposes)
        ProcessType singleTaskProcess = new ProcessType("single-task-process", "Process with one task")
                .addTask("validate", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"Validating data from ${input_file}\"", System.getProperty("java.io.tmpdir"), 30, 2);
        registry.register(singleTaskProcess);
        logger.debug("Registered single-task-process with {} tasks", singleTaskProcess.getTaskCount());

        ProcessType twoTaskProcess = new ProcessType("two-task-process", "Process with two tasks")
                .addTask("extract", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"Extracting data from ${input_file} to ${output_dir}\"", System.getProperty("java.io.tmpdir"), 45, 2)
                .addTask("transform", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"Transforming data from ${output_dir}/extracted.json to ${output_dir}/transformed.json\"", System.getProperty("java.io.tmpdir"), 60, 3);
        registry.register(twoTaskProcess);
        logger.debug("Registered two-task-process with {} tasks", twoTaskProcess.getTaskCount());

        ProcessType threeTaskProcess = new ProcessType("three-task-process", "Process with three tasks")
                .addTask("load", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"Loading data from ${input_file} to ${output_dir}/loaded.json\"", System.getProperty("java.io.tmpdir"), 30, 2)
                .addTask("process", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"Processing data from ${output_dir}/loaded.json to ${output_dir}/processed.json\"", System.getProperty("java.io.tmpdir"), 60, 3)
                .addTask("analyze", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"Analyzing data from ${output_dir}/processed.json to ${output_dir}/analysis.json\"", System.getProperty("java.io.tmpdir"), 45, 2);
        registry.register(threeTaskProcess);
        logger.debug("Registered three-task-process with {} tasks", threeTaskProcess.getTaskCount());

        // Failing Process (for testing error handling)
        ProcessType failingProcess = new ProcessType("failing-process", "Process that intentionally fails")
                .addTask("failing-task", "cmd /c exit 1", System.getProperty("java.io.tmpdir"), 30, 2);
        registry.register(failingProcess);
        logger.debug("Registered failing-process with {} tasks", failingProcess.getTaskCount());
        
        logger.info("Successfully registered {} process types", registry.getAllProcessTypes().size());
    }

    /**
     * Register test process types (used in tests)
     */
    public static void registerTestProcessTypes(ProcessTypeRegistry registry) {
        logger.info("Registering test process types...");

        // Single Print Task (for testing)
        ProcessType singlePrintTask = new ProcessType("single-print-task", "Single print task for testing")
                .addTask("print-text", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"!!!THIS IS A TEST MESSAGE!!!\"", System.getProperty("java.io.tmpdir"), 30, 2);
        registry.register(singlePrintTask);
        logger.debug("Registered single-print-task with {} tasks", singlePrintTask.getTaskCount());

        // Dual Print Task (for testing)
        ProcessType dualPrintTask = new ProcessType("dual-print-task", "Dual print task for testing")
                .addTask("print-greeting", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"=== TASK 1: GREETING === Hello from Task 1!\"", System.getProperty("java.io.tmpdir"), 30, 2)
                .addTask("print-farewell", "java -cp " + System.getProperty("user.dir") + "/target/classes com.processorchestrator.util.MessagePrinter \"=== TASK 2: FAREWELL === Goodbye from Task 2!\"", System.getProperty("java.io.tmpdir"), 30, 2);
        registry.register(dualPrintTask);
        logger.debug("Registered dual-print-task with {} tasks", dualPrintTask.getTaskCount());

        // Test Process Type (for unit tests)
        ProcessType testProcessType = new ProcessType("data-processing", "Data processing pipeline")
                .addTask("validate", "python scripts/validate.py ${input_file}", "/data", 30, 2)
                .addTask("transform", "python scripts/transform.py ${input_file} ${output_dir}", "/data", 60, 3)
                .addTask("load", "python scripts/load.py ${output_dir}", "/data", 45, 2);
        registry.register(testProcessType);
        logger.debug("Registered data-processing with {} tasks", testProcessType.getTaskCount());

        logger.info("Successfully registered {} test process types", registry.getAllProcessTypes().size());
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
