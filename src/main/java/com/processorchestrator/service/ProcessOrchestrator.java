package com.processorchestrator.service;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.processorchestrator.config.ProcessType;
import com.processorchestrator.config.ProcessTypeRegistry;
import com.processorchestrator.executor.CLITaskExecutor;
import com.processorchestrator.model.ProcessInputData;
import com.processorchestrator.model.ProcessData;
import com.processorchestrator.model.ProcessDetails;
import com.processorchestrator.model.TaskData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Process Orchestrator service that leverages db-scheduler for task execution
 * Simplified to work with 3-table schema: process_record, tasks, scheduled_tasks
 */
public class ProcessOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(ProcessOrchestrator.class);
    
    private final Scheduler scheduler;
    private final SchedulerClient schedulerClient;
    private final ProcessTypeRegistry processTypeRegistry;
    private final CLITaskExecutor taskExecutor;
    
    // Task definitions for db-scheduler
    private final OneTimeTask<ProcessData> processTask;
    private final OneTimeTask<TaskData> cliTask;
    
    // Task name for db-scheduler
    private static final String PROCESS_TASK_NAME = "process-execution";
    private static final String CLI_TASK_NAME = "cli-execution";

    public ProcessOrchestrator(DataSource dataSource, ProcessTypeRegistry processTypeRegistry) {
        this.processTypeRegistry = processTypeRegistry;
        this.taskExecutor = new CLITaskExecutor();
        
        // Create db-scheduler tasks
        this.processTask = Tasks.oneTime(PROCESS_TASK_NAME, ProcessData.class)
                .execute(this::executeProcess);
        
        this.cliTask = Tasks.oneTime(CLI_TASK_NAME, TaskData.class)
                .execute(this::executeCLITask);
        
        // Initialize scheduler
        this.scheduler = Scheduler.create(dataSource)
                .threads(10) // Support parallel process execution
                .pollingInterval(Duration.ofSeconds(5))
                .build();
        
        this.schedulerClient = scheduler;
    }

    /**
     * Start a new process with input data
     */
    public String startProcess(String processTypeName, ProcessInputData inputData) {
        return startProcess(processTypeName, inputData, generateProcessId());
    }
    
    public String startProcess(String processTypeName, ProcessInputData inputData, String processId) {
        logger.info("Starting process of type: {} with id: {}", processTypeName, processId);
        
        ProcessType processType = processTypeRegistry.getProcessTypeOrThrow(processTypeName);
        
        // Create process data with input context
        ProcessData processData = new ProcessData(processId, processTypeName, 
                                                processType.getTaskCount(), inputData);
        
        // Create task data for each task in the process
        List<TaskData> tasks = new ArrayList<>();
        for (int i = 0; i < processType.getTaskCount(); i++) {
            var taskDef = processType.getTask(i);
            String taskId = processId + "-task-" + i;
            
            TaskData taskData = new TaskData(taskId, processId, taskDef.getName(), 
                                           taskDef.getCommand(), taskDef.getWorkingDirectory(),
                                           taskDef.getTimeoutMinutes(), taskDef.getMaxRetries());
            tasks.add(taskData);
        }
        processData.setTasks(tasks);
        
        // Schedule the process for immediate execution
        schedulerClient.schedule(
            processTask.instance(processId, processData),
            Instant.now()
        );
        
        logger.info("Process {} scheduled for execution", processId);
        return processId;
    }

    /**
     * Execute a process (called by db-scheduler)
     */
    private void executeProcess(TaskInstance<ProcessData> taskInstance, ExecutionContext context) {
        ProcessData processData = taskInstance.getData();
        String processId = processData.getProcessId();
        
        logger.info("Executing process: {}", processId);
        
        try {
        // Mark process as started
        processData.markAsStarted();
        
        // Persist process data
        // Save process data (simplified - no longer using ProcessResultService)
        logger.info("Process {} started with {} tasks", processData.getProcessId(), processData.getTasks().size());
        
        // Execute current task
        executeCurrentTask(processData, context);
            
        } catch (Exception e) {
            logger.error("Error executing process: {}", processId, e);
            processData.markAsFailed("Process execution error: " + e.getMessage());
        }
    }

    /**
     * Execute the current task in the process
     */
    private void executeCurrentTask(ProcessData processData, ExecutionContext context) {
        TaskData currentTask = processData.getCurrentTask();
        if (currentTask == null) {
            logger.warn("No current task found for process: {}", processData.getProcessId());
            return;
        }
        
        logger.info("Executing task: {} for process: {}", currentTask.getName(), processData.getProcessId());
        
        // Mark task as started
        currentTask.markAsStarted();
        
        // Persist task data
        // Save task data (simplified - no longer using ProcessResultService)
        logger.debug("Task {} completed with status {}", currentTask.getTaskId(), currentTask.getStatus());
        
        // Schedule CLI task execution
        schedulerClient.schedule(
            cliTask.instance(currentTask.getTaskId(), currentTask),
            Instant.now()
        );
    }

    /**
     * Execute a CLI task (called by db-scheduler)
     */
    private void executeCLITask(TaskInstance<TaskData> taskInstance, ExecutionContext context) {
        TaskData taskData = taskInstance.getData();
        String taskId = taskData.getTaskId();
        
        logger.info("Executing CLI task: {}", taskId);
        
        try {
            // Build command with context
            String command = buildCommandWithContext(taskData, taskInstance.getData());
            
            // Execute the CLI command
            CLITaskExecutor.ExecutionResult result = taskExecutor.execute(taskData, command);
            
            if (result.isSuccess()) {
                logger.info("CLI task {} completed successfully", taskId);
                taskData.markAsCompleted(result.getExitCode(), result.getOutput());
                
                // Persist task results
                // Save task data (simplified - no longer using ProcessResultService)
                logger.debug("Task {} completed successfully", taskData.getTaskId());
                
                // Update process context with results
                updateProcessContext(taskData, result);
                
                // Continue with next task in the process
                continueProcess(taskData, context);
                
            } else {
                logger.warn("CLI task {} failed: {}", taskId, result.getErrorMessage());
                taskData.markAsFailed(result.getErrorMessage());
                
                // Persist task failure
                // Save task data (simplified - no longer using ProcessResultService)
                logger.debug("Task {} completed successfully", taskData.getTaskId());
                
                // Check if we can retry
                if (taskData.canRetry()) {
                    logger.info("Retrying CLI task {} (attempt {}/{})", taskId, 
                              taskData.getRetryCount() + 1, taskData.getMaxRetries());
                    taskData.incrementRetryCount();
                    taskData.setStatus("PENDING");
                    
                    // Persist retry attempt
                    // Save task data (simplified - no longer using ProcessResultService)
                logger.debug("Task {} completed successfully", taskData.getTaskId());
                    
                    // Schedule retry
                    schedulerClient.schedule(
                        cliTask.instance(taskId, taskData),
                        Instant.now().plusSeconds(30) // Retry after 30 seconds
                    );
                } else {
                    logger.error("CLI task {} failed permanently, failing process", taskId);
                    failProcess(taskData, result.getErrorMessage(), context);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error executing CLI task: {}", taskId, e);
            taskData.markAsFailed("Execution error: " + e.getMessage());
            failProcess(taskData, "CLI task execution error: " + e.getMessage(), context);
        }
    }

    /**
     * Build command with input context and process context
     */
    private String buildCommandWithContext(TaskData taskData, TaskData originalTaskData) {
        String command = originalTaskData.getCommand();
        
        // Get process data to access input context
        ProcessData processData = getProcessDataFromScheduler(taskData.getProcessId());
        if (processData != null && processData.getInputData() != null) {
            ProcessInputData inputData = processData.getInputData();
            
            // Replace input file placeholder
            if (inputData.getInputFile() != null) {
                command = command.replace("${input_file}", inputData.getInputFile());
            }
            if (inputData.getOutputDir() != null) {
                command = command.replace("${output_dir}", inputData.getOutputDir());
            }
            
            // Replace config variables
            if (inputData.getConfig() != null) {
                for (var entry : inputData.getConfig().entrySet()) {
                    command = command.replace("${" + entry.getKey() + "}", entry.getValue());
                }
            }
        }
        
        // Replace process context variables
        if (processData != null && processData.getProcessContext() != null) {
            for (var entry : processData.getProcessContext().entrySet()) {
                command = command.replace("${" + entry.getKey() + "}", entry.getValue().toString());
            }
        }
        
        return command;
    }

    /**
     * Update process context with task results
     */
    private void updateProcessContext(TaskData taskData, CLITaskExecutor.ExecutionResult result) {
        ProcessData processData = getProcessDataFromScheduler(taskData.getProcessId());
        if (processData != null) {
            // Add task-specific context
            processData.addContext(taskData.getName() + "_exit_code", result.getExitCode());
            processData.addContext(taskData.getName() + "_output", result.getOutput());
            processData.addContext("last_completed_task", taskData.getName());
        }
    }

    /**
     * Continue process execution with next task
     */
    private void continueProcess(TaskData completedTask, ExecutionContext context) {
        ProcessData processData = getProcessDataFromScheduler(completedTask.getProcessId());
        if (processData == null) {
            logger.error("Could not find process data for task: {}", completedTask.getTaskId());
            return;
        }
        
        // Move to next task
        processData.moveToNextTask();
        
        if (processData.isCompleted()) {
            logger.info("Process {} completed successfully", processData.getProcessId());
        } else if (processData.hasMoreTasks()) {
            logger.info("Process {} moving to next task (index: {})", 
                      processData.getProcessId(), processData.getCurrentTaskIndex());
            
            // Schedule next task execution
            schedulerClient.schedule(
                processTask.instance(processData.getProcessId(), processData),
                Instant.now()
            );
        }
    }

    /**
     * Fail the process
     */
    private void failProcess(TaskData failedTask, String errorMessage, ExecutionContext context) {
        ProcessData processData = getProcessDataFromScheduler(failedTask.getProcessId());
        if (processData == null) {
            logger.error("Could not find process data for failed task: {}", failedTask.getTaskId());
            return;
        }
        
        processData.markAsFailed(errorMessage);
        logger.error("Process {} failed: {}", processData.getProcessId(), errorMessage);
    }

    /**
     * Get process data from scheduler (simplified - in real implementation, you'd query the database)
     */
    private ProcessData getProcessDataFromScheduler(String processId) {
        // TODO: Implement proper process data retrieval from database
        // For now, this is a placeholder - in a real implementation, you would:
        // 1. Query the scheduled_tasks table
        // 2. Deserialize the task_data field
        // 3. Return the ProcessData object
        
        // This is a limitation of the current approach - we need to persist process data separately
        // or query the scheduler's internal state
        return null;
    }

    /**
     * Generate a unique process ID
     */
    private String generateProcessId() {
        return "process-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Start the scheduler
     */
    public void start() {
        scheduler.start();
        logger.info("Process Orchestrator started");
    }

    /**
     * Stop the scheduler
     */
    public void stop() {
        scheduler.stop();
        logger.info("Process Orchestrator stopped");
    }

    /**
     * Get process data by process ID
     * Note: This method is simplified and may need to be updated based on actual requirements
     */
    public ProcessData getProcess(String processId) {
        // TODO: Implement based on simplified schema
        logger.warn("getProcess method needs to be implemented for simplified schema");
        return null;
    }

    /**
     * Get all tasks for a process
     * Note: This method is simplified and may need to be updated based on actual requirements
     */
    public List<TaskData> getProcessTasks(String processId) {
        // TODO: Implement based on simplified schema
        logger.warn("getProcessTasks method needs to be implemented for simplified schema");
        return new ArrayList<>();
    }

    /**
     * Get all processes
     * Note: This method is simplified and may need to be updated based on actual requirements
     */
    public List<ProcessData> getAllProcesses() {
        // TODO: Implement based on simplified schema
        logger.warn("getAllProcesses method needs to be implemented for simplified schema");
        return new ArrayList<>();
    }

    /**
     * Get processes by status
     * Note: This method is simplified and may need to be updated based on actual requirements
     */
    public List<ProcessData> getProcessesByStatus(String status) {
        // TODO: Implement based on simplified schema
        logger.warn("getProcessesByStatus method needs to be implemented for simplified schema");
        return new ArrayList<>();
    }
}