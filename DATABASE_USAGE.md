# Database-Driven Process Orchestrator

This document explains how to use the Process Orchestrator with a database-driven approach, where processes are stored in database tables and managed through API calls.

## Database Schema

The Process Orchestrator uses three main tables:

### 1. `processes` Table
Stores process definitions and execution state:

| Column | Type | Description |
|--------|------|-------------|
| `id` | VARCHAR(255) | Unique process identifier (PRIMARY KEY) |
| `type` | VARCHAR(255) | Process type that defines the task structure |
| `input_data` | TEXT | Input data passed to the process and tasks |
| `schedule` | VARCHAR(255) | Cron expression for scheduled execution (NULL for manual only) |
| `status` | VARCHAR(50) | Current process status (PENDING, IN_PROGRESS, COMPLETED, FAILED, STOPPED) |
| `current_task_index` | INTEGER | Index of currently executing task |
| `total_tasks` | INTEGER | Total number of tasks in the process |
| `started_at` | TIMESTAMP | When the process started |
| `completed_at` | TIMESTAMP | When the process completed |
| `error_message` | TEXT | Error message if process failed |
| `created_at` | TIMESTAMP | When the process was created |
| `updated_at` | TIMESTAMP | When the process was last updated |

### 2. `tasks` Table
Stores individual task execution details:

| Column | Type | Description |
|--------|------|-------------|
| `task_id` | VARCHAR(255) | Unique task identifier (PRIMARY KEY) |
| `process_id` | VARCHAR(255) | Reference to parent process |
| `task_index` | INTEGER | Order of task in the process |
| `name` | VARCHAR(255) | Task name |
| `command` | TEXT | Command to execute |
| `working_directory` | VARCHAR(500) | Working directory for command |
| `timeout_minutes` | INTEGER | Task timeout in minutes |
| `max_retries` | INTEGER | Maximum retry attempts |
| `retry_count` | INTEGER | Current retry count |
| `status` | VARCHAR(50) | Task status |
| `started_at` | TIMESTAMP | When task started |
| `completed_at` | TIMESTAMP | When task completed |
| `error_message` | TEXT | Error message if task failed |
| `exit_code` | INTEGER | Command exit code |
| `output` | TEXT | Command output |
| `created_at` | TIMESTAMP | When task was created |
| `updated_at` | TIMESTAMP | When task was last updated |

### 3. `process_executions` Table
Tracks execution history:

| Column | Type | Description |
|--------|------|-------------|
| `execution_id` | VARCHAR(255) | Unique execution identifier (PRIMARY KEY) |
| `process_id` | VARCHAR(255) | Reference to process |
| `execution_started_at` | TIMESTAMP | When execution started |
| `execution_completed_at` | TIMESTAMP | When execution completed |
| `execution_status` | VARCHAR(50) | Execution status |
| `triggered_by` | VARCHAR(50) | What triggered the execution (MANUAL, SCHEDULED, API) |
| `error_message` | TEXT | Error message if execution failed |
| `created_at` | TIMESTAMP | When execution record was created |

## API Usage

### 1. Creating a Process

```java
// Create a manual process
ProcessController controller = new ProcessController(processManager, orchestrator, resultService);

String processId = "my-data-process";
String processType = "data-processing-pipeline";
String inputData = "input_file:/data/input.json;output_dir:/data/output;user_id:user123";
String schedule = null; // Manual execution only

ProcessManager.ProcessRecord process = controller.createProcess(processId, processType, inputData, schedule);
```

```java
// Create a scheduled process
String schedule = "0 2 * * *"; // Daily at 2 AM
ProcessManager.ProcessRecord process = controller.createProcess(processId, processType, inputData, schedule);
```

### 2. Starting a Process

```java
// Start a process by ID
ProcessController.ProcessStartResponse response = controller.startProcess(processId);

if (response.isSuccess()) {
    System.out.println("Process started: " + response.getMessage());
    System.out.println("Execution ID: " + response.getExecutionId());
} else {
    System.out.println("Failed to start: " + response.getMessage());
}
```

### 3. Getting Process State

```java
// Get current process state
ProcessController.ProcessStateResponse stateResponse = controller.getProcessState(processId);

ProcessManager.ProcessRecord process = stateResponse.getProcessRecord();
System.out.println("Status: " + process.getStatus());
System.out.println("Current Task: " + process.getCurrentTaskIndex() + "/" + process.getTotalTasks());
System.out.println("Started At: " + process.getStartedAt());
System.out.println("Completed At: " + process.getCompletedAt());

// Get execution history
List<ProcessManager.ExecutionRecord> history = stateResponse.getExecutionHistory();
for (ProcessManager.ExecutionRecord execution : history) {
    System.out.println("Execution: " + execution.getExecutionStartedAt() + 
                      " - " + execution.getExecutionStatus() + 
                      " (triggered by " + execution.getTriggeredBy() + ")");
}
```

### 4. Stopping a Process

```java
// Stop a running process
ProcessController.ProcessStopResponse stopResponse = controller.stopProcess(processId);

if (stopResponse.isSuccess()) {
    System.out.println("Process stopped: " + stopResponse.getMessage());
} else {
    System.out.println("Failed to stop: " + stopResponse.getMessage());
}
```

### 5. Process Management

```java
// Get all processes
List<ProcessManager.ProcessRecord> allProcesses = controller.getAllProcesses();

// Get processes by status
List<ProcessManager.ProcessRecord> pendingProcesses = controller.getProcessesByStatus("PENDING");
List<ProcessManager.ProcessRecord> completedProcesses = controller.getProcessesByStatus("COMPLETED");

// Get scheduled processes
List<ProcessManager.ProcessRecord> scheduledProcesses = processManager.getScheduledProcesses();

// Delete a process (only if not completed)
ProcessController.ProcessDeleteResponse deleteResponse = controller.deleteProcess(processId);
```

## Input Data Format

The `input_data` field uses a simple key-value format separated by semicolons:

```
input_file:/data/input.json;output_dir:/data/output;user_id:user123;batch_size:100
```

Supported keys:
- `input_file`: Input file path
- `output_dir`: Output directory path  
- `user_id`: User identifier
- Any other key-value pairs are stored as configuration

## Process Lifecycle

1. **PENDING**: Process created but not started
2. **IN_PROGRESS**: Process is currently executing
3. **COMPLETED**: Process finished successfully
4. **FAILED**: Process failed with error
5. **STOPPED**: Process was manually stopped

## Scheduled Execution

Processes with a `schedule` field containing a cron expression will be automatically executed:

- `"0 2 * * *"` - Daily at 2 AM
- `"0 2 * * 0"` - Weekly on Sunday at 2 AM  
- `"0 1 1 * *"` - Monthly on 1st at 1 AM
- `null` or `""` - Manual execution only

## Complete Example

```java
public class ProcessOrchestratorExample {
    public static void main(String[] args) {
        // Setup
        DataSource dataSource = createDataSource();
        ProcessTypeRegistry registry = createProcessTypeRegistry();
        ProcessOrchestrator orchestrator = new ProcessOrchestrator(dataSource, registry);
        ProcessResultService resultService = new ProcessResultService(dataSource);
        ProcessManager processManager = new ProcessManager(dataSource);
        ProcessController controller = new ProcessController(processManager, orchestrator, resultService);
        
        orchestrator.start();
        
        try {
            // Create a process
            String processId = "example-process";
            String inputData = "input_file:/data/input.json;output_dir:/data/output;user_id:example";
            
            ProcessManager.ProcessRecord process = controller.createProcess(
                processId, "data-processing-pipeline", inputData, null);
            
            // Start the process
            ProcessController.ProcessStartResponse startResponse = controller.startProcess(processId);
            
            if (startResponse.isSuccess()) {
                // Monitor the process
                while (true) {
                    ProcessController.ProcessStateResponse stateResponse = controller.getProcessState(processId);
                    ProcessManager.ProcessRecord currentProcess = stateResponse.getProcessRecord();
                    
                    System.out.println("Process status: " + currentProcess.getStatus());
                    
                    if ("COMPLETED".equals(currentProcess.getStatus()) || 
                        "FAILED".equals(currentProcess.getStatus())) {
                        break;
                    }
                    
                    Thread.sleep(5000); // Wait 5 seconds
                }
                
                // Get final state
                ProcessController.ProcessStateResponse finalState = controller.getProcessState(processId);
                System.out.println("Final status: " + finalState.getProcessRecord().getStatus());
            }
            
        } finally {
            orchestrator.stop();
        }
    }
}
```

## Running the Examples

1. **Database-Driven Process Example**:
   ```bash
   mvn exec:java -Dexec.mainClass="com.processorchestrator.examples.DatabaseDrivenProcessExample"
   ```

2. **Complete Database Example**:
   ```bash
   mvn exec:java -Dexec.mainClass="com.processorchestrator.examples.CompleteDatabaseExample"
   ```

## Key Benefits

1. **Persistent Storage**: Processes survive application restarts
2. **API-Driven**: Easy integration with external systems
3. **Scheduled Execution**: Automatic process execution based on cron expressions
4. **Process History**: Complete execution history tracking
5. **Flexible Management**: Start, stop, monitor processes via API calls
6. **Scalable**: Database-backed approach supports multiple instances

This database-driven approach provides a robust foundation for process orchestration in production environments.
