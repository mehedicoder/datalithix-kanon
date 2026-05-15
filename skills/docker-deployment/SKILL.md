# Skill: Docker Deployment

## Purpose

Deploy the application stack using Docker Compose.

This skill validates Docker availability, deploys the application, and verifies deployment health.

## Inputs

- Project root directory
- `Dockerfile`
- `docker-compose.yml`

## Output

- Running Docker containers for project services
- Deployment verification status
- Deployment diagnostics if failures occur

## Required Files

The following files must exist in project root:

- `Dockerfile`
- `docker-compose.yml`

## Working Directory

All commands must be executed from project root.

## Deployment Workflow

### 1. Validate Docker Availability

Run:

```sh
docker info
```

If Docker is reachable:
- continue deployment

If Docker is unavailable:
- execute Docker startup recovery for the current OS

---

### 2. Docker Startup Recovery (OS-specific)

If Docker daemon is not running, start it using the OS-native method:

- Linux: start/restart Docker service (`systemctl` or equivalent)
- macOS: start Docker Desktop
- Windows: start Docker Desktop

Then wait for Docker readiness.

Poll every 5 seconds:

```sh
docker info
```

Maximum wait time:
- 5 minutes

If Docker becomes reachable:
- continue deployment

If Docker is still unavailable:
- report deployment failure
- stop execution

---

### 3. Validate Docker Compose

Run:

```sh
docker compose version
```

If unavailable:
- stop deployment
- report missing Docker Compose support

---

### 4. Build and Deploy

Run:

```sh
docker compose up -d --build
```

---

### 5. Verify Deployment

Run:

```sh
docker compose ps
docker compose ps --format json
```

Validate:
- required services are running
- no critical container is exited
- no restart loop detected
- each required service has `"State": "running"` in JSON output
- each required service has a non-empty container name

Required service names are taken from `docker-compose.yml` service keys.

---

### 6. Collect Diagnostics on Failure

If deployment fails:

Run:

```sh
docker compose logs --tail=100
```

Collect:
- failing service names
- startup exceptions
- database connection failures
- Flyway migration failures
- port conflicts

---

## Success Criteria

Deployment is successful only if:

- Docker Engine is reachable
- Docker Compose executes successfully
- required containers are running
- no critical service is crash-looping

---

## Safety Rules

Allowed automatic operations:

```sh
docker info
docker compose version
docker compose ps
docker compose logs --tail=100
```

Operations requiring explicit user approval:

```powershell
docker compose down -v
docker system prune
docker system prune -a
docker volume rm
```

Never perform destructive cleanup automatically.

---

## Quality Checks

- `docker-compose.yml` exists in project root
- Docker Engine reachable before deployment
- deployment executed from project root
- no failed containers after deployment
- deployment status clearly reported
- JSON verification performed using `docker compose ps --format json`

---

## Failure Reporting Format

If deployment fails, report:

```text
Deployment failed.

Docker status:
...

Failed service:
...

Detected issue:
...

Suggested next action:
...
```
