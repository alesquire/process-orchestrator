package com.processorchestrator.service;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.processorchestrator.config.ProcessType;
import com.processorchestrator.config.ProcessTypeRegistry;
import com.processorchestrator.dao.ProcessRecordDAO;
import com.processorchestrator.executor.CLITaskExecutor;
import com.processorchestrator.model.ProcessInputData;
import com.processorchestrator.model.ProcessData;
import com.processorchestrator.model.TaskData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final ProcessRecordDAO processRecordDAO;
    private final DataSource dataSource;
    
    // In-memory cache for process data
    private final Map<String, ProcessData> processDataCache = new ConcurrentHashMap<>();
    
    // Task definitions for db-scheduler
    private final OneTimeTask<ProcessData> processTask;
    private final OneTimeTask<TaskData> cliTask;
    
    // Task name for db-scheduler
    private static final String PROCESS_TASK_NAME = "process-execution";
    private static final String CLI_TASK_NAME = "cli-execution";

    public ProcessOrchestrator(DataSource dataSource, ProcessTypeRegistry processTypeRegistry) {
        this.dataSource = dataSource;
        this.processTypeRegistry = processTypeRegistry;
        this.taskExecutor = new CLITaskExecutor();
        this.processRecordDAO = new ProcessRecordDAO(dataSource);
        
        // Create db-scheduler tasks
        this.processTask = Tasks.oneTime(PROCESS_TASK_NAME, ProcessData.class)
                .execute(this::executeProcess);
        
        this.cliTask = Tasks.oneTime(CLI_TASK_NAME, TaskData.class)
                .execute(this::executeCLITask);
        
        // Initialize scheduler with task registrations
        this.scheduler = Scheduler.create(dataSource, processTask, cliTask)
                .threads(10) // Support parallel process execution
                .pollingInterval(Duration.ofSeconds(5))
                .build();
        
        this.schedulerClient = scheduler;
    }

    /**
     * Start a new process with input data
     */
    public String startProcess(String processTypeName, ProcessInputData inputData) {
        return startProcess(processTypeName, inputData, generateProcessId(), null);
    }
    
    public String startProcess(String processTypeName, ProcessInputData inputData, String processId) {
        return startProcess(processTypeName, inputData, processId, null);
    }
    
    public String startProcess(String processTypeName, ProcessInputData inputData, String processId, String processRecordId) {
        logger.info("Starting process of type: {} with id: {}", processTypeName, processId);
        
        ProcessType processType = processTypeRegistry.getProcessTypeOrThrow(processTypeName);
        
        // Create process data with input context
        ProcessData processData = new ProcessData(processId, processTypeName, 
                                                processType.getTaskCount(), inputData, processRecordId);
        
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
        
        // Cache the process data for later retrieval
        processDataCache.put(processId, processData);
        
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
        
        // Persist task data to database
        saveTaskData(currentTask);
        logger.debug("Task {} started and persisted", currentTask.getTaskId());
        
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
                saveTaskData(taskData);
                logger.debug("Task {} completed successfully and persisted", taskData.getTaskId());
                
                // Update process context with results
                updateProcessContext(taskData, result);
                
                // Continue with next task in the process
                continueProcess(taskData, context);
                
            } else {
                logger.warn("CLI task {} failed: {}", taskId, result.getErrorMessage());
                taskData.markAsFailed(result.getErrorMessage());
                
                // Persist task failure
                saveTaskData(taskData);
                logger.debug("Task {} failed and persisted", taskData.getTaskId());
                
                // Check if we can retry
                if (taskData.canRetry()) {
                    logger.info("Retrying CLI task {} (attempt {}/{})", taskId, 
                              taskData.getRetryCount() + 1, taskData.getMaxRetries());
                    taskData.incrementRetryCount();
                    taskData.setStatus("PENDING");
                    
                    // Persist retry attempt
                    saveTaskData(taskData);
                    logger.debug("Task {} retry attempt persisted", taskData.getTaskId());
                    
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
            saveTaskData(taskData);
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
        
        if (!processData.hasMoreTasks()) {
            // All tasks completed, mark process as completed
            processData.markAsCompleted();
            logger.info("Process {} completed successfully", processData.getProcessId());
            
            // Update database status if we have a processRecordId
            if (processData.getProcessRecordId() != null) {
                try {
                    processRecordDAO.updateStatus(processData.getProcessRecordId(), "COMPLETED", 
                                                processData.getCompletedAt(), null);
                    logger.info("Updated process record {} status to COMPLETED", processData.getProcessRecordId());
                } catch (Exception e) {
                    logger.error("Failed to update process record status: {}", processData.getProcessRecordId(), e);
                }
            }
            
            // Clean up cached process data
            processDataCache.remove(processData.getProcessId());
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
        
        // Update database status if we have a processRecordId
        if (processData.getProcessRecordId() != null) {
            try {
                processRecordDAO.updateStatus(processData.getProcessRecordId(), "FAILED", 
                                            processData.getCompletedAt(), errorMessage);
                logger.info("Updated process record {} status to FAILED", processData.getProcessRecordId());
            } catch (Exception e) {
                logger.error("Failed to update process record status: {}", processData.getProcessRecordId(), e);
            }
        }
        
        // Clean up cached process data
        processDataCache.remove(processData.getProcessId());
    }

    /**
     * Save TaskData to the database
     */
    private void saveTaskData(TaskData taskData) {
        try {
            String sql = "INSERT INTO tasks (id, process_record_id, task_index, task_name, command, working_directory, " +
                        "timeout_minutes, max_retries, status, started_at, completed_at, " +
                        "exit_code, output, error_message, retry_count) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (id) DO UPDATE SET " +
                        "status = EXCLUDED.status, " +
                        "started_at = EXCLUDED.started_at, " +
                        "completed_at = EXCLUDED.completed_at, " +
                        "exit_code = EXCLUDED.exit_code, " +
                        "output = EXCLUDED.output, " +
                        "error_message = EXCLUDED.error_message, " +
                        "retry_count = EXCLUDED.retry_count";
            
            try (var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, taskData.getTaskId());
                
                // Get the actual process record ID from the cached process data
                ProcessData processData = processDataCache.get(taskData.getProcessId());
                String processRecordId;
                if (processData != null && processData.getProcessRecordId() != null) {
                    processRecordId = processData.getProcessRecordId();
                } else {
                    // If we can't find the process data, try to extract the process record ID from the task ID
                    // Task ID format: {processRecordId}-{timestamp}-{uuid}-task-{index}
                    String taskId = taskData.getTaskId();
                    if (taskId.contains("-task-")) {
                        processRecordId = taskId.substring(0, taskId.indexOf("-task-"));
                        // Extract the actual process record ID (before the first timestamp)
                        // Format: {processRecordId}-{timestamp}-{uuid}
                        String[] parts = processRecordId.split("-");
                        if (parts.length >= 3) {
                            // Find the first part that looks like a timestamp (long number)
                            int timestampIndex = -1;
                            for (int i = 1; i < parts.length; i++) {
                                try {
                                    Long.parseLong(parts[i]);
                                    timestampIndex = i;
                                    break;
                                } catch (NumberFormatException e) {
                                    // Not a timestamp, continue
                                }
                            }
                            if (timestampIndex > 0) {
                                processRecordId = String.join("-", java.util.Arrays.copyOfRange(parts, 0, timestampIndex));
                            } else {
                                processRecordId = parts[0]; // fallback to first part
                            }
                        } else {
                            processRecordId = parts[0]; // fallback to first part
                        }
                    } else {
                        processRecordId = taskData.getProcessId(); // fallback
                    }
                }
                statement.setString(2, processRecordId);
                statement.setInt(3, 0); // task_index - we'll set this properly later
                statement.setString(4, taskData.getName());
                statement.setString(5, taskData.getCommand());
                statement.setString(6, taskData.getWorkingDirectory());
                statement.setInt(7, taskData.getTimeoutMinutes());
                statement.setInt(8, taskData.getMaxRetries());
                statement.setString(9, taskData.getStatus());
                
                // Handle timestamps
                if (taskData.getStartedAt() != null) {
                    statement.setTimestamp(10, java.sql.Timestamp.from(taskData.getStartedAt()));
                } else {
                    statement.setNull(10, java.sql.Types.TIMESTAMP);
                }
                
                if (taskData.getCompletedAt() != null) {
                    statement.setTimestamp(11, java.sql.Timestamp.from(taskData.getCompletedAt()));
                } else {
                    statement.setNull(11, java.sql.Types.TIMESTAMP);
                }
                
                statement.setInt(12, taskData.getExitCode());
                statement.setString(13, taskData.getOutput());
                statement.setString(14, taskData.getErrorMessage());
                statement.setInt(15, taskData.getRetryCount());
                
                statement.executeUpdate();
                logger.debug("Task data saved for task: {}", taskData.getTaskId());
            }
            
        } catch (Exception e) {
            logger.error("Failed to save task data for task: {}", taskData.getTaskId(), e);
        }
    }

    /**
     * Get process data from scheduler cache
     */
    private ProcessData getProcessDataFromScheduler(String processId) {
        return processDataCache.get(processId);
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
     * Note: Since db-scheduler doesn't provide extensive querying APIs, we maintain
     * our own task tracking in the tasks table for process monitoring
     */
    public List<TaskData> getProcessTasks(String processId) {
        List<TaskData> tasks = new ArrayList<>();
        
        try {
            // Query our own tasks table instead of db-scheduler's internal tables
            String sql = "SELECT id, process_record_id, task_index, task_name, command, working_directory, " +
                        "timeout_minutes, max_retries, status, started_at, completed_at, " +
                        "exit_code, output, error_message, retry_count " +
                        "FROM tasks WHERE process_record_id = ? ORDER BY task_index";
            
            try (var connection = dataSource.getConnection();
                 var statement = connection.prepareStatement(sql)) {
                
                // Use the process record ID, not the orchestrator process ID
                statement.setString(1, processId);
                
                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        TaskData taskData = new TaskData();
                        taskData.setTaskId(resultSet.getString("id"));
                        taskData.setProcessId(resultSet.getString("process_record_id"));
                        taskData.setName(resultSet.getString("task_name"));
                        taskData.setCommand(resultSet.getString("command"));
                        taskData.setWorkingDirectory(resultSet.getString("working_directory"));
                        taskData.setTimeoutMinutes(resultSet.getInt("timeout_minutes"));
                        taskData.setMaxRetries(resultSet.getInt("max_retries"));
                        taskData.setStatus(resultSet.getString("status"));
                        
                        // Handle timestamps
                        var startedAt = resultSet.getTimestamp("started_at");
                        if (startedAt != null) {
                            taskData.setStartedAt(startedAt.toInstant());
                        }
                        
                        var completedAt = resultSet.getTimestamp("completed_at");
                        if (completedAt != null) {
                            taskData.setCompletedAt(completedAt.toInstant());
                        }
                        
                        taskData.setExitCode(resultSet.getInt("exit_code"));
                        taskData.setOutput(resultSet.getString("output"));
                        taskData.setErrorMessage(resultSet.getString("error_message"));
                        taskData.setRetryCount(resultSet.getInt("retry_count"));
                        
                        tasks.add(taskData);
                    }
                }
            }
            
            logger.debug("Retrieved {} tasks for process: {}", tasks.size(), processId);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve tasks for process: {}", processId, e);
        }
        
        return tasks;
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