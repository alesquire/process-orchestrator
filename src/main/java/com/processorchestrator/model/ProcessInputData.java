package com.processorchestrator.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Input data for a process - contains the context and configuration
 */
public class ProcessInputData implements Serializable {
    private String inputFile;
    private String outputDir;
    private Map<String, String> config;
    private String userId;
    private Map<String, Object> metadata;
    
    public ProcessInputData() {
        this.config = new HashMap<>();
        this.metadata = new HashMap<>();
    }
    
    public ProcessInputData(String inputFile, String outputDir) {
        this();
        this.inputFile = inputFile;
        this.outputDir = outputDir;
    }
    
    public ProcessInputData(String inputFile, String outputDir, String userId) {
        this(inputFile, outputDir);
        this.userId = userId;
    }
    
    // Getters and Setters
    public String getInputFile() { return inputFile; }
    public void setInputFile(String inputFile) { this.inputFile = inputFile; }
    
    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
    
    public Map<String, String> getConfig() { return config; }
    public void setConfig(Map<String, String> config) { this.config = config; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public void addConfig(String key, String value) {
        this.config.put(key, value);
    }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
}
