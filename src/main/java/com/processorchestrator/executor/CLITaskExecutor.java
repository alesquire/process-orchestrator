package com.processorchestrator.executor;

import com.processorchestrator.model.TaskData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Executes CLI commands with timeout and retry support
 */
public class CLITaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(CLITaskExecutor.class);

    public ExecutionResult execute(TaskData taskData) {
        return execute(taskData, taskData.getCommand());
    }
    
    public ExecutionResult execute(TaskData taskData, String command) {
        logger.info("Executing CLI task: {} - {}", taskData.getName(), command);
        
        ProcessBuilder processBuilder = new ProcessBuilder();
        
        // Set command - split by spaces for proper execution
        String[] commandParts = command.split("\\s+");
        processBuilder.command(commandParts);
        
        // Set working directory if specified
        if (taskData.getWorkingDirectory() != null && !taskData.getWorkingDirectory().trim().isEmpty()) {
            processBuilder.directory(new java.io.File(taskData.getWorkingDirectory()));
        }
        
        // Redirect error stream to output stream
        processBuilder.redirectErrorStream(true);
        
        try {
            Process process = processBuilder.start();
            
            // Set timeout
            boolean finished = process.waitFor(taskData.getTimeoutMinutes(), TimeUnit.MINUTES);
            
            if (!finished) {
                process.destroyForcibly();
                return ExecutionResult.failure("Task timed out after " + taskData.getTimeoutMinutes() + " minutes");
            }
            
            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.exitValue();
            String outputStr = output.toString();
            
            if (exitCode == 0) {
                logger.info("Task {} completed successfully with exit code {}", taskData.getName(), exitCode);
                return ExecutionResult.success(exitCode, outputStr);
            } else {
                logger.warn("Task {} failed with exit code {}", taskData.getName(), exitCode);
                return ExecutionResult.failure(exitCode, "Task failed with exit code " + exitCode + "\nOutput: " + outputStr);
            }
            
        } catch (IOException e) {
            logger.error("IO error executing task: {}", taskData.getName(), e);
            return ExecutionResult.failure("IO error: " + e.getMessage());
        } catch (InterruptedException e) {
            logger.error("Task execution interrupted: {}", taskData.getName(), e);
            Thread.currentThread().interrupt();
            return ExecutionResult.failure("Task execution interrupted: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error executing task: {}", taskData.getName(), e);
            return ExecutionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public static class ExecutionResult {
        private final boolean success;
        private final int exitCode;
        private final String output;
        private final String errorMessage;

        private ExecutionResult(boolean success, int exitCode, String output, String errorMessage) {
            this.success = success;
            this.exitCode = exitCode;
            this.output = output;
            this.errorMessage = errorMessage;
        }

        public static ExecutionResult success(int exitCode, String output) {
            return new ExecutionResult(true, exitCode, output, null);
        }

        public static ExecutionResult failure(String errorMessage) {
            return new ExecutionResult(false, -1, null, errorMessage);
        }

        public static ExecutionResult failure(int exitCode, String errorMessage) {
            return new ExecutionResult(false, exitCode, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public int getExitCode() { return exitCode; }
        public String getOutput() { return output; }
        public String getErrorMessage() { return errorMessage; }
    }
}