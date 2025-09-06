# Docker Setup for Process Orchestrator

This directory contains Docker configuration files to run PostgreSQL and pgAdmin for the Process Orchestrator application.

## Quick Start

### Prerequisites
- Docker Desktop installed and running
- Windows 10/11 with WSL2 enabled (recommended)

### Starting the Services

**Option 1: Using the management script (Recommended)**
```bash
# Run the batch script
docker-manage.bat

# Or run the PowerShell script
.\docker-manage.ps1
```

**Option 2: Using Docker Compose directly**
```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down
```

## Services

### PostgreSQL Database
- **Port**: 5432
- **Database**: process_orchestrator
- **Username**: postgres
- **Password**: your-super-secret-and-long-postgres-password
- **Data Storage**: `./postgres-data/` (Windows local directory)

### pgAdmin Web Interface
- **URL**: http://localhost:8080
- **Email**: admin@processorchestrator.com
- **Password**: admin123
- **Data Storage**: `./pgadmin-data/` (Windows local directory)

## Directory Structure

```
.
├── docker-compose.yml          # Docker Compose configuration
├── docker-manage.bat          # Windows batch management script
├── docker-manage.ps1          # PowerShell management script
├── postgres-data/             # PostgreSQL data directory (created automatically)
├── pgadmin-data/              # pgAdmin data directory (created automatically)
└── src/main/resources/schema.sql  # Database schema (mounted to PostgreSQL)
```

## Data Persistence

The Docker setup uses Windows local directories for data persistence:

- **PostgreSQL Data**: Stored in `./postgres-data/`
- **pgAdmin Data**: Stored in `./pgadmin-data/`

These directories are created automatically when you first start the services. Your data will persist between container restarts and even if you remove the containers.

## Connecting to PostgreSQL

### From your Java application:
```java
String url = "jdbc:postgresql://localhost:5432/process_orchestrator";
String username = "postgres";
String password = "your-super-secret-and-long-postgres-password";
```

### From pgAdmin:
1. Open http://localhost:8080 in your browser
2. Login with admin@processorchestrator.com / admin123
3. Add a new server:
   - **Name**: Process Orchestrator DB
   - **Host**: postgres (or localhost)
   - **Port**: 5432
   - **Username**: postgres
   - **Password**: your-super-secret-and-long-postgres-password

### From command line:
```bash
# Using psql
psql -h localhost -p 5432 -U postgres -d process_orchestrator

# Using Docker exec
docker exec -it process_orchestrator_postgres psql -U postgres -d process_orchestrator
```

## Management Commands

### Using Docker Compose
```bash
# Start services in background
docker-compose up -d

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f postgres
docker-compose logs -f pgadmin

# Stop services
docker-compose down

# Stop and remove volumes (WARNING: This deletes all data!)
docker-compose down -v

# Restart services
docker-compose restart

# Check status
docker-compose ps
```

### Using Docker commands
```bash
# List running containers
docker ps

# View container logs
docker logs process_orchestrator_postgres
docker logs process_orchestrator_pgadmin

# Execute commands in container
docker exec -it process_orchestrator_postgres bash
docker exec -it process_orchestrator_pgadmin bash

# Stop specific container
docker stop process_orchestrator_postgres
docker stop process_orchestrator_pgadmin
```

## Troubleshooting

### Common Issues

1. **Port already in use**
   - Check if PostgreSQL is already running on port 5432
   - Check if pgAdmin is already running on port 8080
   - Stop conflicting services or change ports in docker-compose.yml

2. **Permission denied on Windows**
   - Ensure Docker Desktop has access to your drive
   - Run Docker Desktop as administrator
   - Check WSL2 integration settings

3. **Data not persisting**
   - Ensure the postgres-data and pgadmin-data directories exist
   - Check Docker Desktop volume settings
   - Verify the volume mounts in docker-compose.yml

4. **Cannot connect to database**
   - Wait for PostgreSQL to fully start (check health status)
   - Verify the password matches in all configurations
   - Check firewall settings

### Health Checks

The PostgreSQL container includes a health check. You can monitor it with:
```bash
docker ps
```

Look for the "healthy" status in the STATUS column.

### Logs

View detailed logs to troubleshoot issues:
```bash
# All services
docker-compose logs

# Specific service
docker-compose logs postgres
docker-compose logs pgadmin

# Follow logs in real-time
docker-compose logs -f
```

## Security Notes

- The default passwords are for development only
- Change passwords for production use
- Consider using Docker secrets for sensitive data
- Ensure proper network isolation in production

## Backup and Restore

### Backup PostgreSQL data
```bash
# Create backup
docker exec process_orchestrator_postgres pg_dump -U postgres process_orchestrator > backup.sql

# Or backup the entire data directory
# The postgres-data directory contains all PostgreSQL data
```

### Restore PostgreSQL data
```bash
# Restore from SQL dump
docker exec -i process_orchestrator_postgres psql -U postgres process_orchestrator < backup.sql
```

## Performance Tuning

For better performance, you can modify the PostgreSQL configuration:

1. Create a custom postgresql.conf file
2. Mount it in the docker-compose.yml:
```yaml
volumes:
  - ./postgres-data:/var/lib/postgresql/data
  - ./postgresql.conf:/etc/postgresql/postgresql.conf
```

## Cleanup

To completely remove all data and containers:
```bash
# Stop and remove containers and volumes
docker-compose down -v

# Remove unused Docker resources
docker system prune -f

# Remove the data directories
rmdir /s postgres-data
rmdir /s pgadmin-data
```

**Warning**: This will permanently delete all your database data!


