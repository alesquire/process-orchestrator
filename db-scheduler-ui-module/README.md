# DB Scheduler UI Module

This module provides a web-based dashboard for monitoring and managing db-scheduler tasks in the Process Orchestrator.

## Features

- **View Tasks**: Monitor scheduled, running, and failed tasks
- **Task Management**: Re-run, run, or delete tasks directly from the UI
- **Task History**: View execution history of all tasks (optional)
- **Security**: Optional authentication and role-based access control
- **Real-time Monitoring**: Live updates of task status

## Getting Started

### Prerequisites

- Java 17+
- Spring Boot 3.3+
- Existing Process Orchestrator setup
- Database with `scheduled_tasks` table

### Running the UI Module

1. **Build the module**:
   ```bash
   mvn clean install
   ```

2. **Run the UI application**:
   ```bash
   cd db-scheduler-ui-module
   mvn spring-boot:run
   ```

3. **Access the UI**:
   - Open your browser and go to: `http://localhost:8080/db-scheduler`
   - The UI will show all your scheduled tasks with their current status

### Configuration

The UI can be configured via `application.properties`:

```properties
# Enable task history
db-scheduler-ui.history=true
db-scheduler-ui.log-limit=1000

# Show task data
db-scheduler-ui.task-data=true

# Read-only mode
db-scheduler-ui.read-only=false

# Custom context path
db-scheduler-ui.context-path=/admin
```

### Security (Optional)

To enable basic authentication, add to `application.properties`:

```properties
spring.security.user.name=admin
spring.security.user.password=admin123
spring.security.user.roles=ADMIN
```

**Security Roles**:
- `ADMIN`: Full access (can run, delete, schedule tasks)
- `USER`: Read-only access (can view tasks and history)

### Task History (Optional)

To enable task history, add the dependency to `pom.xml`:

```xml
<dependency>
    <groupId>io.rocketbase.extension</groupId>
    <artifactId>db-scheduler-log-spring-boot-starter</artifactId>
    <version>0.7.0</version>
</dependency>
```

Then set `db-scheduler-ui.history=true` in your properties.

## Integration with Process Orchestrator

This UI module integrates seamlessly with your existing Process Orchestrator:

1. **Shared Database**: Uses the same database and `scheduled_tasks` table
2. **Task Visibility**: Shows all tasks created by the Process Orchestrator
3. **Task Management**: Can manually trigger, retry, or delete tasks
4. **Monitoring**: Real-time status updates for all processes

## API Endpoints

The UI exposes REST API endpoints at `/db-scheduler-api/`:

- `GET /db-scheduler-api/tasks` - List all tasks
- `GET /db-scheduler-api/tasks/{id}` - Get task details
- `POST /db-scheduler-api/tasks/{id}/run` - Run a task manually
- `DELETE /db-scheduler-api/tasks/{id}` - Delete a task
- `GET /db-scheduler-api/history` - Get task execution history

## Troubleshooting

### Common Issues

1. **UI not accessible**: Check that the server is running on port 8080
2. **No tasks visible**: Ensure the Process Orchestrator is running and creating tasks
3. **Authentication issues**: Verify security configuration in `application.properties`

### Logs

Enable debug logging for troubleshooting:

```properties
logging.level.no.bekk.db-scheduler-ui=DEBUG
logging.level.com.processorchestrator=DEBUG
```

## Development

### Building from Source

```bash
mvn clean install
```

### Running Tests

```bash
mvn test
```

### Customization

The UI can be customized by:
- Modifying `application.properties` for configuration
- Adding custom security rules in `DbSchedulerUiSecurityConfig`
- Extending the configuration controller for dynamic behavior

## References

- [DB Scheduler UI GitHub](https://github.com/bekk/db-scheduler-ui)
- [DB Scheduler Documentation](https://github.com/kagkarlsson/db-scheduler)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
