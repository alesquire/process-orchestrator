-- Database schema for Process Orchestrator
-- This script creates the necessary tables for process management

-- Process Definitions table - stores process templates and execution history (user-managed)
CREATE TABLE IF NOT EXISTS process_definitions (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    input_data TEXT NOT NULL,
    schedule VARCHAR(255), -- Cron expression, NULL for manual execution only
    
    -- Current execution status (updated by engine)
    current_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    current_process_id VARCHAR(255), -- Reference to active process in processes table
    
    -- Execution statistics (updated by engine)
    started_when TIMESTAMP,
    completed_when TIMESTAMP,
    failed_when TIMESTAMP,
    stopped_when TIMESTAMP,
    last_error_message TEXT,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes for performance
    INDEX idx_process_definitions_status (current_status),
    INDEX idx_process_definitions_type (type),
    INDEX idx_process_definitions_schedule (schedule),
    INDEX idx_process_definitions_created_at (created_at)
);

-- Processes table - stores active process execution state (engine-managed)
CREATE TABLE IF NOT EXISTS processes (
    id VARCHAR(255) PRIMARY KEY,
    definition_id VARCHAR(255) NOT NULL, -- Reference to process_definitions
    type VARCHAR(255) NOT NULL,
    input_data TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    current_task_index INTEGER NOT NULL DEFAULT 0,
    total_tasks INTEGER NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    FOREIGN KEY (definition_id) REFERENCES process_definitions(id) ON DELETE CASCADE,
    
    -- Indexes for performance
    INDEX idx_processes_status (status),
    INDEX idx_processes_type (type),
    INDEX idx_processes_definition_id (definition_id),
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
