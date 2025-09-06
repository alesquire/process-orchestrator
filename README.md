# Process Orchestrator

A robust process orchestration system built on top of `db-scheduler` that enables sequential task execution with automatic progression, retry mechanisms, and comprehensive result persistence.

## Features

- **Sequential Task Execution**: Processes execute tasks one after another with automatic progression
- **Process State Management**: Track process lifecycle (Not Started, In Progress, Completed, Failed)
- **Task Persistence**: All task and process data stored in PostgreSQL via `db-scheduler`
- **Retry Mechanisms**: Failed tasks can be automatically retried with configurable limits
- **CLI Task Execution**: Execute command-line utilities with cross-platform support
- **Result Persistence**: Complete audit trail of all process and task executions
- **Parallel Process Support**: Multiple processes can run concurrently
- **Scalable**: Designed to handle 5000+ processes with 10+ tasks each

## Architecture

The system leverages `db-scheduler`'s out-of-the-box capabilities:

- **Process Data**: Stored in `db-scheduler`'s `task_data` field as JSON
- **Task Scheduling**: Uses `db-scheduler`'s task scheduling and execution engine
- **Persistence**: Automatic persistence to PostgreSQL database
- **Cluster Support**: Built-in clustering and failover capabilities

## Quick Start

### 1. Setup Process Types

```java
// Define a data processing pipeline
ProcessType dataProcessingPipeline = new ProcessType("data-processing-pipeline", "Complete data processing pipeline")
    .addTask("load", "python scripts/load_data.py ${input_file} ${output_dir}/loaded_data.json", "/data", 30, 2)
    .addTask("process", "python scripts/process_data.py ${output_dir}/loaded_data.json ${output_dir}/processed_data.json", "/data", 60, 3)
    .addTask("generate", "python scripts/generate_report.py ${output_dir}/processed_data.json ${output_dir}/report.html", "/data", 45, 2)
    .addTask("analyze", "python scripts/analyze_results.py ${output_dir}/report.html ${output_dir}/analysis.json", "/data", 30, 2);

// Register the process type
ProcessTypeRegistry registry = new ProcessTypeRegistry();
registry.register(dataProcessingPipeline);
```

### 2. Create Input Data

```java
ProcessInputData inputData = new ProcessInputData();
inputData.setInputFile("/data/sample_input.json");
inputData.setOutputDir("/data/output");
inputData.setUserId("user123");
inputData.addConfig("batch_size", "100");
inputData.addConfig("quality_threshold", "95.0");
inputData.addMetadata("source_system", "legacy_database");
inputData.addMetadata("priority", "high");
```

### 3. Start Process

```java
ProcessOrchestrator orchestrator = new ProcessOrchestrator(dataSource, registry);
orchestrator.start();

String processId = orchestrator.startProcess("data-processing-pipeline", inputData);
```

### 4. Monitor Results

```java
// Get process status
ProcessData process = orchestrator.getProcess(processId);

// Get all tasks for the process
List<TaskData> tasks = orchestrator.getProcessTasks(processId);

// Get all processes
List<ProcessData> allProcesses = orchestrator.getAllProcesses();

// Get processes by status
List<ProcessData> completedProcesses = orchestrator.getProcessesByStatus(ProcessStatus.COMPLETED);
```

## Example: Data Processing Pipeline

The system includes a complete example of a data processing pipeline with four sequential tasks:

1. **Load**: Load data from input file
2. **Process**: Transform and process the data
3. **Generate**: Create HTML report from processed data
4. **Analyze**: Analyze results and create analysis report

### Python Scripts

The example includes Python scripts that demonstrate real CLI task execution:

- `scripts/load_data.py`: Loads and structures input data
- `scripts/process_data.py`: Applies transformations to the data
- `scripts/generate_report.py`: Creates HTML reports
- `scripts/analyze_results.py`: Performs analysis and quality checks

### Running the Example

```bash
# Compile the project
mvn compile

# Run the main application with examples
mvn exec:java -Dexec.mainClass="com.processorchestrator.Main"
```

## Process Structure

### ProcessInputData
Contains the initial context for a process:
- `inputFile`: Source file path
- `outputDir`: Output directory path
- `userId`: User identifier
- `config`: Configuration parameters (key-value pairs)
- `metadata`: Additional metadata (key-value pairs)

### ProcessData
Runtime process information stored in database:
- Process ID, type, and status
- Current task index and total tasks
- Timestamps (started, completed)
- Error messages
- List of TaskData objects
- Process context (runtime data)

### TaskData
Individual task information:
- Task ID, name, and command
- Working directory and timeout
- Retry configuration
- Execution status and timestamps
- Exit code and output
- Error messages

## Database Schema

The system creates additional tables for monitoring and result persistence:

```sql
-- Process tracking table
CREATE TABLE processes (
    process_id VARCHAR(255) PRIMARY KEY,
    process_type VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    current_task_index INTEGER NOT NULL DEFAULT 0,
    total_tasks INTEGER NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Task tracking table
CREATE TABLE tasks (
    task_id VARCHAR(255) PRIMARY KEY,
    process_id VARCHAR(255) NOT NULL,
    task_index INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    command TEXT NOT NULL,
    working_directory VARCHAR(500),
    timeout_minutes INTEGER NOT NULL DEFAULT 60,
    max_retries INTEGER NOT NULL DEFAULT 3,
    retry_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    exit_code INTEGER,
    output TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (process_id) REFERENCES processes(process_id)
);
```

## Configuration

### Process Types
Define reusable process templates with:
- Task sequences
- Command templates with variable substitution
- Working directories
- Timeout settings
- Retry limits

### Command Substitution
Commands support variable substitution:
- `${input_file}`: Input file path
- `${output_dir}`: Output directory path
- `${config.key}`: Configuration values
- `${metadata.key}`: Metadata values
- `${processContext.key}`: Runtime context values

## Error Handling

- **Task Failures**: Automatic retry with exponential backoff
- **Process Failures**: Complete process state tracking
- **Timeout Handling**: Configurable timeouts per task
- **Error Persistence**: All errors logged to database

## Monitoring and Observability

- **Process Status**: Real-time process state tracking
- **Task Execution**: Detailed task execution logs
- **Performance Metrics**: Execution times and resource usage
- **Error Tracking**: Comprehensive error logging and analysis

## Production Considerations

- **Database**: Use PostgreSQL for production deployments
- **Clustering**: Leverage `db-scheduler`'s built-in clustering
- **Monitoring**: Implement health checks and metrics collection
- **Logging**: Configure appropriate log levels and retention
- **Security**: Secure database connections and file system access

## Dependencies

- Java 17+
- Maven 3.6+
- PostgreSQL 12+ (or H2 for testing)
- Python 3.7+ (for example scripts)

## License

This project is licensed under the MIT License.