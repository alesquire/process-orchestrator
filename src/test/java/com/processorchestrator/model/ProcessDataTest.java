package com.processorchestrator.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

class ProcessDataTest {

    @Test
    void testConstructor() {
        ProcessInputData inputData = new ProcessInputData("/data/input.csv", "/data/output");
        ProcessData processData = new ProcessData("process-123", "data-processing", 3, inputData);
        
        assertEquals("process-123", processData.getProcessId());
        assertEquals("data-processing", processData.getProcessType());
        assertEquals(ProcessStatus.NOT_STARTED, processData.getStatus());
        assertEquals(0, processData.getCurrentTaskIndex());
        assertEquals(3, processData.getTotalTasks());
        assertNotNull(processData.getInputData());
        assertNotNull(processData.getProcessContext());
    }

    @Test
    void testStatusMethods() {
        ProcessInputData inputData = new ProcessInputData("/data/input.csv", "/data/output");
        ProcessData processData = new ProcessData("process-123", "data-processing", 3, inputData);
        
        assertFalse(processData.isCompleted());
        assertFalse(processData.isFailed());
        
        processData.setStatus(ProcessStatus.COMPLETED);
        assertTrue(processData.isCompleted());
        assertFalse(processData.isFailed());
        
        processData.setStatus(ProcessStatus.FAILED);
        assertFalse(processData.isCompleted());
        assertTrue(processData.isFailed());
    }

    @Test
    void testTaskManagement() {
        ProcessInputData inputData = new ProcessInputData("/data/input.csv", "/data/output");
        ProcessData processData = new ProcessData("process-123", "data-processing", 3, inputData);
        
        // Create mock tasks
        List<TaskData> tasks = new ArrayList<>();
        tasks.add(new TaskData("task-1", "process-123", "validate", "python validate.py", "/data", 30, 2));
        tasks.add(new TaskData("task-2", "process-123", "transform", "python transform.py", "/data", 60, 3));
        tasks.add(new TaskData("task-3", "process-123", "load", "python load.py", "/data", 45, 2));
        
        processData.setTasks(tasks);
        
        assertTrue(processData.hasMoreTasks());
        assertEquals("validate", processData.getCurrentTask().getName());
        
        processData.moveToNextTask();
        assertEquals(1, processData.getCurrentTaskIndex());
        assertEquals("transform", processData.getCurrentTask().getName());
        assertTrue(processData.hasMoreTasks());
        
        processData.moveToNextTask();
        assertEquals(2, processData.getCurrentTaskIndex());
        assertEquals("load", processData.getCurrentTask().getName());
        assertTrue(processData.hasMoreTasks());
        
        processData.moveToNextTask();
        assertEquals(3, processData.getCurrentTaskIndex());
        assertFalse(processData.hasMoreTasks());
        assertEquals(ProcessStatus.COMPLETED, processData.getStatus());
        assertNotNull(processData.getCompletedAt());
    }

    @Test
    void testMarkAsFailed() {
        ProcessInputData inputData = new ProcessInputData("/data/input.csv", "/data/output");
        ProcessData processData = new ProcessData("process-123", "data-processing", 3, inputData);
        
        processData.markAsFailed("Task validation failed");
        
        assertEquals(ProcessStatus.FAILED, processData.getStatus());
        assertEquals("Task validation failed", processData.getErrorMessage());
        assertNotNull(processData.getCompletedAt());
    }

    @Test
    void testMarkAsStarted() {
        ProcessInputData inputData = new ProcessInputData("/data/input.csv", "/data/output");
        ProcessData processData = new ProcessData("process-123", "data-processing", 3, inputData);
        
        processData.markAsStarted();
        
        assertEquals(ProcessStatus.IN_PROGRESS, processData.getStatus());
        assertNotNull(processData.getStartedAt());
    }

    @Test
    void testContextManagement() {
        ProcessInputData inputData = new ProcessInputData("/data/input.csv", "/data/output");
        ProcessData processData = new ProcessData("process-123", "data-processing", 3, inputData);
        
        processData.addContext("batch_size", 1000);
        processData.addContext("last_task", "validate");
        
        assertEquals(1000, processData.getContext("batch_size"));
        assertEquals("validate", processData.getContext("last_task"));
        assertNull(processData.getContext("non_existent"));
    }
}