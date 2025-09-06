package com.processorchestrator.executor;

import com.processorchestrator.model.TaskData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CLITaskExecutorTest {

    @Test
    void testExecuteSuccess() {
        CLITaskExecutor executor = new CLITaskExecutor();
        
        // Use Windows-compatible command
        String command = System.getProperty("os.name").toLowerCase().contains("windows") 
            ? "cmd /c echo Hello World" 
            : "echo 'Hello World'";
        
        TaskData taskData = new TaskData("test-task", "test-process", "echo-test", 
                                       command, System.getProperty("java.io.tmpdir"), 1, 1);
        
        CLITaskExecutor.ExecutionResult result = executor.execute(taskData);
        
        assertTrue(result.isSuccess());
        assertEquals(0, result.getExitCode());
        assertTrue(result.getOutput().contains("Hello World"));
        assertNull(result.getErrorMessage());
    }

    @Test
    void testExecuteFailure() {
        CLITaskExecutor executor = new CLITaskExecutor();
        
        // Use Windows-compatible command that fails
        String command = System.getProperty("os.name").toLowerCase().contains("windows") 
            ? "cmd /c exit 1" 
            : "exit 1";
        
        TaskData taskData = new TaskData("test-task", "test-process", "fail-test", 
                                       command, System.getProperty("java.io.tmpdir"), 1, 1);
        
        CLITaskExecutor.ExecutionResult result = executor.execute(taskData);
        
        assertFalse(result.isSuccess());
        assertEquals(1, result.getExitCode());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void testExecuteWithCustomCommand() {
        CLITaskExecutor executor = new CLITaskExecutor();
        
        String originalCommand = System.getProperty("os.name").toLowerCase().contains("windows") 
            ? "cmd /c echo original" 
            : "echo 'original'";
        
        String customCommand = System.getProperty("os.name").toLowerCase().contains("windows") 
            ? "cmd /c echo custom command" 
            : "echo 'custom command'";
        
        TaskData taskData = new TaskData("test-task", "test-process", "custom-test", 
                                       originalCommand, System.getProperty("java.io.tmpdir"), 1, 1);
        
        // Execute with custom command
        CLITaskExecutor.ExecutionResult result = executor.execute(taskData, customCommand);
        
        assertTrue(result.isSuccess());
        assertEquals(0, result.getExitCode());
        assertTrue(result.getOutput().contains("custom command"));
    }

    @Test
    void testExecuteWithWorkingDirectory() {
        CLITaskExecutor executor = new CLITaskExecutor();
        
        String command = System.getProperty("os.name").toLowerCase().contains("windows") 
            ? "cmd /c cd" 
            : "pwd";
        
        TaskData taskData = new TaskData("test-task", "test-process", "pwd-test", 
                                       command, System.getProperty("user.home"), 1, 1);
        
        CLITaskExecutor.ExecutionResult result = executor.execute(taskData);
        
        assertTrue(result.isSuccess());
        assertEquals(0, result.getExitCode());
        // On Windows, cd command shows current directory, on Unix pwd shows current directory
        assertTrue(result.getOutput().contains(System.getProperty("user.home")) || 
                  result.getOutput().contains("C:"));
    }

    @Test
    void testExecuteWithTimeout() {
        CLITaskExecutor executor = new CLITaskExecutor();
        
        // Use a command that will definitely take longer than 1 second
        String command = System.getProperty("os.name").toLowerCase().contains("windows") 
            ? "cmd /c ping 127.0.0.1 -n 100" 
            : "sleep 100";
        
        TaskData taskData = new TaskData("test-task", "test-process", "sleep-test", 
                                       command, System.getProperty("java.io.tmpdir"), 1, 1); // 1 second timeout
        
        long startTime = System.currentTimeMillis();
        CLITaskExecutor.ExecutionResult result = executor.execute(taskData);
        long endTime = System.currentTimeMillis();
        
        // Should timeout and fail
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("timed out"));
        
        // Should not take more than 2 minutes (allowing some buffer)
        assertTrue(endTime - startTime < 120000);
    }

    @Test
    void testExecuteInvalidCommand() {
        CLITaskExecutor executor = new CLITaskExecutor();
        
        TaskData taskData = new TaskData("test-task", "test-process", "invalid-test", 
                                       "nonexistentcommand12345", System.getProperty("java.io.tmpdir"), 1, 1);
        
        CLITaskExecutor.ExecutionResult result = executor.execute(taskData);
        
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }
}