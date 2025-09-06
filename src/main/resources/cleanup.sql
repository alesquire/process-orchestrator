-- Cleanup script for Process Orchestrator
-- Removes test data and resets database state

-- Delete from child tables first due to foreign key constraints
DELETE FROM tasks WHERE process_record_id LIKE 'test-%' 
   OR process_record_id LIKE 'all-%' 
   OR process_record_id LIKE 'status-test-%' 
   OR process_record_id LIKE 'db-test-%' 
   OR process_record_id LIKE 'crud-test-%'
   OR process_record_id LIKE 'multi-test-%'
   OR process_record_id LIKE 'history-test-%'
   OR process_record_id LIKE 'restart-test-%'
   OR process_record_id LIKE 'execution-test-%'
   OR process_record_id LIKE 'scheduled-%'
   OR process_record_id LIKE 'duplicate-test-%'
   OR process_record_id LIKE 'error-test-%'
   OR process_record_id LIKE 'example-%'
   OR process_record_id LIKE 'single-task-test-%'
   OR process_record_id LIKE 'dual-task-test-%'
   OR process_record_id LIKE 'ping-test-%'
   OR process_record_id LIKE 'concurrent-test-%'
   OR process_record_id = 'running-record';

-- Delete from process_record table
DELETE FROM process_record WHERE id LIKE 'test-%' 
   OR id LIKE 'all-%' 
   OR id LIKE 'status-test-%' 
   OR id LIKE 'db-test-%' 
   OR id LIKE 'crud-test-%'
   OR id LIKE 'multi-test-%'
   OR id LIKE 'history-test-%'
   OR id LIKE 'restart-test-%'
   OR id LIKE 'execution-test-%'
   OR id LIKE 'scheduled-%'
   OR id LIKE 'duplicate-test-%'
   OR id LIKE 'error-test-%'
   OR id LIKE 'example-%'
   OR id LIKE 'single-task-test-%'
   OR id LIKE 'dual-task-test-%'
   OR id LIKE 'ping-test-%'
   OR id LIKE 'concurrent-test-%'
   OR id = 'running-record';

-- Clean up db-scheduler tasks
DELETE FROM scheduled_tasks WHERE task_name LIKE 'process-orchestrator-task-%'
   OR task_name = 'cli-execution'
   OR task_name = 'process-execution';
