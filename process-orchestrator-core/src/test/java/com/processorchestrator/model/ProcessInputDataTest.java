package com.processorchestrator.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

class ProcessInputDataTest {

    @Test
    void testDefaultConstructor() {
        ProcessInputData inputData = new ProcessInputData();
        
        assertNull(inputData.getInputFile());
        assertNull(inputData.getOutputDir());
        assertNull(inputData.getUserId());
        assertNotNull(inputData.getConfig());
        assertNotNull(inputData.getMetadata());
        assertTrue(inputData.getConfig().isEmpty());
        assertTrue(inputData.getMetadata().isEmpty());
    }

    @Test
    void testConstructorWithFileAndDir() {
        ProcessInputData inputData = new ProcessInputData("/data/input.csv", "/data/output");
        
        assertEquals("/data/input.csv", inputData.getInputFile());
        assertEquals("/data/output", inputData.getOutputDir());
        assertNotNull(inputData.getConfig());
        assertNotNull(inputData.getMetadata());
    }

    @Test
    void testConstructorWithFileDirAndUser() {
        ProcessInputData inputData = new ProcessInputData("/data/input.csv", "/data/output", "user123");
        
        assertEquals("/data/input.csv", inputData.getInputFile());
        assertEquals("/data/output", inputData.getOutputDir());
        assertEquals("user123", inputData.getUserId());
    }

    @Test
    void testAddConfig() {
        ProcessInputData inputData = new ProcessInputData();
        
        inputData.addConfig("batch_size", "1000");
        inputData.addConfig("format", "json");
        
        assertEquals("1000", inputData.getConfig().get("batch_size"));
        assertEquals("json", inputData.getConfig().get("format"));
        assertEquals(2, inputData.getConfig().size());
    }

    @Test
    void testAddMetadata() {
        ProcessInputData inputData = new ProcessInputData();
        
        inputData.addMetadata("priority", "high");
        inputData.addMetadata("retry_count", 3);
        
        assertEquals("high", inputData.getMetadata().get("priority"));
        assertEquals(3, inputData.getMetadata().get("retry_count"));
        assertEquals(2, inputData.getMetadata().size());
    }
}