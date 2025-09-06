# Process Orchestrator - Database Configuration

## Database Setup

The Process Orchestrator uses PostgreSQL as its database. Database credentials are configured through properties files.

### Configuration Files

#### Main Configuration (`src/main/resources/application.properties`)
This file contains the main database configuration for production use:

```properties
# Database connection details
db.url=jdbc:postgresql://localhost:5432/process_orchestrator
db.username=postgres
db.password=password

# Connection pool settings
db.pool.initial.size=5
db.pool.max.size=20
db.pool.min.idle=5

# Connection timeout settings
db.connection.timeout=30000
db.socket.timeout=60000
```

#### Test Configuration (`src/test/resources/test.properties`)
This file contains test-specific database configuration:

```properties
# Test database connection details
test.db.url=jdbc:postgresql://localhost:5432/process_orchestrator
test.db.username=postgres
test.db.password=password

# Test connection pool settings
test.db.pool.initial.size=2
test.db.pool.max.size=5
test.db.pool.min.idle=1

# Test connection timeout settings
test.db.connection.timeout=10000
test.db.socket.timeout=30000
```

### Setting Up PostgreSQL

1. **Install PostgreSQL** on your system
2. **Create a database** named `process_orchestrator`:
   ```sql
   CREATE DATABASE process_orchestrator;
   ```
3. **Update the properties files** with your actual database credentials:
   - Update `db.username` and `db.password` in `application.properties`
   - Update `test.db.username` and `test.db.password` in `test.properties`
4. **Ensure PostgreSQL is running** on the configured host and port

### Database Schema

The database schema is automatically created by the `DBInitializer` class when the application starts. The schema includes:

- `process_record` - Process templates and execution history
- `processes` - Active process instances
- `tasks` - Individual task execution details
- `process_executions` - Process execution history
- `scheduled_tasks` - db-scheduler required table

### Running Tests

Tests will automatically use the test configuration from `test.properties`. Make sure your PostgreSQL database is accessible with the credentials specified in the test properties file.

### Environment-Specific Configuration

You can override the default configuration by:

1. **Setting system properties**:
   ```bash
   java -Ddb.url=jdbc:postgresql://your-host:5432/your-db \
        -Ddb.username=your-user \
        -Ddb.password=your-password \
        -jar process-orchestrator.jar
   ```

2. **Using environment variables** (if you modify `DatabaseConfig` to support them)

3. **Creating environment-specific properties files** and placing them in the classpath

### Troubleshooting

#### Connection Issues
- Verify PostgreSQL is running: `pg_isready -h localhost -p 5432`
- Check database exists: `psql -h localhost -U postgres -l`
- Verify credentials: `psql -h localhost -U postgres -d process_orchestrator`

#### Permission Issues
- Ensure the database user has CREATE, INSERT, UPDATE, DELETE permissions
- For tests, ensure the user can create/drop tables

#### Port Issues
- Default PostgreSQL port is 5432
- Update `db.url` if using a different port
