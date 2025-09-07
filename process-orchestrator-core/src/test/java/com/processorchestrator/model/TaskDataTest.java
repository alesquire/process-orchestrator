package com.processorchestrator.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

class TaskDataTest {

    @Test
    void testConstructor() {
        TaskData taskData = new TaskData("task-123", "process-456", "validate", 
                                       "python validate.py", "/data", 30, 2);
        
        assertEquals("task-123", taskData.getTaskId());
        assertEquals("process-456", taskData.getProcessId());
        assertEquals("validate", taskData.getName());
        assertEquals("python validate.py", taskData.getCommand());
        assertEquals("/data", taskData.getWorkingDirectory());
        assertEquals(30, taskData.getTimeoutMinutes());
        assertEquals(2, taskData.getMaxRetries());
        assertEquals("PENDING", taskData.getStatus());
        assertEquals(0, taskData.getRetryCount());
    }

    @Test
    void testRetryLogic() {
        TaskData taskData = new TaskData("task-123", "process-456", "validate", 
                                       "python validate.py", "/data", 30, 2);
        
        assertTrue(taskData.canRetry());
        assertEquals(0, taskData.getRetryCount());
        
        taskData.incrementRetryCount();
        assertEquals(1, taskData.getRetryCount());
        assertTrue(taskData.canRetry());
        
        taskData.incrementRetryCount();
        assertEquals(2, taskData.getRetryCount());
        assertFalse(taskData.canRetry());
    }

    @Test
    void testMarkAsCompleted() {
        TaskData taskData = new TaskData("task-123", "process-456", "validate", 
                                       "python validate.py", "/data", 30, 2);
        
        taskData.markAsCompleted(0, "Validation completed successfully");
        
        assertEquals("COMPLETED", taskData.getStatus());
        assertEquals(0, taskData.getExitCode());
        assertEquals("Validation completed successfully", taskData.getOutput());
        assertNotNull(taskData.getCompletedAt());
    }

    @Test
    void testMarkAsFailed() {
        TaskData taskData = new TaskData("task-123", "process-456", "validate", 
                                       "python validate.py", "/data", 30, 2);
        
        taskData.markAsFailed("Validation failed: invalid format");
        
        assertEquals("FAILED", taskData.getStatus());
        assertEquals("Validation failed: invalid format", taskData.getErrorMessage());
        assertNotNull(taskData.getCompletedAt());
    }

    @Test
    void testMarkAsStarted() {
        TaskData taskData = new TaskData("task-123", "process-456", "validate", 
                                       "python validate.py", "/data", 30, 2);
        
        taskData.markAsStarted();
        
        assertEquals("IN_PROGRESS", taskData.getStatus());
        assertNotNull(taskData.getStartedAt());
    }
}