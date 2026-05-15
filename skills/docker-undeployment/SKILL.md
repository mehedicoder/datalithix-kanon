# Skill: Docker Undeployment

## Purpose

Stop and undeploy the application stack that was started with Docker Compose.

This skill safely stops running services first, verifies teardown, and only performs destructive cleanup when explicitly approved.

## Inputs

- Project root directory
- `docker-compose.yml`

## Output

- Application stack stopped/removed from Docker Compose
- Undeployment verification status
- Diagnostics if undeployment fails

## Required Files

The following file must exist in project root:

- `docker-compose.yml`

## Working Directory

All commands must be executed from project root.

## Undeployment Workflow

### 1. Validate Docker Availability

Run:

```sh
docker info
```

If Docker is unreachable:
- report failure
- stop execution

---

### 2. Validate Docker Compose

Run:

```sh
docker compose version
```

If unavailable:
- report failure
- stop execution

---

### 3. Capture Current Stack State

Run:

```sh
docker compose ps
docker compose ps --format json
```

Use this as pre-undeploy snapshot.

---

### 4. Stop and Remove Compose Services (Non-destructive)

Run:

```sh
docker compose down
```

This removes containers and network for the compose project, but keeps volumes.

---

### 5. Verify Undeployment

Run:

```sh
docker compose ps
docker compose ps --format json
```

Success condition:
- no running services in this compose project

---

### 6. Collect Diagnostics on Failure

If undeployment fails:

Run:

```sh
docker compose logs --tail=100
```

Collect:
- service(s) that did not stop
- error messages from compose
- resource conflict messages

---

## Optional Destructive Cleanup (Explicit Approval Required)

Only if user explicitly requests data cleanup:

```sh
docker compose down -v
```

This removes named volumes and can permanently delete local service data.

## Safety Rules

Allowed automatic operations:

```sh
docker info
docker compose version
docker compose ps
docker compose ps --format json
docker compose down
docker compose logs --tail=100
```

Operations requiring explicit user approval:

```sh
docker compose down -v
docker system prune
docker system prune -a
docker volume rm
```

Never perform destructive cleanup automatically.

## Quality Checks

- `docker-compose.yml` exists in project root
- Docker Engine reachable
- `docker compose down` executed from project root
- post-check confirms stack is down
- status clearly reported

## Failure Reporting Format

If undeployment fails, report:

```text
Undeployment failed.

Docker status:
...

Remaining service(s):
...

Detected issue:
...

Suggested next action:
...
```
