# Kanon Application Testing Manual

This document defines manual and automated testing procedures for the Kanon platform. It covers smoke tests, integration tests, UI tests, performance/scalability tests, training pipeline tests, and security tests.

## Quick Reference

| Test category | Execution | Frequency |
| --- | --- | --- |
| Smoke tests | Manual or scripted | Per build / deployment |
| Integration tests | Automated (Maven) | Per commit / CI |
| UI tests | Automated + manual | Per milestone |
| Performance tests | Manual | Pre-release |
| Training pipeline tests | Manual + automated | Per milestone |
| Security tests | Automated + manual | Per milestone |

---

## 1. Smoke Tests

Run these after every deployment to verify basic platform health.

### 1.1 Platform Startup

| Step | Action | Expected result |
| --- | --- | --- |
| 1.1.1 | Start Kanon with `mvn -pl kanon-bootstrap -am spring-boot:run` | Application starts without errors. |
| 1.1.2 | Verify PostgreSQL and MongoDB connections in startup logs | Logs show successful database connection. |
| 1.1.3 | Verify YAML configuration packs (Accounting, HR) are loaded | Startup logs confirm pack import. |
| 1.1.4 | Access the Kanon UI at `http://localhost:8080` | Login page renders. |

### 1.2 Authentication

| Step | Action | Expected result |
| --- | --- | --- |
| 1.2.1 | Log in as Platform Admin | Dashboard loads with admin navigation items. |
| 1.2.2 | Log in as Tenant Admin | Dashboard loads with tenant-scoped views. |
| 1.2.3 | Log in as Reviewer / Annotator | Dashboard loads with review task views. |
| 1.2.4 | Log in as Auditor | Dashboard loads with audit/evidence views. |
| 1.2.5 | Log out | Session ends, login page displayed. |

### 1.3 Configuration

| Step | Action | Expected result |
| --- | --- | --- |
| 1.3.1 | Navigate to Configuration > Domains | Domain list displays Accounting and HR packs. |
| 1.3.2 | Navigate to Configuration > Workflows | MVP workflow templates visible. |
| 1.3.3 | Navigate to Configuration > Agents | MVP agent list visible. |

### 1.4 API Health

| Step | Action | Expected result |
| --- | --- | --- |
| 1.4.1 | Call `GET /api/health` | Returns 200 with status UP. |
| 1.4.2 | Call `GET /api/config/domains` | Returns valid domain list (needs auth). |

---

## 2. Integration Tests

Run these via Maven:

```bash
# Full test suite (excludes Docker-dependent tests)
mvn clean verify

# Including Docker-dependent integration tests
mvn clean verify -Dgroups="docker"
```

### 2.1 Requirements

- Docker (for Testcontainers-based tests)
- PostgreSQL and MongoDB Testcontainers configured automatically by test classes
- No external services required

### 2.2 Testcontainers Configuration

Tests tagged `@Tag("docker")` start PostgreSQL and MongoDB containers automatically:

| Container | Image | Purpose |
|-----------|-------|---------|
| PostgreSQL | `postgres:16` | Relational state (config, workflows, datasets, training, models, active learning) |
| MongoDB | `mongo:7` | Evidence ledger events |

The `@Profile("!test")` annotation on Postgres repositories ensures they are inactive during unit tests and active only during Spring Boot tests without the `test` profile. Tests that use `@SpringBootTest` with `@ActiveProfiles("tc")` or direct JDBC/Mongo connections can verify full persistence behavior.

### 2.3 Coverage Requirements

| Area | Minimum coverage |
| --- | --- |
| Repository layer | 90% of repository operations tested with Testcontainers |
| Service layer | 80% of service logic covered |
| Controller / API | 70% of API endpoints covered |
| All new modules | Per-module integration tests |

### 2.4 Test Types

- **Repository tests (PostgreSQL)**: Verify CRUD operations, upsert behavior, audit column population, tenant-aware indexes, and pagination via Testcontainers with `JdbcTemplate`.
- **Repository tests (MongoDB)**: Verify evidence event append, query by tenant/case/event-type, index creation, and cross-tenant isolation via Testcontainers with `MongoTemplate`.
- **Service tests**: Verify business logic, evidence emission, error handling, and permission checks.
- **Controller tests**: Verify request/response contracts, authorization enforcement, and error formatting.
- **Adapter tests**: Verify connector, model, annotation node, compute backend, and export adapter behavior with fakes.

### 2.5 Persistence Adapter Test Coverage

Each Postgres repository implementation has corresponding CRUD round-trip tests in `KanonPostgresRepositoryIntegrationTest`:

| Repository | Operations Tested |
|------------|------------------|
| `PostgresActiveConfigurationVersionRepository` | save, findActive, findActiveByTenant, findAllActive, deactivation, overwrite |
| `PostgresActiveLearningCycleRepository` | save, findById, findByTenant, findByModel, findByStatus, findAllByStatus, status transitions |
| `PostgresDatasetRepository` | saveDefinition, findDefinition, saveVersion, findVersions, version immutability |
| `PostgresTrainingJobRepository` | saveJob, findJob, updateStatus, saveBackend, findBackend, job lifecycle |
| `PostgresModelRegistryRepository` | saveEntry, findEntry, saveVersion, findVersion, saveEvaluation, findEvaluation, deploy |
| `PostgresAgentProfileRepository` | save, findById, findByTenant, enable/disable |
| `PostgresWorkflowDefinitionRepository` | save, findById, findByTenant, enable/disable |
| `PostgresWorkflowInstanceRepository` | save, findById, findByTenantAndCase, state transitions |
| `PostgresExternalAnnotationNodeRepository` | save, findById, findByTenant, status management |
| `PostgresModelProfileRepository` | save, findById, findByTenant, enable/disable |

MongoDB evidence persistence is tested via `KanonMongoEvidenceIntegrationTest`:
| Operation | Verification |
|-----------|-------------|
| Append event | Event is persisted and retrievable |
| Find recent | Events returned in correct order, limited |
| Find by case ID | Filtering by tenant + case, correct ordering |
| Index auto-creation | Required indexes exist after PostConstruct |
| Cross-tenant isolation | Tenant A cannot see Tenant B events |

---

## 3. UI Tests

### 3.1 Automated UI Tests

Run via:

```bash
mvn verify -Pui-tests
```

Coverage expectations:

| View | Test coverage |
| --- | --- |
| Login / authentication | Auth flow, invalid credentials, session expiry |
| Configuration views | Read-only display, edit forms, validation |
| Source connectors | List, create, edit, delete, health status |
| Source traces | List with filters, detail view, evidence link |
| Workflow board | List, filter by status, detail view |
| Agent management | List, create, edit, enable/disable, delete |
| Model configuration | List, create, edit, test connection, dry-run |
| External annotation nodes | List, create, edit, test connection, status display |
| Annotation sync status | Task list, sync status, retry action, evidence link |
| Evidence explorer | List by tenant/case, filter by event type, detail view |
| User / role management | List, create, edit, permission assignment |
| Administration views | Tenant, organization, workspace management |
| Security / audit views | Denied access, role changes, permission changes, break-glass |
| Localization | UI renders in English and German, locale switching works |
| Dataset management (post-MVP) | List, version history, curation config, export status |
| Training jobs (post-MVP) | List, detail, status, metrics, log viewer |
| Model registry (post-MVP) | List, version detail, lineage, evaluation results, comparison |
| Deployments (post-MVP) | List, deploy, rollback, health status |
| Active learning (post-MVP) | Cycle status, records selected, promotion queue |

### 3.2 Manual UI Test Scenarios

#### 3.2.1 End-to-End Ingestion → Annotation Flow

| Step | Action | Expected result |
| --- | --- | --- |
| 1 | Upload a document via Upload Connector UI | Source trace created, ingestion evidence recorded. |
| 2 | Verify Ingestion Agent processes the document | Agent state transitions. |
| 3 | Verify Extraction/Annotation Agent produces draft | Draft annotations visible in workflow board. |
| 4 | Review draft annotations in Kanon UI | Before/after diff view shows changes. |
| 5 | Push task to Label Studio (if configured) | External task link available. |
| 6 | Sync annotations back from Label Studio | Results normalized, evidence recorded. |
| 7 | Approve annotations in Kanon UI | Approval evidence recorded, state transitions. |
| 8 | Verify evidence explorer shows full event chain | All events visible with correct correlation ids. |

#### 3.2.2 Media Annotation Flow

| Step | Action | Expected result |
| --- | --- | --- |
| 1 | Upload a video file | Media asset created in object storage. |
| 2 | Verify media processing (thumbnail, frame extraction) | Processing evidence recorded. |
| 3 | Push annotation task to CVAT | External CVAT task created. |
| 4 | Sync CVAT result back | Annotations normalized, revisions created. |
| 5 | View annotation overlays in Kanon UI | Read-only overlays display correctly. |
| 6 | Approve video annotations | Approval evidence recorded. |

#### 3.2.3 Tenant Isolation

| Step | Action | Expected result |
| --- | --- | --- |
| 1 | Log in as Tenant A admin | Only Tenant A data visible. |
| 2 | Verify Tenant B data is not accessible | No Tenant B sources, workflows, or evidence visible. |
| 3 | Direct API call for Tenant B data | 403 Forbidden. |

---

## 4. Performance and Scalability Tests

### 4.1 Test Scenarios

| Scenario | Target | Method |
| --- | --- | --- |
| Source ingestion throughput | 1000 records/min per connector | Load test with synthetic data |
| API response time (p95) | < 500ms for paged list queries | JMeter / Gatling |
| Evidence write rate | > 100 events/sec | Async load test |
| Concurrent model invocations | 50 concurrent, < 5s p95 latency | Staged load test |
| UI grid rendering | < 2s for 10k rows with lazy loading | Browser performance test |
| Training job orchestration | 10 concurrent jobs | Compute backend test |

### 4.2 Pass Criteria

- P95 latency under target load is within limits.
- No connection pool exhaustion under sustained load.
- Evidence writes are not dropped under load.
- UI remains responsive with large datasets due to lazy loading and pagination.

---

## 5. Training Pipeline Tests (Post-MVP)

### 5.1 Dataset Curation Tests

| Test | Description |
| --- | --- |
| Curate annotations into dataset | Select annotation records, apply inclusion rules, verify dataset version created. |
| Train/val/test split strategies | Verify random, stratified, temporal, and domain-aware splits produce correct distributions. |
| Dataset version immutability | Verify a curated dataset version does not change after creation. |
| Export format correctness | Verify JSONL, Parquet, Hugging Face, and TFRecord exports match source records. |
| Streaming export | Verify large datasets export to object storage without OOM. |
| Cross-tenant isolation | Verify Tenant A cannot access Tenant B datasets. |

### 5.2 Training Job Tests

| Test | Description |
| --- | --- |
| Training job lifecycle | Submit job → verify queued → starting → running → completed state transitions. |
| Compute backend abstraction | Verify local GPU, Kubernetes, and cloud adapters all produce correct job submissions. |
| Checkpoint behavior | Verify mid-training checkpoints are stored and restartable. |
| Failure recovery | Simulate training failure → verify failure state, checkpoint preserved, evidence recorded. |
| Job cancellation | Cancel running job → verify clean state and evidence. |
| Concurrent jobs | Verify multiple training jobs run without interference. |

### 5.3 Model Registry Tests

| Test | Description |
| --- | --- |
| Model versioning | Register model → create new version → verify version history. |
| Model lineage | Verify full lineage chain: annotation → dataset → training run → model version. |
| Model lifecycle stages | Promote through dev → staging → production → deprecated → archived. |
| Promotion gates | Verify evaluation threshold gates block/promote correctly. |
| Rollback | Deploy production model → rollback → verify previous version active. |

### 5.4 Evaluation Tests

| Test | Description |
| --- | --- |
| Evaluation job execution | Run model against test dataset → verify metrics computed and stored. |
| Metric correctness | Verify accuracy, precision, recall, F1, etc. against known test data. |
| Model comparison | Compare two model versions on same test set → verify side-by-side display. |
| Threshold enforcement | Verify evaluation below promotion threshold blocks promotion. |

### 5.5 Active Learning Tests

| Test | Description |
| --- | --- |
| Uncertainty sampling | Verify low-confidence records are selected. |
| Diversity sampling | Verify selected records cover feature space. |
| Query-by-committee | Verify disagreement-based selection works with ensemble. |
| Cycle lifecycle | Trigger cycle → verify records selected, review tasks created, re-annotation collected, dataset updated, retraining triggered, evaluation completed. |
| Promotion decision | Verify better model promoted, worse model blocked with notification. |
| Cross-tenant isolation | Verify Tenant A active learning config and cycles are invisible to Tenant B. |

### 5.6 Deployment Tests

| Test | Description |
| --- | --- |
| Model deployment | Deploy model to serving endpoint → verify endpoint registered in Model Router. |
| Health check | Verify unhealthy endpoint is detected and traffic is blocked. |
| Rollback | Deploy → rollback → verify previous version handles inference. |
| Blue/green deployment | Verify configurable deployment strategy works correctly. |

---

## 6. Security Tests

### 6.1 Automated Security Tests

Run as part of the integration test suite.

| Test | Description |
| --- | --- |
| Cross-tenant isolation | Verify one tenant cannot read, write, or enumerate another tenant's records via API. |
| Unauthorized read | Verify user without read permission gets 403. |
| Unauthorized write | Verify user without write permission gets 403. |
| Redaction | Verify secrets, prompts, responses, and payloads are redacted without explicit permission. |
| Break-glass access | Verify break-glass requires reason, expiry, and approver; access is denied otherwise. |
| Evidence visibility | Verify users see only authorized evidence records. |
| Annotation revision immutability | Verify annotation corrections create revisions, not overwrites. |
| Training pipeline access | Verify training dataset, job, model registry, evaluation, and active learning operations enforce permissions. |
| Dataset export permissions | Verify user without export permission cannot trigger or download dataset exports. |
| Model promotion permissions | Verify user without promote permission cannot change model lifecycle stage. |

### 6.2 Manual Security Test Scenarios

| Step | Action | Expected result |
| --- | --- | --- |
| 1 | Log in as Reviewer, attempt to call admin API endpoint | 403 Forbidden. |
| 2 | Log in as Tenant A, attempt to access Tenant B evidence via manipulated API request | 403 or empty result, no data leak. |
| 3 | Verify API keys and secrets are never returned in UI or API responses | Redacted/masked values only. |
| 4 | Verify break-glass audit trail | Reason, approver, expiry, and access recorded as evidence. |
| 5 | Verify denied access produces evidence event | Security evidence visible in auditor view. |

---

## 7. Test Environment Setup

### 7.1 Local Development

```bash
# Start infrastructure
docker compose up -d

# Run all tests
mvn clean verify

# Run specific test module
mvn -pl kanon-policy test

# Run integration tests
mvn verify -Pintegration

# Run UI tests
mvn verify -Pui-tests
```

### 7.2 Required Services

| Service | Default connection | Test container |
| --- | --- | --- |
| PostgreSQL | `localhost:5432` | Testcontainers (integration) |
| MongoDB | `localhost:27017` | Testcontainers (integration) |
| MinIO | `localhost:9000` | Testcontainers or local |
| Label Studio (optional) | External | Not required for unit tests |
| CVAT (optional) | External | Not required for unit tests |

### 7.3 Test Fixtures

Test fixtures are located in each module's `src/test/resources`:

| Fixture | Location | Purpose |
| --- | --- | --- |
| Accounting domain YAML | `kanon-config/src/test/resources/` | Domain config loading tests |
| HR domain YAML | `kanon-config/src/test/resources/` | Domain config loading tests |
| Sample source traces | `kanon-connector/src/test/resources/` | Connector ingestion tests |
| Sample media metadata | `kanon-annotation/src/test/resources/` | Media asset tests |
| Sample dataset records | `kanon-dataset/src/test/resources/` | Dataset curation tests |
| Sample training config | `kanon-training/src/test/resources/` | Training job tests |

---

## 8. Test Checklist

Use this checklist before each release:

- [ ] All smoke tests pass
- [ ] Integration tests pass (no regressions)
- [ ] UI tests pass (automated + manual smoke)
- [ ] Security tests pass (no new vulnerabilities)
- [ ] Training pipeline tests pass (if post-MVP modules exist)
- [ ] Performance tests within thresholds
- [ ] No high-severity open issues
- [ ] Localization verified (English + German)
- [ ] Evidence ledger integrity verified (no missing events)
- [ ] Tenant isolation verified (manual + automated)

---

## 9. Bug Reporting

When a test fails:

1. Capture the exact step, input, and observed output.
2. Include relevant evidence event ids, case ids, and tenant context.
3. Attach logs, screenshots, or API request/response pairs.
4. File the issue with severity: blocker, critical, major, minor, or cosmetic.
