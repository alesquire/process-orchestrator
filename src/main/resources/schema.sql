-- Process Orchestrator Database Schema
-- PostgreSQL - Simplified 3-table schema

-- db-scheduler's scheduled_tasks table (required by db-scheduler)
CREATE TABLE IF NOT EXISTS scheduled_tasks (
    task_name VARCHAR(255) PRIMARY KEY,
    task_instance VARCHAR(255) PRIMARY KEY,
    task_data BYTEA,
    execution_time TIMESTAMP WITH TIME ZONE NOT NULL,
    picked BOOLEAN NOT NULL DEFAULT FALSE,
    picked_by VARCHAR(50),
    last_success TIMESTAMP WITH TIME ZONE,
    last_failure TIMESTAMP WITH TIME ZONE,
    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    last_heartbeat TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (task_name, task_instance)
);

-- Process record table (user-managed process templates)
CREATE TABLE IF NOT EXISTS process_record (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(100) NOT NULL,
    input_data TEXT,
    schedule VARCHAR(100),
    -- Engine-managed fields (immutable for user)
    current_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    current_task_index INTEGER DEFAULT 0,
    total_tasks INTEGER DEFAULT 0,
    started_when TIMESTAMP,
    completed_when TIMESTAMP,
    failed_when TIMESTAMP,
    stopped_when TIMESTAMP,
    last_error_message TEXT,
    triggered_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tasks table (individual task execution records)
CREATE TABLE IF NOT EXISTS tasks (
    id VARCHAR(255) PRIMARY KEY,
    process_record_id VARCHAR(255) NOT NULL REFERENCES process_record(id) ON DELETE CASCADE,
    task_index INTEGER NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    command TEXT NOT NULL,
    working_directory VARCHAR(500),
    timeout_minutes INTEGER DEFAULT 60,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    exit_code INTEGER,
    output TEXT,
    metadata JSONB
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_process_record_status ON process_record(current_status);
CREATE INDEX IF NOT EXISTS idx_process_record_type ON process_record(type);
CREATE INDEX IF NOT EXISTS idx_tasks_process_record_id ON tasks(process_record_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_process_index ON tasks(process_record_id, task_index);

-- Update trigger for process_record table
CREATE OR REPLACE FUNCTION update_process_record_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Drop existing triggers first to avoid conflicts
DROP TRIGGER IF EXISTS update_process_record_updated_at ON process_record;
CREATE TRIGGER update_process_record_updated_at BEFORE UPDATE ON process_record
    FOR EACH ROW EXECUTE FUNCTION update_process_record_updated_at();

-- Update trigger for tasks table
CREATE OR REPLACE FUNCTION update_tasks_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Drop existing triggers first to avoid conflicts
DROP TRIGGER IF EXISTS update_tasks_updated_at ON tasks;
CREATE TRIGGER update_tasks_updated_at BEFORE UPDATE ON tasks
    FOR EACH ROW EXECUTE FUNCTION update_tasks_updated_at();