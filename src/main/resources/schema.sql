-- Process Orchestrator Database Schema
-- PostgreSQL

-- Process table
CREATE TABLE IF NOT EXISTS process (
    id VARCHAR(255) PRIMARY KEY,
    process_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    current_task_index INTEGER DEFAULT 0,
    total_tasks INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    metadata JSONB
);

-- Task table (extends db-scheduler's scheduled_tasks)
CREATE TABLE IF NOT EXISTS task (
    id VARCHAR(255) PRIMARY KEY,
    process_id VARCHAR(255) NOT NULL REFERENCES process(id) ON DELETE CASCADE,
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
CREATE INDEX IF NOT EXISTS idx_process_status ON process(status);
CREATE INDEX IF NOT EXISTS idx_process_type ON process(process_type);
CREATE INDEX IF NOT EXISTS idx_task_process_id ON task(process_id);
CREATE INDEX IF NOT EXISTS idx_task_status ON task(status);
CREATE INDEX IF NOT EXISTS idx_task_process_index ON task(process_id, task_index);

-- Update trigger for process table
CREATE OR REPLACE FUNCTION update_process_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_process_updated_at BEFORE UPDATE ON process
    FOR EACH ROW EXECUTE FUNCTION update_process_updated_at();

-- Update trigger for task table
CREATE OR REPLACE FUNCTION update_task_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_task_updated_at BEFORE UPDATE ON task
    FOR EACH ROW EXECUTE FUNCTION update_task_updated_at();


