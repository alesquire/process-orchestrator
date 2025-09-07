package com.processorchestrator.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application for the DB Scheduler UI module.
 * This provides a web-based dashboard for monitoring and managing db-scheduler tasks.
 */
@SpringBootApplication
public class DbSchedulerUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbSchedulerUiApplication.class, args);
    }
}
