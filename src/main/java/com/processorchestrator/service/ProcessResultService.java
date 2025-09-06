package com.processorchestrator.service;

import com.processorchestrator.model.ProcessData;
import com.processorchestrator.model.TaskData;
import com.processorchestrator.model.ProcessStatus;
import com.processorchestrator.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for persisting and retrieving process and task results
 */
public class ProcessResultService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessResultService.class);
    
    private final DataSource dataSource;

    public ProcessResultService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Persist process data to database
     */
    public void saveProcessData(ProcessData processData) {
        String sql = """
            INSERT INTO processes (process_id, process_type, status, current_task_index, total_tasks, 
                                 started_at, completed_at, error_message, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (process_id) DO UPDATE SET
                status = EXCLUDED.status,
                current_task_index = EXCLUDED.current_task_index,
                started_at = EXCLUDED.started_at,
                completed_at = EXCLUDED.completed_at,
                error_message = EXCLUDED.error_message,
                updated_at = CURRENT_TIMESTAMP
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, processData.getProcessId());
            statement.setString(2, processData.getProcessType());
            statement.setString(3, processData.getStatus().name());
            statement.setInt(4, processData.getCurrentTaskIndex());
            statement.setInt(5, processData.getTotalTasks());
            statement.setTimestamp(6, processData.getStartedAt() != null ? 
                Timestamp.from(processData.getStartedAt()) : null);
            statement.setTimestamp(7, processData.getCompletedAt() != null ? 
                Timestamp.from(processData.getCompletedAt()) : null);
            statement.setString(8, processData.getErrorMessage());
            
            statement.executeUpdate();
            logger.debug("Saved process data for process: {}", processData.getProcessId());
            
        } catch (SQLException e) {
            logger.error("Failed to save process data for process: {}", processData.getProcessId(), e);
            throw new RuntimeException("Failed to save process data", e);
        }
    }

    /**
     * Persist task data to database
     */
    public void saveTaskData(TaskData taskData) {
        String sql = """
            INSERT INTO tasks (task_id, process_id, task_index, name, command, working_directory,
                             timeout_minutes, max_retries, retry_count, status, started_at, 
                             completed_at, error_message, exit_code, output, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (task_id) DO UPDATE SET
                retry_count = EXCLUDED.retry_count,
                status = EXCLUDED.status,
                started_at = EXCLUDED.started_at,
                completed_at = EXCLUDED.completed_at,
                error_message = EXCLUDED.error_message,
                exit_code = EXCLUDED.exit_code,
                output = EXCLUDED.output,
                updated_at = CURRENT_TIMESTAMP
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, taskData.getTaskId());
            statement.setString(2, taskData.getProcessId());
            statement.setInt(3, extractTaskIndex(taskData.getTaskId()));
            statement.setString(4, taskData.getName());
            statement.setString(5, taskData.getCommand());
            statement.setString(6, taskData.getWorkingDirectory());
            statement.setInt(7, taskData.getTimeoutMinutes());
            statement.setInt(8, taskData.getMaxRetries());
            statement.setInt(9, taskData.getRetryCount());
            statement.setString(10, taskData.getStatus().name());
            statement.setTimestamp(11, taskData.getStartedAt() != null ? 
                Timestamp.from(taskData.getStartedAt()) : null);
            statement.setTimestamp(12, taskData.getCompletedAt() != null ? 
                Timestamp.from(taskData.getCompletedAt()) : null);
            statement.setString(13, taskData.getErrorMessage());
            statement.setInt(14, taskData.getExitCode());
            statement.setString(15, taskData.getOutput());
            
            statement.executeUpdate();
            logger.debug("Saved task data for task: {}", taskData.getTaskId());
            
        } catch (SQLException e) {
            logger.error("Failed to save task data for task: {}", taskData.getTaskId(), e);
            throw new RuntimeException("Failed to save task data", e);
        }
    }

    /**
     * Get process data by process ID
     */
    public ProcessData getProcessData(String processId) {
        String sql = """
            SELECT process_id, process_type, status, current_task_index, total_tasks,
                   started_at, completed_at, error_message, created_at, updated_at
            FROM processes 
            WHERE process_id = ?
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, processId);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    ProcessData processData = new ProcessData();
                    processData.setProcessId(resultSet.getString("process_id"));
                    processData.setProcessType(resultSet.getString("process_type"));
                    processData.setStatus(ProcessStatus.valueOf(resultSet.getString("status")));
                    processData.setCurrentTaskIndex(resultSet.getInt("current_task_index"));
                    processData.setTotalTasks(resultSet.getInt("total_tasks"));
                    
                    Timestamp startedAt = resultSet.getTimestamp("started_at");
                    if (startedAt != null) {
                        processData.setStartedAt(startedAt.toInstant());
                    }
                    
                    Timestamp completedAt = resultSet.getTimestamp("completed_at");
                    if (completedAt != null) {
                        processData.setCompletedAt(completedAt.toInstant());
                    }
                    
                    processData.setErrorMessage(resultSet.getString("error_message"));
                    
                    return processData;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to get process data for process: {}", processId, e);
            throw new RuntimeException("Failed to get process data", e);
        }
        
        return null;
    }

    /**
     * Get all tasks for a process
     */
    public List<TaskData> getProcessTasks(String processId) {
        String sql = """
            SELECT task_id, process_id, task_index, name, command, working_directory,
                   timeout_minutes, max_retries, retry_count, status, started_at,
                   completed_at, error_message, exit_code, output, created_at, updated_at
            FROM tasks 
            WHERE process_id = ?
            ORDER BY task_index
            """;

        List<TaskData> tasks = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, processId);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    TaskData taskData = new TaskData();
                    taskData.setTaskId(resultSet.getString("task_id"));
                    taskData.setProcessId(resultSet.getString("process_id"));
                    taskData.setName(resultSet.getString("name"));
                    taskData.setCommand(resultSet.getString("command"));
                    taskData.setWorkingDirectory(resultSet.getString("working_directory"));
                    taskData.setTimeoutMinutes(resultSet.getInt("timeout_minutes"));
                    taskData.setMaxRetries(resultSet.getInt("max_retries"));
                    taskData.setRetryCount(resultSet.getInt("retry_count"));
                    taskData.setStatus(TaskStatus.valueOf(resultSet.getString("status")));
                    
                    Timestamp startedAt = resultSet.getTimestamp("started_at");
                    if (startedAt != null) {
                        taskData.setStartedAt(startedAt.toInstant());
                    }
                    
                    Timestamp completedAt = resultSet.getTimestamp("completed_at");
                    if (completedAt != null) {
                        taskData.setCompletedAt(completedAt.toInstant());
                    }
                    
                    taskData.setErrorMessage(resultSet.getString("error_message"));
                    taskData.setExitCode(resultSet.getInt("exit_code"));
                    taskData.setOutput(resultSet.getString("output"));
                    
                    tasks.add(taskData);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to get tasks for process: {}", processId, e);
            throw new RuntimeException("Failed to get process tasks", e);
        }
        
        return tasks;
    }

    /**
     * Get all processes
     */
    public List<ProcessData> getAllProcesses() {
        String sql = """
            SELECT process_id, process_type, status, current_task_index, total_tasks,
                   started_at, completed_at, error_message, created_at, updated_at
            FROM processes 
            ORDER BY created_at DESC
            """;

        List<ProcessData> processes = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ProcessData processData = new ProcessData();
                    processData.setProcessId(resultSet.getString("process_id"));
                    processData.setProcessType(resultSet.getString("process_type"));
                    processData.setStatus(ProcessStatus.valueOf(resultSet.getString("status")));
                    processData.setCurrentTaskIndex(resultSet.getInt("current_task_index"));
                    processData.setTotalTasks(resultSet.getInt("total_tasks"));
                    
                    Timestamp startedAt = resultSet.getTimestamp("started_at");
                    if (startedAt != null) {
                        processData.setStartedAt(startedAt.toInstant());
                    }
                    
                    Timestamp completedAt = resultSet.getTimestamp("completed_at");
                    if (completedAt != null) {
                        processData.setCompletedAt(completedAt.toInstant());
                    }
                    
                    processData.setErrorMessage(resultSet.getString("error_message"));
                    
                    processes.add(processData);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to get all processes", e);
            throw new RuntimeException("Failed to get all processes", e);
        }
        
        return processes;
    }

    /**
     * Get processes by status
     */
    public List<ProcessData> getProcessesByStatus(ProcessStatus status) {
        String sql = """
            SELECT process_id, process_type, status, current_task_index, total_tasks,
                   started_at, completed_at, error_message, created_at, updated_at
            FROM processes 
            WHERE status = ?
            ORDER BY created_at DESC
            """;

        List<ProcessData> processes = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, status.name());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ProcessData processData = new ProcessData();
                    processData.setProcessId(resultSet.getString("process_id"));
                    processData.setProcessType(resultSet.getString("process_type"));
                    processData.setStatus(ProcessStatus.valueOf(resultSet.getString("status")));
                    processData.setCurrentTaskIndex(resultSet.getInt("current_task_index"));
                    processData.setTotalTasks(resultSet.getInt("total_tasks"));
                    
                    Timestamp startedAt = resultSet.getTimestamp("started_at");
                    if (startedAt != null) {
                        processData.setStartedAt(startedAt.toInstant());
                    }
                    
                    Timestamp completedAt = resultSet.getTimestamp("completed_at");
                    if (completedAt != null) {
                        processData.setCompletedAt(completedAt.toInstant());
                    }
                    
                    processData.setErrorMessage(resultSet.getString("error_message"));
                    
                    processes.add(processData);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to get processes by status: {}", status, e);
            throw new RuntimeException("Failed to get processes by status", e);
        }
        
        return processes;
    }

    /**
     * Extract task index from task ID (assumes format: processId-task-index)
     */
    private int extractTaskIndex(String taskId) {
        try {
            String[] parts = taskId.split("-");
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (Exception e) {
            logger.warn("Could not extract task index from task ID: {}", taskId);
            return 0;
        }
    }
}
