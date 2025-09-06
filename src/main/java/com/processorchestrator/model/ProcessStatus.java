package com.processorchestrator.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Process status enumeration
 */
public enum ProcessStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}