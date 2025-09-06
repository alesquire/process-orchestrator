package com.processorchestrator.model;

import java.io.Serializable;
import java.time.Instant;

/**
 * Task status enumeration
 */
public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}