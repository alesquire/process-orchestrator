package com.processorchestrator.ui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web controller for serving the Process UI dashboard page.
 */
@Controller
public class ProcessDashboardController {

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("title", "Process Orchestrator Dashboard");
        return "dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboardPage(Model model) {
        model.addAttribute("title", "Process Orchestrator Dashboard");
        return "dashboard";
    }
}
