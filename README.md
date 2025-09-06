# Process Orchestrator

A robust process orchestrator built on top of [db-scheduler](https://github.com/kagkarlsson/db-scheduler) for managing sequential task execution with persistence and cluster support.

## Features

- **Sequential Task Execution**: Processes execute tasks in sequence, with automatic progression to the next task upon successful completion
- **Process State Management**: Tracks process states (Not Started, In Progress, Completed, Failed, Cancelled)
- **Persistent Storage**: All processes and tasks are stored in PostgreSQL with automatic recovery on restart
- **Task Restart Capability**: Failed tasks can be restarted individually while maintaining process state
- **Long-Running CLI Tasks**: Supports CLI commands that run for 1-100 minutes with automatic restart on engine reboot
- **Parallel Process Execution**: Multiple processes can run simultaneously while tasks within each process execute sequentially
- **Configurable Process Types**: Define process structures in Java code with multiple process types
- **High Throughput**: Designed to handle 5000+ processes with 10+ tasks per process

## Quick Start

### Prerequisites

- Java 17+
- PostgreSQL database
- Maven 3.6+

### Database Setup

1. **Using Docker (Recommended)**:
   ```bash
   # Start PostgreSQL and pgAdmin
   docker-compose up -d
   ```

2. **Manual Setup**:
   ```bash
   # Create database
   createdb process_orchestrator
   
   # Run schema
   psql -d process_orchestrator -f src/main/resources/schema.sql
   ```

### Running the Application

1. **Build the project**:
   ```bash
   mvn clean compile
   ```

2. **Run the main application**:
   ```bash
   mvn exec:java -Dexec.mainClass="com.processorchestrator.Main"
   ```

3. **Run examples**:
   ```bash
   mvn exec:java -Dexec.mainClass="com.processorchestrator.examples.ProcessOrchestratorExample"
   ```

## Usage Examples

### Basic Process Execution

```java
// Create and configure the application
DataSource dataSource = createDataSource();
ProcessOrchestratorApp app = new ProcessOrchestratorApp(dataSource);
app.start();

// Start a process
String processId = "my-process-" + System.currentTimeMillis();
app.getProcessOrchestrator().startProcess("data-processing-pipeline", processId);

// Monitor the process
Process process = app.getProcessOrchestrator().getProcess(processId);
System.out.println("Process status: " + process.getStatus());
```

### Custom Process Type

```java
// Define a custom process type
ProcessType customProcess = new ProcessType("custom-pipeline", "Custom data processing")
    .addTask("validate", "python scripts/validate.py", "/data", 30, 2)
    .addTask("transform", "python scripts/transform.py", "/data", 60, 3)
    .addTask("load", "python scripts/load.py", "/data", 45, 2);

// Register the process type
ProcessTypeRegistry registry = new ProcessTypeRegistry();
registry.register(customProcess);
```

## Process Types

The system comes with several built-in process types:

- **Data Processing Pipeline**: Validates, transforms, and loads data
- **Deployment Pipeline**: Builds, tests, and deploys applications
- **Backup Process**: Creates and manages data backups
- **ETL Pipeline**: Extract, Transform, Load data workflows
- **Monitoring Setup**: Configures monitoring and alerting

## Testing

Run the test suite:

```bash
mvn test
```

## Docker Support

The project includes Docker Compose configuration for easy setup:

- **PostgreSQL**: Database server
- **pgAdmin**: Web-based database administration

See `DOCKER-README.md` for detailed Docker setup instructions.

## Architecture

- **Process**: Represents a sequence of tasks that execute sequentially
- **Task**: Individual CLI command execution unit managed by db-scheduler
- **ProcessType**: Configuration defining the structure and tasks for a process
- **ProcessOrchestrator**: Main service managing process lifecycle and task coordination
- **CLITaskExecutor**: Executes CLI commands with timeout and retry support

## License

This project is licensed under the Apache License 2.0.


