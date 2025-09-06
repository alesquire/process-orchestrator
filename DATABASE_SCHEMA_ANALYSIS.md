# Process Orchestrator Database Schema Analysis

## Table Purposes and Analysis

### 1. **`scheduled_tasks`** ✅ **REQUIRED**
**Purpose**: Core db-scheduler table for task scheduling and execution
**Role**: 
- Stores individual tasks that db-scheduler manages
- Handles task scheduling, execution, and retry logic
- Tracks task state, failures, and heartbeats
- **Cannot be removed** - required by db-scheduler library

**Key Fields**:
- `task_name`, `task_instance` - Unique task identification
- `execution_time` - When task should run
- `picked`, `picked_by` - Cluster coordination
- `consecutive_failures` - Retry tracking

---

### 2. **`process_record`** ✅ **USER-REQUESTED**
**Purpose**: Process templates and execution history (user-managed)
**Role**:
- Stores process definitions created by users
- Contains process type, input data, and schedule
- Tracks execution statistics and current status
- **Primary user interface** for process management

**Key Fields**:
- `id`, `type`, `input_data`, `schedule` - User-defined process template
- `current_status`, `current_process_id` - Current execution state
- `started_when`, `completed_when`, etc. - Execution history

---

### 3. **`processes`** ⚠️ **POTENTIALLY REDUNDANT**
**Purpose**: Active process execution state (engine-managed)
**Role**:
- Stores runtime state of executing processes
- Tracks current task index and progress
- **Potential Issue**: Duplicates information from `process_record`

**Analysis**:
- **Redundant Fields**: `type`, `input_data` are duplicated from `process_record`
- **Unique Value**: `current_task_index`, `total_tasks` for runtime state
- **Recommendation**: Could be simplified or merged

---

### 4. **`tasks`** ✅ **USEFUL**
**Purpose**: Individual task execution details
**Role**:
- Stores detailed execution information for each task
- Tracks command, output, exit codes, retries
- Provides audit trail and debugging information

**Key Fields**:
- `command`, `working_directory` - Task execution details
- `output`, `exit_code` - Execution results
- `retry_count`, `error_message` - Failure handling

---

### 5. **`process_executions`** ⚠️ **POTENTIALLY REDUNDANT**
**Purpose**: Process execution history
**Role**:
- Tracks all execution attempts of processes
- Records who triggered the execution (manual/scheduled/API)
- **Potential Issue**: Overlaps with `process_record` execution history

**Analysis**:
- **Redundant Fields**: `execution_started_at`, `execution_completed_at`, `execution_status` overlap with `process_record`
- **Unique Value**: `triggered_by` field for audit purposes
- **Recommendation**: Could be simplified or merged

---

## Redundancy Analysis

### **High Redundancy** 🔴
1. **`processes` vs `process_record`**:
   - Both store process status and execution times
   - `processes` duplicates `type` and `input_data` from `process_record`
   - Only `current_task_index` and `total_tasks` are truly unique

2. **`process_executions` vs `process_record`**:
   - Both track execution history
   - `process_executions` duplicates timing and status information
   - Only `triggered_by` provides unique value

### **Low Redundancy** 🟢
1. **`tasks` table**: Provides unique detailed execution information
2. **`scheduled_tasks`**: Required by db-scheduler, cannot be changed

---

## Recommended Simplification

### **Option 1: Minimal Schema** (Recommended)
Keep only essential tables:
- `scheduled_tasks` (required by db-scheduler)
- `process_record` (user interface)
- `tasks` (detailed execution tracking)

**Remove**:
- `processes` - merge `current_task_index` and `total_tasks` into `process_record`
- `process_executions` - merge `triggered_by` into `process_record`

### **Option 2: Current Schema** (If you prefer separation)
Keep current schema but understand the trade-offs:
- **Pros**: Clear separation of concerns
- **Cons**: Data duplication and complexity

---

## Data Flow Example

```
User creates process_record → Engine creates processes → Engine creates tasks → Tasks stored in scheduled_tasks
```

**Simplified Flow**:
```
User creates process_record → Engine creates tasks → Tasks stored in scheduled_tasks
```

---

## Questions for Decision

1. **Do you need detailed execution history per process run?**
   - If yes: Keep `process_executions`
   - If no: Remove it

2. **Do you need runtime process state tracking?**
   - If yes: Keep `processes` but remove duplicate fields
   - If no: Merge into `process_record`

3. **Do you need audit trail of who triggered executions?**
   - If yes: Keep `triggered_by` field somewhere
   - If no: Remove `process_executions`

**Recommendation**: Start with Option 1 (minimal schema) and add complexity only if needed.
