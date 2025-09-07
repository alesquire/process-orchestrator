package com.processorchestrator.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProcessTypeTest {

    @Test
    void testConstructor() {
        ProcessType processType = new ProcessType("data-processing", "Data processing pipeline");
        
        assertEquals("data-processing", processType.getName());
        assertEquals("Data processing pipeline", processType.getDescription());
        assertNotNull(processType.getTasks());
        assertEquals(0, processType.getTaskCount());
    }

    @Test
    void testAddTaskWithDefaults() {
        ProcessType processType = new ProcessType("data-processing", "Data processing pipeline");
        
        processType.addTask("validate", "python validate.py");
        
        assertEquals(1, processType.getTaskCount());
        TaskDefinition task = processType.getTask(0);
        assertEquals("validate", task.getName());
        assertEquals("python validate.py", task.getCommand());
        assertEquals(60, task.getTimeoutMinutes()); // default
        assertEquals(3, task.getMaxRetries()); // default
    }

    @Test
    void testAddTaskWithFullParams() {
        ProcessType processType = new ProcessType("data-processing", "Data processing pipeline");
        
        processType.addTask("validate", "python validate.py", "/data", 30, 2);
        
        assertEquals(1, processType.getTaskCount());
        TaskDefinition task = processType.getTask(0);
        assertEquals("validate", task.getName());
        assertEquals("python validate.py", task.getCommand());
        assertEquals("/data", task.getWorkingDirectory());
        assertEquals(30, task.getTimeoutMinutes());
        assertEquals(2, task.getMaxRetries());
    }

    @Test
    void testMultipleTasks() {
        ProcessType processType = new ProcessType("data-processing", "Data processing pipeline");
        
        processType.addTask("validate", "python validate.py", "/data", 30, 2)
                  .addTask("transform", "python transform.py", "/data", 60, 3)
                  .addTask("load", "python load.py", "/data", 45, 2);
        
        assertEquals(3, processType.getTaskCount());
        
        assertEquals("validate", processType.getTask(0).getName());
        assertEquals("transform", processType.getTask(1).getName());
        assertEquals("load", processType.getTask(2).getName());
        
        assertNull(processType.getTask(3)); // out of bounds
    }

    @Test
    void testFluentInterface() {
        ProcessType processType = new ProcessType("data-processing", "Data processing pipeline");
        
        ProcessType result = processType.addTask("validate", "python validate.py")
                                       .addTask("transform", "python transform.py");
        
        assertSame(processType, result); // Should return same instance
        assertEquals(2, processType.getTaskCount());
    }
}