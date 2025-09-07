# Process UI Module

A custom web-based dashboard for visualizing processes and tasks in a tabular format with real-time status updates.

## Features

- **Tabular Process View**: Processes displayed as rows, tasks as columns
- **Real-time Status Updates**: Auto-refresh every 5 seconds (configurable)
- **Process Statistics**: Overview cards showing total, completed, in-progress, and failed processes
- **Status Badges**: Color-coded status indicators for easy visual identification
- **Responsive Design**: Bootstrap-based UI that works on desktop and mobile
- **Security**: HTTP Basic authentication with configurable roles

## Architecture

### Backend Components

- **ProcessUiApplication**: Main Spring Boot application class
- **ProcessUiController**: REST API endpoints for process and task data
- **ProcessDashboardController**: Web controller for serving the dashboard page
- **ProcessUiSecurityConfig**: Security configuration with role-based access

### Frontend Components

- **Dashboard HTML**: Thymeleaf template with Bootstrap styling
- **JavaScript**: Client-side logic for data fetching and real-time updates
- **CSS**: Custom styling for status badges and responsive layout

## API Endpoints

### REST API (`/api/*`)

- `GET /api/processes` - Get all processes with their tasks in tabular format
- `GET /api/processes/{processId}` - Get specific process details
- `GET /api/statistics` - Get process statistics (counts by status)

### Web Endpoints

- `GET /` - Dashboard home page
- `GET /dashboard` - Dashboard page (same as home)

## Configuration

### Application Properties

```properties
# Server Configuration
server.port=8082
server.servlet.context-path=/

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/process_orchestrator
spring.datasource.username=postgres
spring.datasource.password=your-super-secret-and-long-postgres-password

# Security Configuration
spring.security.user.name=admin
spring.security.user.password=admin123
spring.security.user.roles=ADMIN

# Thymeleaf Configuration
spring.thymeleaf.cache=false
```

### Security

- **Authentication**: HTTP Basic authentication
- **Roles**: ADMIN (full access), USER (read-only access)
- **Protected Endpoints**: All API endpoints require authentication
- **Public Resources**: Static resources (CSS, JS, images) are publicly accessible

## Usage

### Starting the Application

#### Using Maven
```bash
cd process-ui-module
mvn spring-boot:run
```

#### Using Startup Scripts
```bash
# Git Bash
./start-process-ui.sh

# Windows Command Prompt
start-process-ui.bat
```

### Accessing the Dashboard

1. Open your browser and navigate to: `http://localhost:8082`
2. Login with credentials:
   - Username: `admin`
   - Password: `admin123`
3. The dashboard will load with:
   - Process statistics at the top
   - Tabular view of processes and tasks below
   - Auto-refresh every 5 seconds (can be disabled)

### Dashboard Features

#### Process Table
- **Process ID**: Unique identifier for each process
- **Type**: Process type (e.g., "test-process")
- **Status**: Current process status with color-coded badges
- **Progress**: Completed tasks / Total tasks
- **Created/Updated**: Timestamps for process lifecycle
- **Triggered By**: How the process was initiated
- **Task Columns**: Individual task statuses for each process

#### Status Badges
- **COMPLETED**: Green badge
- **IN_PROGRESS**: Yellow badge with spinner icon
- **FAILED**: Red badge
- **STOPPED**: Red badge
- **PENDING**: Gray badge

#### Controls
- **Auto-refresh**: Toggle automatic data refresh every 5 seconds
- **Refresh Now**: Manual refresh button
- **Floating Refresh**: Fixed refresh button in bottom-right corner

## Data Flow

1. **Frontend** requests data from `/api/processes` and `/api/statistics`
2. **ProcessUiController** queries `ProcessRecordDAO` for process data
3. **ProcessOrchestrator** retrieves task data from the database
4. **Response** includes processes, tasks, and statistics in JSON format
5. **Frontend** renders the data in the tabular format with status badges

## Dependencies

### Core Dependencies
- **Spring Boot Starter Web**: Web application framework
- **Spring Boot Starter Thymeleaf**: Template engine
- **Spring Boot Starter Security**: Security framework
- **Spring Boot Starter Data JPA**: Database access
- **PostgreSQL Driver**: Database connectivity

### UI Dependencies
- **Bootstrap 5.1.3**: CSS framework for responsive design
- **Font Awesome 6.0.0**: Icons for status indicators
- **Thymeleaf**: Server-side template engine

## Development

### Project Structure
```
process-ui-module/
├── src/main/java/com/processorchestrator/ui/
│   ├── ProcessUiApplication.java
│   ├── controller/
│   │   ├── ProcessUiController.java
│   │   └── ProcessDashboardController.java
│   └── config/
│       └── ProcessUiSecurityConfig.java
├── src/main/resources/
│   ├── application.properties
│   └── templates/
│       └── dashboard.html
└── pom.xml
```

### Building
```bash
mvn clean compile
mvn clean package
```

### Testing
The module integrates with the existing process orchestrator core, so it uses the same database and can display real process data.

## Integration

This module integrates with:
- **process-orchestrator-core**: Core process orchestration functionality
- **ProcessRecordDAO**: Database access for process records
- **ProcessOrchestrator**: Service layer for process and task management
- **PostgreSQL Database**: Shared database with the core module

## Future Enhancements

- **WebSocket Support**: Real-time updates without polling
- **Process Management**: Start/stop processes from the UI
- **Task Details**: Expandable task information
- **Filtering/Sorting**: Advanced table controls
- **Export Functionality**: Export process data to CSV/Excel
- **Process Templates**: Create new processes from templates
