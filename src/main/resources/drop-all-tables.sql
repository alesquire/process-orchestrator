-- Drop all tables script for Process Orchestrator
-- This script drops all tables in the correct order to handle foreign key constraints

-- Drop tables in reverse dependency order
DROP TABLE IF EXISTS tasks CASCADE;
DROP TABLE IF EXISTS process_executions CASCADE;
DROP TABLE IF EXISTS processes CASCADE;
DROP TABLE IF EXISTS process_record CASCADE;
DROP TABLE IF EXISTS scheduled_tasks CASCADE;
