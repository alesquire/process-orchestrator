package com.processorchestrator.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application for the Process UI module.
 * This provides a custom web-based dashboard for visualizing processes and tasks
 * in a tabular format with real-time status updates.
 */
@SpringBootApplication(scanBasePackages = {"com.processorchestrator.ui", "com.processorchestrator"})
public class ProcessUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcessUiApplication.class, args);
    }
}
