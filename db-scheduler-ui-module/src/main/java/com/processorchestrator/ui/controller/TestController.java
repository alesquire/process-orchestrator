package com.processorchestrator.ui.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple test controller to verify the application is working.
 */
@RestController
public class TestController {

    @GetMapping("/test")
    public String test() {
        return "Application is running!";
    }
}
