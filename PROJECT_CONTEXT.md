# Process Orchestrator Project Context

## Project Overview
- **Goal**: Create a Process Orchestrator based on db-scheduler engine
- **Location**: `D:\Puzzle\puzzle\process-orchestrator`
- **Status**: Code moved to clean directory, compilation errors need fixing

## Key Features Implemented
- Sequential task execution (tasks run in sequence within a process)
- Process state management (Not Started, In Progress, Completed, Failed, Cancelled)
- PostgreSQL persistence for processes and tasks
- Task restart capability for failed tasks
- Long-running CLI task support (1-100 minutes)
- Parallel process execution support
- Configurable process types in Java code
- Designed for 5000+ processes with 10+ tasks per process

## Architecture Components
- **Process**: Sequence of tasks that execute sequentially
- **Task**: Individual CLI command execution unit
- **ProcessType**: Configuration defining task structure
- **ProcessOrchestrator**: Main service managing lifecycle
- **CLITaskExecutor**: Executes CLI commands with timeout/retry
- **ProcessDAO/TaskDAO**: Database access objects

## Current Issues
- Compilation errors with db-scheduler API:
  - `TaskHandler` interface not found
  - `Task.of()` method signature issues
  - `pollingInterval()` method signature changed
  - `getSchedulerClient()` method not available

## Next Steps
1. Fix db-scheduler API compatibility issues
2. Test basic functionality
3. Run example processes
4. Verify PostgreSQL integration

## Database Setup
- PostgreSQL running via Docker Compose
- Database: `process_orchestrator`
- Username: `postgres`
- Password: `your-super-secret-and-long-postgres-password`
- pgAdmin available at http://localhost:8080

## Files Structure
```
src/main/java/com/processorchestrator/
├── model/ (Process, Task, ProcessStatus, TaskStatus)
├── dao/ (ProcessDAO, TaskDAO)
├── config/ (ProcessType, ProcessTypeRegistry, TaskDefinition)
├── service/ (ProcessOrchestrator)
├── executor/ (CLITaskExecutor)
├── scheduler/ (CLITaskHandler)
├── examples/ (ExampleProcessTypes, ProcessOrchestratorExample)
└── Main.java, ProcessOrchestratorApp.java
```
