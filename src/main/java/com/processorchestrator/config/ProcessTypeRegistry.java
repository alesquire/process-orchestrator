package com.processorchestrator.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for process types
 */
public class ProcessTypeRegistry {
    private final Map<String, ProcessType> processTypes = new HashMap<>();

    public void register(ProcessType processType) {
        processTypes.put(processType.getName(), processType);
    }

    public ProcessType getProcessType(String name) {
        return processTypes.get(name);
    }

    public ProcessType getProcessTypeOrThrow(String name) {
        ProcessType processType = processTypes.get(name);
        if (processType == null) {
            throw new IllegalArgumentException("Process type not found: " + name);
        }
        return processType;
    }

    public Map<String, ProcessType> getAllProcessTypes() {
        return new HashMap<>(processTypes);
    }
}