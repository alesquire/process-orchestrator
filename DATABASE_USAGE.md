# Database-Driven Process Orchestrator

This document explains how to use the Process Orchestrator with a database-driven approach, where processes are split into two tables for better separation of concerns.

## Database Schema

The Process Orchestrator uses two main tables with clear separation of responsibilities:

### 1. `process_definitions` Table (User-Managed)
Stores process templates and execution history:

| Column | Type | Description |
|--------|------|-------------|
| `id` | VARCHAR(255) | Unique process definition identifier (PRIMARY KEY) |
| `type` | VARCHAR(255) | Process type that defines the task structure |
| `input_data` | TEXT | Input data passed to the process and tasks |
| `schedule` | VARCHAR(255) | Cron expression for scheduled execution (NULL for manual only) |
| `current_status` | VARCHAR(50) | Current process status (PENDING, IN_PROGRESS, COMPLETED, FAILED, STOPPED) |
| `current_process_id` | VARCHAR(255) | Reference to active process in processes table |
| `started_when` | TIMESTAMP | When the process was last started |
| `completed_when` | TIMESTAMP | When the process was last completed |
| `failed_when` | TIMESTAMP | When the process last failed |
| `stopped_when` | TIMESTAMP | When the process was last stopped |
| `last_error_message` | TEXT | Last error message if process failed |
| `created_at` | TIMESTAMP | When the process definition was created |
| `updated_at` | TIMESTAMP | When the process definition was last updated |

### 2. `processes` Table (Engine-Managed)
Stores active process execution instances:

| Column | Type | Description |
|--------|------|-------------|
| `id` | VARCHAR(255) | Unique process instance identifier (PRIMARY KEY) |
| `definition_id` | VARCHAR(255) | Reference to process_definitions table |
| `type` | VARCHAR(255) | Process type (copied from definition) |
| `input_data` | TEXT | Input data (copied from definition) |
| `status` | VARCHAR(50) | Current process instance status |
| `current_task_index` | INTEGER | Index of currently executing task |
| `total_tasks` | INTEGER | Total number of tasks in the process |
| `started_at` | TIMESTAMP | When the process instance started |
| `completed_at` | TIMESTAMP | When the process instance completed |
| `error_message` | TEXT | Error message if process instance failed |
| `created_at` | TIMESTAMP | When the process instance was created |
| `updated_at` | TIMESTAMP | When the process instance was last updated |

### 3. `tasks` Table
Stores individual task execution details (unchanged):

| Column | Type | Description |
|--------|------|-------------|
| `task_id` | VARCHAR(255) | Unique task identifier (PRIMARY KEY) |
| `process_id` | VARCHAR(255) | Reference to processes table |
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

## Table Separation Benefits

### Process Definitions (User-Managed)
- **Purpose**: Templates and execution history
- **Management**: Created and deleted by users
- **Persistence**: Never automatically deleted
- **History**: Tracks all execution attempts
- **Status**: Shows current execution state

### Active Processes (Engine-Managed)
- **Purpose**: Current execution instances
- **Management**: Created and cleaned up by engine
- **Persistence**: Automatically cleaned up after completion
- **Instance**: One per execution attempt
- **Status**: Shows current task progress

## API Usage

### 1. Creating a Process Definition

```java
// Create a manual process definition
ProcessController controller = new ProcessController(processManager, orchestrator, resultService);

String definitionId = "my-data-process";
String processType = "data-processing-pipeline";
String inputData = "input_file:/data/input.json;output_dir:/data/output;user_id:user123";
String schedule = null; // Manual execution only

ProcessManager.ProcessDefinition definition = controller.createProcessDefinition(definitionId, processType, inputData, schedule);
```

```java
// Create a scheduled process definition
String schedule = "0 2 * * *"; // Daily at 2 AM
ProcessManager.ProcessDefinition definition = controller.createProcessDefinition(definitionId, processType, inputData, schedule);
```

### 2. Starting a Process

```java
// Start a process by definition ID
ProcessController.ProcessStartResponse response = controller.startProcess(definitionId);

if (response.isSuccess()) {
    System.out.println("Process started: " + response.getMessage());
    System.out.println("Process ID: " + response.getProcessId());
    System.out.println("Orchestrator ID: " + response.getOrchestratorProcessId());
} else {
    System.out.println("Failed to start: " + response.getMessage());
}
```

### 3. Getting Process State

```java
// Get current process state
ProcessController.ProcessStateResponse stateResponse = controller.getProcessState(definitionId);

ProcessManager.ProcessDefinition definition = stateResponse.getDefinition();
System.out.println("Definition Status: " + definition.getCurrentStatus());
System.out.println("Active Process ID: " + definition.getCurrentProcessId());
System.out.println("Last Started: " + definition.getStartedWhen());
System.out.println("Last Completed: " + definition.getCompletedWhen());

// Get active process details if running
ProcessManager.ActiveProcess activeProcess = stateResponse.getActiveProcess();
if (activeProcess != null) {
    System.out.println("Current Task: " + activeProcess.getCurrentTaskIndex() + "/" + activeProcess.getTotalTasks());
    System.out.println("Process Started: " + activeProcess.getStartedAt());
}

// Get task details if available
List<TaskData> tasks = stateResponse.getTasks();
if (tasks != null) {
    for (TaskData task : tasks) {
        System.out.println("Task: " + task.getName() + " - " + task.getStatus());
    }
}
```

### 4. Stopping a Process

```java
// Stop a running process
ProcessController.ProcessStopResponse stopResponse = controller.stopProcess(definitionId);

if (stopResponse.isSuccess()) {
    System.out.println("Process stopped: " + stopResponse.getMessage());
} else {
    System.out.println("Failed to stop: " + stopResponse.getMessage());
}
```

### 5. Process Definition Management

```java
// Get all process definitions
List<ProcessManager.ProcessDefinition> allDefinitions = controller.getAllProcessDefinitions();

// Get process definitions by status
List<ProcessManager.ProcessDefinition> pendingDefinitions = controller.getProcessDefinitionsByStatus("PENDING");
List<ProcessManager.ProcessDefinition> completedDefinitions = controller.getProcessDefinitionsByStatus("COMPLETED");

// Get scheduled process definitions
List<ProcessManager.ProcessDefinition> scheduledDefinitions = processManager.getScheduledProcessDefinitions();

// Delete a process definition (manual deletion only)
ProcessController.ProcessDeleteResponse deleteResponse = controller.deleteProcessDefinition(definitionId);
```

## Process Lifecycle

### Process Definition Lifecycle
1. **PENDING**: Process definition created but not started
2. **IN_PROGRESS**: Process definition has an active execution
3. **COMPLETED**: Process definition last execution completed successfully
4. **FAILED**: Process definition last execution failed
5. **STOPPED**: Process definition last execution was manually stopped

### Active Process Lifecycle
1. **PENDING**: Process instance created but not started
2. **IN_PROGRESS**: Process instance currently executing
3. **COMPLETED**: Process instance finished successfully
4. **FAILED**: Process instance failed with error
5. **STOPPED**: Process instance was manually stopped

## Scheduled Execution

Process definitions with a `schedule` field containing a cron expression will be automatically executed:

- `"0 2 * * *"` - Daily at 2 AM
- `"0 2 * * 0"` - Weekly on Sunday at 2 AM  
- `"0 1 1 * *"` - Monthly on 1st at 1 AM
- `null` or `""` - Manual execution only

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
            // Create a process definition
            String definitionId = "example-process";
            String inputData = "input_file:/data/input.json;output_dir:/data/output;user_id:example";
            
            ProcessManager.ProcessDefinition definition = controller.createProcessDefinition(
                definitionId, "data-processing-pipeline", inputData, null);
            
            // Start the process
            ProcessController.ProcessStartResponse startResponse = controller.startProcess(definitionId);
            
            if (startResponse.isSuccess()) {
                // Monitor the process
                while (true) {
                    ProcessController.ProcessStateResponse stateResponse = controller.getProcessState(definitionId);
                    ProcessManager.ProcessDefinition currentDefinition = stateResponse.getDefinition();
                    
                    System.out.println("Process definition status: " + currentDefinition.getCurrentStatus());
                    
                    if ("COMPLETED".equals(currentDefinition.getCurrentStatus()) || 
                        "FAILED".equals(currentDefinition.getCurrentStatus())) {
                        break;
                    }
                    
                    Thread.sleep(5000); // Wait 5 seconds
                }
                
                // Get final state
                ProcessController.ProcessStateResponse finalState = controller.getProcessState(definitionId);
                System.out.println("Final status: " + finalState.getDefinition().getCurrentStatus());
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

1. **Clear Separation**: User-managed definitions vs engine-managed instances
2. **Persistent History**: Process definitions retain execution history
3. **Automatic Cleanup**: Active processes are cleaned up after completion
4. **API-Driven**: Easy integration with external systems
5. **Scheduled Execution**: Automatic process execution based on cron expressions
6. **Process History**: Complete execution history tracking in definitions
7. **Flexible Management**: Start, stop, monitor processes via API calls
8. **Scalable**: Database-backed approach supports multiple instances

This database-driven approach provides a robust foundation for process orchestration in production environments with clear separation between user-managed templates and engine-managed execution instances.
