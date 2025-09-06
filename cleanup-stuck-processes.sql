-- Script to clean up stuck processes and tasks
-- Run this when processes are stuck in IN_PROGRESS state

-- 1. Check current stuck processes
SELECT 'Current stuck processes:' as info;
SELECT id, type, current_status, triggered_by, total_tasks, current_task_index 
FROM process_record 
WHERE current_status = 'IN_PROGRESS' 
ORDER BY created_at DESC;

-- 2. Check scheduled tasks that are not picked up
SELECT 'Scheduled tasks not picked up:' as info;
SELECT task_name, task_instance, execution_time, picked, picked_by, consecutive_failures 
FROM scheduled_tasks 
WHERE picked = false 
ORDER BY execution_time DESC;

-- 3. Clean up stuck processes (uncomment to execute)
-- DELETE FROM scheduled_tasks WHERE picked = false;
-- DELETE FROM tasks WHERE process_record_id IN (
--     SELECT id FROM process_record WHERE current_status = 'IN_PROGRESS'
-- );
-- DELETE FROM process_record WHERE current_status = 'IN_PROGRESS';

-- 4. Verify cleanup
SELECT 'After cleanup - scheduled tasks count:' as info;
SELECT COUNT(*) as scheduled_tasks_count FROM scheduled_tasks;

SELECT 'After cleanup - process records count:' as info;
SELECT COUNT(*) as process_records_count FROM process_record WHERE current_status = 'IN_PROGRESS';

SELECT 'After cleanup - tasks count:' as info;
SELECT COUNT(*) as tasks_count FROM tasks;
