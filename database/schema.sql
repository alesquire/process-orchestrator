-- Database schema for Process Orchestrator
-- This script creates the necessary tables for process management

-- Processes table - stores process definitions and execution state
CREATE TABLE IF NOT EXISTS processes (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    input_data TEXT NOT NULL,
    schedule VARCHAR(255), -- Cron expression, NULL for manual execution only
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    current_task_index INTEGER NOT NULL DEFAULT 0,
    total_tasks INTEGER NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes for performance
    INDEX idx_processes_status (status),
    INDEX idx_processes_type (type),
    INDEX idx_processes_schedule (schedule),
    INDEX idx_processes_created_at (created_at)
);

-- Tasks table - stores individual task execution details
CREATE TABLE IF NOT EXISTS tasks (
    task_id VARCHAR(255) PRIMARY KEY,
    process_id VARCHAR(255) NOT NULL,
    task_index INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    command TEXT NOT NULL,
    working_directory VARCHAR(500),
    timeout_minutes INTEGER NOT NULL DEFAULT 60,
    max_retries INTEGER NOT NULL DEFAULT 3,
    retry_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    exit_code INTEGER,
    output TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (process_id) REFERENCES processes(id) ON DELETE CASCADE,
    
    -- Indexes for performance
    INDEX idx_tasks_process_id (process_id),
    INDEX idx_tasks_status (status),
    INDEX idx_tasks_task_index (task_index)
);

-- Process execution history - keeps track of all execution attempts
CREATE TABLE IF NOT EXISTS process_executions (
    execution_id VARCHAR(255) PRIMARY KEY,
    process_id VARCHAR(255) NOT NULL,
    execution_started_at TIMESTAMP NOT NULL,
    execution_completed_at TIMESTAMP,
    execution_status VARCHAR(50) NOT NULL,
    triggered_by VARCHAR(50) NOT NULL, -- 'MANUAL', 'SCHEDULED', 'API'
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (process_id) REFERENCES processes(id) ON DELETE CASCADE,
    
    -- Indexes for performance
    INDEX idx_executions_process_id (process_id),
    INDEX idx_executions_status (execution_status),
    INDEX idx_executions_started_at (execution_started_at)
);
