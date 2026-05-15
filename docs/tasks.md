# KANON Task Tracker

This document tracks near-term implementation work derived from the current README, module structure, and existing module skeletons.

This file is the authoritative implementation status tracker for the repository.

For product-level architecture and documentation navigation, use `README.md` as the entry point.

For behavior-level requirements, use `docs/spec.md` as the source of truth. Every new behavior task must reference an existing spec id or add/update a spec entry in the same change. Specs answer what happens, failure behavior, and edge cases; they must not describe implementation details.

For module boundaries, use `docs/module-structure.md` as the source of truth. Keep code in existing modules unless a new boundary is explicitly justified.

For MVP agent and workflow taxonomy, use `docs/mvp-agents-workflows.md` as the source of truth. Do not introduce new MVP agent or workflow types unless that document is updated with the reason.

For LLM service configuration, use `docs/llm-service-configuration.md` as the source of truth. LLM models must be configurable Spring-managed services and must be manageable from the admin UI.

For data source architecture, use `docs/data-source-architecture.md` as the source of truth. Think in source categories and connectors, not just files.

For security and access control, use `docs/security-access-control.md` as the source of truth. Enforce authorization in APIs, services, repositories, and UI visibility.

For tenant, organization, workspace, membership, role, and bootstrap administration modeling, use `docs/multi-tenant-governance-architecture.md` as the source of truth. Workspace is the operational boundary for workflows, agents, cases, tasks, human review, policy bindings, model preferences, and workspace-scoped audit visibility.

For scalability, use `docs/scalability-architecture.md` as the source of truth. Scalability is a primary coding and architecture constraint.

For configuration architecture, use `docs/configuration-architecture.md` as the source of truth. YAML templates are for bootstrap and reusable packs; PostgreSQL active configuration is the runtime source of truth.

For localization architecture, use `docs/localization-architecture.md` as the source of truth. UI strings should use translation keys and the initial supported languages are English and German.

Runtime defaults must use local PostgreSQL and local MongoDB, not in-memory stores. Docker assets must stay cloud-deployable as the application evolves.

## SDD Governance Links

- `docs/sdd/CONSTITUTION.md`
- `docs/sdd/CHANGE_CONTROL.md`
- `docs/sdd/TRACEABILITY.md`
- `docs/specification/*`

Behavior-changing task execution must satisfy change-control and traceability rules before completion status can be marked done.

## Status Legend

- `[ ]` Not started
- `[~]` In progress
- `[x]` Done
- `[!]` Blocked or needs a decision

## Spec Coverage

Task entries should stay implementation-oriented and checklist-friendly. Behavior belongs in `docs/spec.md` and is linked by spec id.

| Task area | Behavior spec |
| --- | --- |
| Configuration contracts | `SPEC-001: Configuration Activation` |
| Persistence adapters and backend persistence rules | `SPEC-002: Runtime Persistence` |
| Security and access control | `SPEC-003: Security and Access Control` |
| Data source connectors | `SPEC-004: Data Source Ingestion` |
| AI routing adapters | `SPEC-005: AI Model Routing and Invocation` |
| Workflow planner / GOAP | `SPEC-006: Workflow Planning` |
| Agent runtime and evidence | `SPEC-007: Agent Runtime and Evidence` |
| API and Vaadin command center | `SPEC-008: Command Center UI and APIs` |
| Media, drone, robotics, and external annotation tools | `SPEC-009: Media, Drone, and Robotics Data` |
| Bootstrap and operations | `SPEC-010: Bootstrap and Operations` |
| Annotation intake and bulk upload | `SPEC-011: Annotation Intake and Bulk Upload` |
| Localization | `SPEC-012: Localization` |
| Dataset curation and management | `SPEC-016: Dataset Curation and Management` |
| Model training orchestration | `SPEC-017: Model Training Orchestration` |
| Model registry, evaluation, and serving | `SPEC-018: Model Registry and Versioning`, `SPEC-019: Model Evaluation and Testing`, `SPEC-021: Model Serving and Deployment` |
| Active learning and continuous training | `SPEC-020: Active Learning and Continuous Training` |

## Documentation Map

| Document | Role in planning |
| --- | --- |
| `README.md` | Product-level architecture and user-facing positioning. |
| `docs/spec.md` | Behavior requirements for task work. |
| `docs/module-structure.md` | Module ownership and integration boundaries. |
| `docs/configuration-architecture.md` | Configuration source-of-truth and activation rules. |
| `docs/data-source-architecture.md` | Source categories, connectors, upload/manual entry, and traceability. |
| `docs/security-access-control.md` | Authorization, tenant isolation, redaction, and security evidence. |
| `docs/multi-tenant-governance-architecture.md` | Tenant, organization, workspace, user, membership, role, and bootstrap administration model. |
| `docs/scalability-architecture.md` | Scale constraints, async work, queues, object storage, and external annotation scaling. |
| `docs/llm-service-configuration.md` | Model service/profile configuration. |
| `docs/mvp-agents-workflows.md` | Fixed MVP agent/workflow taxonomy. |
| `docs/localization-architecture.md` | UI translation, locale selection, fallback, and language expansion rules. |

## Recommendation Check

The proposed next steps are recommended, with one ordering adjustment: establish configuration contracts before adding external adapters. The repository already defines the intended module boundaries and uses the package root `ai.datalithix.kanon`, so new code should continue under that root.

| Proposed step | Recommendation | Notes |
| --- | --- | --- |
| Add persistence adapters: PostgreSQL and MongoDB | Recommended | Fits the README stack. Use PostgreSQL for tenant/domain/workflow state and MongoDB for append-oriented evidence events. Start behind repository interfaces so in-memory implementations remain useful for tests and demos. |
| Add LangChain4j / Spring AI model adapters in `kanon-ai-routing` | Recommended | The current router resolves logical model route names only. Add provider adapters after the model profile and routing contracts are stable. |
| Add Embabel / GOAP planner integration in `kanon-workflow` | Recommended | The current planner is static. Add GOAP as an adapter behind `WorkflowPlanner` after workflow actions, goals, and policy inputs are modeled. |
| Add Label Studio / CVAT annotation node adapters in `kanon-annotation` | Recommended | Kanon should orchestrate and govern annotation tasks while specialized open source tools execute the human annotation UI. Kanon remains the source of truth. |
| Expand the Vaadin command center in `kanon-ui` | Recommended | The UI is currently a shell. Build read-only operational views before adding mutation workflows. |
| Add tenant-specific YAML / database-backed configuration loading | Highly recommended first | This is a prerequisite for domain-configurable behavior, policy-driven routing, and tenant-specific model/workflow behavior. |
| Suggested package root: `ai.datalithix.kanon` | Already applied | Existing Java sources already use this root. Keep module subpackages under it, such as `ai.datalithix.kanon.airouting`. |

## Current Baseline

- `[x]` Maven parent project defines modules for common, domain, tenant, policy, AI routing, agent runtime, workflow, evidence, annotation, API, UI, and bootstrap.
- `[x]` Package root is `ai.datalithix.kanon`.
- `[x]` Baseline in-memory/easy-start implementations exist for model routing, workflow planning, and evidence ledger.
- `[x]` Vaadin command center shell exists.
- `[x]` MVP agent and workflow taxonomy is documented in `docs/mvp-agents-workflows.md`.
- `[x]` LLM service configuration approach is documented in `docs/llm-service-configuration.md`.
- `[x]` Data source architecture is documented in `docs/data-source-architecture.md`.
- `[x]` Security and access control model is documented in `docs/security-access-control.md`.
- `[x]` Scalability architecture is documented in `docs/scalability-architecture.md`.
- `[x]` Configuration architecture is documented in `docs/configuration-architecture.md`.
- `[ ]` Persistence adapters are not implemented.
- `[ ]` External AI model adapters are not implemented.
- `[ ]` GOAP planner integration is not implemented.
- `[~]` Tenant/domain configuration contracts, YAML loading, startup bootstrap import, admin read APIs, and PostgreSQL active configuration version persistence are started.
- `[ ]` Video, drone, and robotics data storage contracts are not implemented.
- `[~]` External annotation execution contracts are started in `kanon-annotation`; Label Studio and CVAT adapters are not implemented.
- `[~]` V1 data source connector contracts are started; concrete connectors are not implemented.
- `[x]` Security roles, permissions, ABAC dimensions, tenant isolation contracts, role/permission persistence ports, break-glass contracts, and security audit contracts are defined.
- `[x]` Scalability contracts for paged queries, tenant-aware table/index definitions, connector execution policy, workflow execution policy, model execution policy, and lazy UI data loading are defined.
- `[x]` Behavior spec registry is documented in `docs/spec.md` and mapped to task areas.

## Milestone 1: Configuration Contracts

- `[x]` Implement configuration architecture according to `docs/configuration-architecture.md`.
- `[x]` Add a dedicated `kanon-config` module for configuration contracts, YAML loading, validation, registry, and seed/import contracts.
- `[x]` Define typed configuration contracts for tenant, domain, workflow template, agent definition, model routing policy, connector definition, and policy template.
- `[x]` Define tenant configuration schema for tenant id, domain, regulatory act, policy references, model preferences, connector settings, security defaults, and data residency.
- `[x]` Define domain configuration schema for entities, fields, tasks, agents, rules, workflow goals, and supported source/asset types.
- `[x]` Add sample YAML templates for Accounting and HR domain packs.
- `[x]` Add sample YAML templates for workflow, agent, model routing, connector, and policy templates.
- `[x]` Add YAML-backed template loader for local development, bootstrap, and test fixtures.
- `[x]` Add validation for required fields, duplicate ids, known domain/task/source/model values, and cross-template references.
- `[x]` Add in-memory configuration registry for validated templates.
- `[x]` Add active configuration version and seed/import service contracts for later database persistence.
- `[x]` Load Accounting and HR YAML packs during application startup and import them into the active in-memory configuration registry.
- `[x]` Add admin/read API endpoints to inspect loaded config packs, domains, tenants, workflows, agents, model routing policies, connectors, policies, and active configuration versions.
- `[x]` Add PostgreSQL-backed active configuration version repository for runtime use.
- `[x]` Ensure runtime services consume typed configuration objects, not raw YAML maps.
- `[x]` Add unit tests for valid config, missing fields, duplicate ids, invalid domain/source/task values, and broken references.

## Milestone 2: Persistence Adapters

- `[x]` Define repository ports for tenant profiles, domain definitions, active configuration versions, workflow state, and evidence events.
- `[x]` Define repository ports for source descriptors, source traces, ingestion batches, connector configuration, and connector health.
- `[x]` Replace in-memory implementations with PostgreSQL/MongoDB runtime adapters; keep fakes only for isolated unit tests where needed.
- `[x]` Add PostgreSQL adapter module or package for tenant, domain, active configuration versions, and workflow state.
- `[x]` Add Flyway database migrations (V1–V15) for all PostgreSQL tables.
- `[x]` Ensure every Flyway-created backend table includes audit columns such as `created_at`, `created_by`, `updated_at`, `updated_by`, and `audit_version`.
- `[x]` Ensure persistence code explicitly writes and updates audit columns instead of relying on migrations alone.
- `[x]` Add MongoDB evidence store adapter (`MongoEvidenceLedger`) in `kanon-bootstrap`.
- `[x]` Add indexes for tenant id, case id, event type, actor type, and event timestamp (PostgreSQL + MongoDB).
- `[x]` Add Testcontainers-based integration tests for PostgreSQL repositories (CRUD, tenant isolation, audit columns).
- `[x]` Add Testcontainers-based integration tests for MongoDB evidence storage (append, query, ordering, isolation, indexes).
- `[x]` Document local database startup and required environment variables in README.md and contributing.md.

## Backend Persistence Rules

- `[x]` New relational tables must be introduced through Flyway migrations.
- `[x]` Flyway migrations must include audit columns for backend-owned state tables: `created_at`, `created_by`, `updated_at`, `updated_by`, and an optimistic locking or revision column where updates are expected.
- `[x]` Backend-owned state tables must include access-control dimensions where applicable, such as `tenant_id`, `domain_type`, `case_id`, `owner_id`, `assigned_user_id`, `assigned_group_id`, `data_classification`, `compliance_classification`, and `data_residency`.
- `[x]` Flyway migrations must include tenant-aware indexes and operational indexes for expected query patterns such as tenant, case, status, event type, actor type, timestamp, source identifier, and workflow state.
- `[x]` Every new PostgreSQL table must include deliberate indexes on the best candidate columns for faster searching, filtering, joining, sorting, tenant isolation, and operational dashboards.
- `[x]` Index candidates must be chosen from real access patterns, including tenant id, foreign keys, natural/external identifiers, case id, workflow id, source trace id, media asset id, status, type, classification, created/updated timestamps, and frequently filtered UI grid columns.
- `[x]` Composite indexes should prefer tenant-first ordering for tenant-owned data, such as `(tenant_id, status, created_at)` or `(tenant_id, case_id)`, when those match query patterns.
- `[x]` Avoid adding low-value indexes blindly; each index should support a known query, lookup, join, uniqueness rule, or dashboard filter.
- `[x]` Append-only tables, such as immutable evidence events, must include creation audit metadata and must not expose update paths unless there is a documented exception.
- `[x]` Repository/entity code must populate audit columns on insert and update.
- `[x]` Repository list queries must support pagination, sorting, filtering, and tenant-scoped predicates.
- `[x]` Tests for persistence adapters must verify audit columns are written correctly.

## Scalability Rules

- `[ ]` Implement scalability according to `docs/scalability-architecture.md`.
- `[ ]` Do not load unbounded lists into memory.
- `[ ]` Every list API and grid-backed query must support server-side pagination, sorting, and filtering.
- `[ ]` Long-running operations must run asynchronously instead of blocking request/UI threads.
- `[ ]` Heavy connector ingestion, model invocation, media processing, and export paths must support retries, backpressure, and idempotency.
- `[ ]` External annotation task push, result pull, import, export, and retry must run through async jobs or queues.
- `[ ]` Scale Label Studio and CVAT as execution workbenches only: partition tasks, store large payloads in object storage, keep metadata in Kanon, and track sync lag/failures.
- `[ ]` Avoid wrapping slow external calls in database transactions.
- `[ ]` Avoid N+1 query patterns and chatty service loops.
- `[ ]` Use object storage for large payloads and direct/presigned transfer where possible.
- `[ ]` Add observability for throughput, latency, queue depth, error rate, fallback rate, and denial rate.

## Security and Access Control Rules

- `[ ]` Implement security according to `docs/security-access-control.md`.
- `[ ]` Enforce default-deny authorization for APIs, services, repositories, and UI actions.
- `[ ]` Enforce tenant isolation on every tenant-owned record and query.
- `[ ]` Use RBAC for coarse permissions and ABAC for tenant, domain, case, assignment, source, classification, residency, and purpose checks.
- `[ ]` Keep metadata access separate from payload/download access.
- `[ ]` Redact or hide secrets, prompts, responses, raw payloads, and sensitive fields unless explicit permission allows visibility.
- `[x]` Log evidence/security events for sensitive reads, mutations, denied access, exports, and break-glass access.
- `[ ]` Never rely on Vaadin menu hiding as the only security control.

## Milestone 3: Security Foundation

- `[x]` Define MVP roles from `docs/security-access-control.md`: Platform Admin, Tenant Admin, Domain Manager, Reviewer / Annotator, Auditor, Integration Service Account, Model Operator, and Viewer.
- `[x]` Define permission constants for tenant, user, role, policy, connector, source, asset, annotation, review, workflow, evidence, model, and break-glass actions.
- `[x]` Define access-control context object with tenant id, user id, roles, permissions, domain scope, assignment scope, data classifications, and purpose.
- `[x]` Define security dimensions for tenant, organization unit, domain, case, workflow, source trace, media asset, classification, residency, ownership, assignment, and purpose.
- `[x]` Define repository/service contracts for role assignments, permission grants, break-glass grants, access-control context resolution, security predicates, and security audit events.
- `[x]` Add Spring Security configuration for authentication and method-level authorization.
- `[x]` Add tenant context propagation through API, service, repository, workflow, connector, model invocation, and evidence paths.
- `[x]` Add authorization service for RBAC and ABAC checks.
- `[x]` Add repository-level tenant filters for tenant-owned entities.
- `[x]` Add redaction service for secrets, prompts, responses, raw payloads, and sensitive fields.
- `[x]` Add break-glass access model with reason, expiry, approver, and evidence events.
- `[x]` Add security evidence events for role changes, permission changes, denied access, sensitive reads, asset downloads, model config changes, connector config changes, and evidence exports.
- `[x]` Add tests for cross-tenant isolation.
- `[x]` Add tests for unauthorized reads and writes.
- `[x]` Add tests for redaction behavior.
- `[x]` Add tests for reviewer assignment-based visibility.

## Media, Drone, and Robotics Storage Rules

- `[ ]` Treat video, image sequences, drone footage, robot camera streams, sensor captures, and derived media as platform data assets.
- `[ ]` Treat Label Studio and CVAT as pluggable annotation execution nodes, not as Kanon replacement systems of record.
- `[ ]` Use Label Studio as an optional specialist editor for text, document, audio, tabular, sequence, span, and complex structured annotation execution.
- `[ ]` Use CVAT as an optional specialist vision workbench for image, video, LiDAR, drone, robotics, medical imaging, frame review, tracking, boxes, polygons, masks, and segmentation.
- `[ ]` Keep Kanon UI as the primary operational workspace; users should jump to Label Studio or CVAT only when a task needs specialized annotation interaction.
- `[ ]` Keep Kanon responsible for workflow, policy, tenant isolation, audit, evidence, model routing, task lifecycle, result normalization, and final annotation state.
- `[ ]` Support full autonomous annotation mode where Kanon can generate, validate, auto-approve, and append evidence without creating an external annotation task.
- `[ ]` Support human review mode where Kanon creates pre-annotations, pushes a review task to Label Studio or CVAT, syncs corrections, normalizes results, and appends evidence.
- `[ ]` Support mandatory human mode where Kanon blocks final approval until human signoff is synced back from Label Studio or CVAT.
- `[ ]` Ensure canonical annotation task ids originate in Kanon and external Label Studio/CVAT task ids are stored only as references.
- `[ ]` Store large binary media in object storage, not directly in PostgreSQL or MongoDB.
- `[ ]` Use S3-compatible object storage as the default abstraction, with MinIO preferred for local and on-prem deployments.
- `[ ]` Store original media, normalized/transcoded media, thumbnails, extracted frames, masks, overlays, and export files in object storage.
- `[ ]` Store structured metadata, workflow state, annotation state, review state, and asset relationships in PostgreSQL.
- `[ ]` Store immutable audit/evidence events for media ingestion, transcoding, annotation, review, approval, export, and model invocation in the evidence ledger.
- `[ ]` Store only object storage URIs, checksums, sizes, content types, and technical metadata in relational records.
- `[ ]` Require tenant-aware storage paths or buckets so tenant isolation is preserved for media assets.
- `[ ]` Include checksum verification, content type, file size, capture timestamp, source device id, mission id, and data residency metadata for media assets.
- `[ ]` For drone and robotics sources, support telemetry synchronization by timestamp, frame number, or time offset.

## Data Source Rules

- `[ ]` Model data sources by category using `docs/data-source-architecture.md`: interactive, document, communication, system, API, machine, storage, and streaming.
- `[ ]` Do not model each vendor or tool as a new core source concept unless the generic connector model cannot represent it.
- `[ ]` Capture mandatory source traceability fields for every ingestion path: source type, source category, source system, source identifier, source URI, ingestion timestamp, original payload hash, tenant id, actor, retention policy, compliance classification, data residency, consent reference, case id, correlation id, and evidence event id.
- `[ ]` Store large source payloads in object storage and structured source metadata in PostgreSQL.
- `[ ]` Create immutable evidence events for every ingestion action, retry, rejection, correction, and source-driven workflow trigger.
- `[ ]` Add idempotency support for retryable sources such as email, APIs, webhooks, databases, object storage, and event streams.
- `[ ]` Ensure manual UI entry is treated as a data source and is audited like external ingestion.

## Milestone 4: Data Source Connectors

- `[x]` Implement source architecture according to `docs/data-source-architecture.md`.
- `[x]` Define source category and source type enums.
- `[x]` Define `DataSourceConnector`, `DataSourceConnectorRegistry`, `IngestionRequest`, `IngestionResult`, `SourceDescriptor`, `SourcePayload`, and `SourceTrace` contracts.
- `[x]` Add Flyway migrations V16–V20 for source descriptors, source traces, ingestion batches, connector configuration, and connector health with audit columns.
- `[x]` Ensure source persistence code writes audit columns on insert and update (Postgres repos).
- `[x]` Implement Upload Connector for PDFs, DOCX, XLSX, CSV, scanned images, ZIP uploads, and user-uploaded forms.
- `[x]` Implement Manual Entry Connector for manual case creation, forms, notes, corrections, approvals, and rejections.
- `[x]` Implement REST/Webhook Connector with idempotency key support.
- `[x]` Implement Object Storage Connector for S3-compatible storage and MinIO.
- `[x]` Implement Email Connector for IMAP inboxes, shared mailboxes, forwarded messages, email body text, and attachments.
- `[x]` Implement Database Import Connector for PostgreSQL first.
- `[~]` Add batch import, checkpoint, retry with backoff, and failed-ingestion state for scalable connector execution (policies defined, runtime engine TBD).
- `[~]` Add connector-level rate limits and backpressure controls (policies defined, runtime enforcement TBD).
- `[x]` Add connector health checks and last-ingestion status tracking (health returned from connector, tracked via IngestionOrchestrationService).
- `[ ]` Add connector lag, throughput, failure rate, and retry count metrics.
- `[x]` Add evidence events for source ingestion via `IngestionOrchestrationService` (SOURCE_INGESTED, SOURCE_DUPLICATE, SOURCE_REJECTED, SOURCE_FAILED).
- `[x]` Add `INGESTION_PERFORMED` and `INGESTION_FAILED` security event types.
- `[x]` Enforce connector permissions via `@Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})` on API endpoints.
- `[x]` Add `IngestionOrchestrationService` as a `@Component` service wiring connector → source descriptor save → connector ingest → evidence append → security audit → health update.
- `[x]` Add `IngestionController` with `POST /api/sources/ingest`, `GET /api/sources/traces`, `GET /api/sources/traces/{traceId}` endpoints.
- `[x]` Wire in-memory repository beans (`SourceTraceRepository`, `SourceDescriptorRepository`, `IngestionBatchRepository`, `ConnectorConfigurationRepository`, `ConnectorHealthRepository`) in `KanonServiceAutoConfiguration` with `@ConditionalOnMissingBean`.
- `[ ]` Ensure source trace views separate metadata access from payload access.
- `[x]` Add tests for mandatory ingestion orchestration: connector routing, source descriptor save, trace save, evidence event append, security audit publish, health update, and failure paths.
- `[ ]` Add tests for idempotency behavior in email, REST/webhook, database import, and object storage connectors.
- `[ ]` Add security tests for connector access, source trace access, and payload download access.
- `[ ]` Document deferred connectors for SAP, DATEV, Workday, Salesforce, ServiceNow, Kafka, RabbitMQ, SharePoint, Google Drive, OneDrive, Dropbox, LiDAR, CCTV, and DICOM.

## Milestone 5: AI Routing Adapters

- `[ ]` Implement LLM services according to `docs/llm-service-configuration.md`.
- `[ ]` Define `LlmService`, `LlmServiceRegistry`, `LlmServiceFactory`, `ModelInvocationService`, and model profile repository contracts.
- `[ ]` Expose LLM backends as Spring-managed beans with configurable properties.
- `[ ]` Support setter/getter-bound configuration objects for model service properties where runtime/admin configuration requires mutable configuration.
- `[ ]` Support constructor injection for required service dependencies and setter/getter injection for configurable model properties.
- `[ ]` Extend `ModelProfile` to include provider, model id, task capabilities, cost class, latency class, locality, and compliance tags.
- `[ ]` Extend `ModelProfile` to support local server models and API-backed models.
- `[ ]` Add PostgreSQL persistence for model profiles with Flyway migrations and audit columns.
- `[ ]` Ensure model profile persistence code writes audit columns on insert and update.
- `[ ]` Add a model invocation port separate from route resolution.
- `[ ]` Add local OpenAI-compatible LLM adapter for server-hosted models such as Ollama, llama.cpp server, or vLLM-compatible endpoints.
- `[ ]` Add API OpenAI-compatible LLM adapter for configurable external/managed APIs.
- `[ ]` Add LangChain4j adapter for chat or structured extraction calls after the common LLM service contract is stable.
- `[ ]` Add Spring AI adapter if a second abstraction is still needed after LangChain4j and the common LLM service contract are evaluated.
- `[ ]` Add fallback execution behavior when primary model invocation fails.
- `[ ]` Record model route and invocation metadata as evidence events.
- `[ ]` Record model profile create/update/enable/disable and health-check events as evidence events.
- `[ ]` Add model health check support for local server models and API models.
- `[ ]` Add secret-reference handling so API keys are referenced, encrypted, or stored outside plain UI-visible fields.
- `[ ]` Add model timeout, retry, rate limit, and concurrency limit enforcement.
- `[ ]` Add async execution boundary for expensive model invocations.
- `[ ]` Add model invocation latency, error rate, fallback rate, and queue depth metrics.
- `[ ]` Enforce model configuration permissions for create, update, enable, disable, test connection, and dry-run routing.
- `[ ]` Enforce prompt/response redaction based on role, tenant policy, and data classification.
- `[ ]` Add tests for routing decisions by source category, asset type, task type, tenant preference, and compliance constraints.
- `[ ]` Add adapter tests with fake model clients to avoid network-dependent unit tests.
- `[ ]` Add security tests for model profile visibility, secret redaction, and model invocation summaries.

## Milestone 6: Workflow Planner / GOAP

- `[ ]` Implement only the MVP workflow types documented in `docs/mvp-agents-workflows.md`: Data Ingestion, Annotation / Extraction, and Human Review / Approval.
- `[ ]` Model workflow goals, actions, preconditions, effects, and policy constraints.
- `[ ]` Map existing static workflow steps to first-class workflow action definitions.
- `[ ]` Add an Embabel planner adapter behind `WorkflowPlanner`.
- `[ ]` Add fallback to `DefaultWorkflowPlanner` when GOAP planning cannot produce a valid plan.
- `[ ]` Include source category, source trace, tenant, domain, policy decision, and task descriptor data in planner input.
- `[ ]` Include access-control context in planner input so generated workflow actions respect tenant, role, assignment, and classification constraints.
- `[ ]` Ensure workflow plans are resumable, retryable where safe, and do not require large payloads in orchestration state.
- `[ ]` Emit evidence for workflow plan creation and planner fallback decisions.
- `[ ]` Add planner tests for accounting and HR scenarios.

## Milestone 7: Agent Runtime and Evidence

- `[ ]` Implement only the MVP agent types documented in `docs/mvp-agents-workflows.md`: Ingestion Agent, Policy Agent, Extraction / Annotation Agent, and Review Orchestration Agent.
- `[ ]` Implement Evidence Ledger as a shared service for MVP, not as a separate autonomous agent unless the taxonomy document is updated.
- `[ ]` Add agent persistence/UI fields from `docs/mvp-agents-workflows.md` before building the agent management UI.
- `[ ]` Add workflow persistence/UI fields from `docs/mvp-agents-workflows.md` before building the workflow management UI.
- `[ ]` Define agent execution lifecycle events: requested, started, source ingested, model invoked, completed, failed, reviewed, approved, exported.
- `[ ]` Add correlation ids for tenant id, case id, source trace id, workflow id, agent id, and model invocation id.
- `[ ]` Enforce agent execution permissions and assignment visibility for agent outputs.
- `[ ]` Keep agent execution stateless where possible and use queues or executor pools for heavy work.
- `[ ]` Add retry, backoff, timeout, and concurrency controls for agent execution.
- `[ ]` Ensure every agent execution appends evidence events.
- `[ ]` Add immutable revision behavior for annotation changes.
- `[ ]` Add replay/read APIs for evidence by tenant and case.
- `[ ]` Enforce evidence visibility rules so users see only authorized evidence records and redacted sensitive fields.
- `[ ]` Add tests for append-only evidence behavior and annotation revision history.
- `[ ]` Add security tests for evidence visibility and sensitive evidence redaction.

## Milestone 8: API and Vaadin Command Center

- `[~]` Add API endpoints for tenant config, domain config, workflow plans, evidence events, and model routes.
- `[x]` Add read-only admin API endpoints for bootstrap configuration packs and active configuration versions.
- `[ ]` Ensure all list/read APIs support pagination, sorting, filtering, and tenant-scoped predicates.
- `[ ]` Add admin API endpoints for connector configuration, connector health, source traces, ingestion batches, and ingestion retry.
- `[ ]` Add admin API endpoints for model profile create, update, enable, disable, test connection, health status, and dry-run routing.
- `[ ]` Add source connector management UI using `docs/data-source-architecture.md`.
- `[ ]` Add source trace and ingestion batch views.
- `[ ]` Add connector configuration forms for Upload, Email, Manual Entry, REST/Webhook, Database Import, and Object Storage connectors.
- `[ ]` Add connector health grid columns for connector type, category, enabled state, last ingestion time, last success, last failure, failure reason, and retry count.
- `[ ]` Add source trace grid columns for source category, source type, source system, source identifier, tenant, actor, ingestion timestamp, hash, compliance classification, retention policy, and evidence link.
- `[ ]` Add user, role, permission, and assignment management UI using `docs/security-access-control.md`.
- `[ ]` Add platform administration UI for super admin to create tenants, organizations, workspaces, tenant admins, organization admins, memberships, roles, and platform configuration.
- `[ ]` Add organization administration UI for organization admins to create allowed organizations, workspaces, workspace users, workspace memberships, and organization-level configuration.
- `[ ]` Keep all tenant, organization, workspace, user, membership, role, and governance configuration views under the Administration parent menu.
- `[x]` Scope Administration UI lists, forms, lookups, actions, and API results by platform, tenant, organization, and workspace permissions; Administration menu access must not grant global access.
- `[x]` Make tenant-level master data read-only for Tenant Admin unless explicit tenant edit permission is present.
- `[x]` Make organization-level master data read-only for Organization Admin unless explicit organization edit permission is present.
- `[x]` Add backend API authorization checks for tenant, organization, workspace, user, membership, role, and configuration administration mutations.
- `[ ]` Add access-control badges and visibility hints for tenant, domain, assignment, classification, and payload access.
- `[x]` Hide unavailable menus, submenus, views, grid actions, and primary actions based on permissions while still enforcing access on APIs/services.
- `[x]` Add redacted display components for sensitive fields, prompts, responses, payload previews, and secrets.
- `[x]` Add security/audit views for denied access, sensitive reads, role changes, permission changes, and break-glass usage.
- `[x]` Add agent management UI using the fixed MVP agent list from `docs/mvp-agents-workflows.md`.
- `[x]` Add workflow management UI using the fixed MVP workflow list from `docs/mvp-agents-workflows.md`.
- `[ ]` Add model configuration UI using `docs/llm-service-configuration.md`.
- `[ ]` Allow admins to configure local server models and API-backed models from the UI.
- `[ ]` Add model profile forms with backend-specific validation for local server and API models.
- `[ ]` Add model profile grid columns for backend type, provider, model name, enabled state, health status, cost class, latency class, locality, fallback profile, and last failure reason.
- `[ ]` Add model test connection and dry-run routing actions in the admin UI.
- `[ ]` Ensure credential fields store secret references instead of raw visible API keys.
- `[ ]` Ensure model configuration UI honors Model Operator, Tenant Admin, and Platform Admin permissions.
- `[x]` Include the full MVP agent fields in the UI before marking agent management complete.
- `[ ]` Include the full MVP workflow fields in the UI before marking workflow management complete.
- `[ ]` Use meaningful icons for every main menu item, submenu item, view header, primary action, status indicator, and important grid column.
- `[ ]` Keep icon usage semantically consistent across the app, such as agent icons for agents, route/model icons for AI routing, shield/check icons for policy, database/storage icons for persistence, timeline/history icons for evidence, and video/device icons for media sources.
- `[ ]` Add reactive status indicators for source connector state, ingestion state, access-control state, agent state, workflow state, review state, policy decision, model route, evidence status, storage status, and media processing status.
- `[ ]` Ensure grids use user-friendly columns with clear labels, meaningful icons where they reduce scanning time, status badges, timestamps, owner/tenant context, and row-level actions.
- `[ ]` Ensure all Vaadin grids use lazy data providers and never load unbounded datasets into memory.
- `[ ]` Use aggregate queries for dashboard summary cards instead of loading raw records into UI memory.
- `[ ]` Load large payload previews only on authorized demand and avoid previewing large media by default.
- `[ ]` Ensure command center views are agentic and reactive: users should see what was ingested, what is running, what needs review, what changed, which source/model/agent acted, and what evidence was recorded.
- `[ ]` Make Kanon UI the operational workspace for case lifecycle, task routing, autonomy state, approval flow, audit trail, model routing, escalation, and evidence history.
- `[x]` Add Vaadin workflow board backed by workflow plan/read APIs.
- `[x]` Add human task inbox for review, approve, and escalate states.
- `[x]` Add evidence explorer with tenant/case filtering.
- `[x]` Add model visibility panel showing selected model, fallback model, and routing rationale.
- `[x]` Add before/after annotation diff view.
- `[x]` Localize Vaadin component text and wire user logout from the application shell.
- `[x]` Remove the view-level language selector, keep tenant/profile language switching, and verify logout wiring.
- `[ ]` Add lightweight document field correction UI for extracted-field review in Kanon.
- `[ ]` Add linked or embedded specialist workbench handoff for Label Studio and CVAT where rich annotation editing is required.
- `[ ]` Add external annotation sync status and final review/approval/export views in Kanon after results are normalized.
- `[ ]` Add UI smoke tests for the command center route.
- `[x]` Add UI tests that verify unauthorized menus/actions are hidden and unauthorized routes are denied.

## Milestone 9: Video, Drone, and Robotics Annotation

- `[ ]` Keep video, drone, and robotics behavior under the existing MVP agents and workflows instead of adding separate Drone, Robot, or Video agents/workflows.
- `[ ]` Define a `media_asset` persistence model for videos, image sequences, frame captures, and derived media.
- `[ ]` Add Flyway migration for `media_asset` with audit columns, tenant id, source type, storage URI, checksum, content type, size, duration, frame rate, resolution, capture timestamp, data residency, and version.
- `[ ]` Add persistence code that writes audit columns for `media_asset` inserts and updates.
- `[ ]` Define source types such as upload, drone, robot, camera stream, sensor stream, and external system import.
- `[ ]` Define video annotation records that reference media assets by id and support labels over frame ranges and time ranges.
- `[x]` Define annotation execution node SPI for creating external annotation tasks and fetching results.
- `[x]` Add Label Studio annotation node adapter for text, document, audio, and tabular annotation tasks.
- `[x]` Add CVAT annotation node adapter for image, video, LiDAR, drone, robotics, and medical imaging annotation tasks.
- `[x]` Add annotation task sync service to push tasks, pull results, map annotations, update Kanon state, and append evidence.
- `[x]` Add annotation execution mode policy for auto, human review, and mandatory human signoff based on confidence, risk, tenant policy, asset type, and domain.
- `[ ]` Add persistence for external annotation task ids, annotation node ids, sync status, sync failures, and retry metadata.
- `[ ]` Persist normalized external annotation results as Kanon annotation records and corrections as immutable revisions.
- `[ ]` Support annotation geometry for bounding boxes, polygons, masks, keypoints, tracks, and scene-level labels.
- `[ ]` Add `robot_telemetry` or equivalent telemetry records linked to media assets by timestamp, frame number, or time offset.
- `[ ]` Capture drone-specific metadata such as latitude, longitude, altitude, heading, speed, mission id, and device id when available.
- `[x]` Add object storage adapter interface for put, get, delete marker, metadata lookup, presigned read URL, and checksum verification.
- `[ ]` Add MinIO/S3-compatible object storage adapter implementation.
- `[ ]` Add multipart/direct upload support for large media assets where possible.
- `[ ]` Add async media processing for normalization, thumbnail generation, frame extraction, and export.
- `[ ]` Add media processing queue depth, processing latency, and failure rate metrics.
- `[ ]` Add evidence events for media uploaded, media normalized, frame extracted, annotation created, annotation corrected, track corrected, human reviewed, human approved, model invoked, and export generated.
- `[ ]` Add evidence events for annotation execution mode selected, auto-approved, external annotation task created, pushed, synced, failed, corrected, reviewed, approved, rejected, retried, and cancelled.
- `[ ]` Add API endpoints for media registration, metadata lookup, upload handoff, annotation reads, annotation writes, telemetry reads, and evidence lookup by media asset.
- `[ ]` Add API endpoints for annotation node configuration, annotation task creation, sync status, and manual sync retry.
- `[ ]` Enforce separate permissions for media metadata view, payload view/download, annotation view, annotation edit, telemetry view, and evidence view.
- `[ ]` Add command center views for annotation node status, external task links, sync state, lightweight review, video evidence playback, frame/time navigation, read-only annotation overlays, telemetry context, and evidence history.
- `[ ]` Keep deep vision editing in CVAT for boxes, tracks, polygons, masks, segmentation, and frame-by-frame correction instead of rebuilding CVAT in Kanon v1.
- `[ ]` Keep advanced Label Studio patterns available for sequence labeling, span annotation, complex structured labeling, audio, and time-series workflows while Kanon handles basic field review.
- `[ ]` Add tests for media metadata persistence, audit column persistence, object storage adapter behavior, annotation node behavior, sync mapping, and evidence event creation.
- `[ ]` Add security tests for media metadata access, payload access, telemetry access, annotation access, and external annotation task access.
- `[ ]` Document local MinIO setup and required object storage configuration.
- `[ ]` Document local Label Studio and CVAT setup and required annotation node configuration.

## Milestone 10: Bootstrap and Operations

- `[x]` Wire configuration loading into `kanon-bootstrap`.
- `[x]` Add Spring profiles for local PostgreSQL/MongoDB and production-like cloud configuration.
- `[x]` Add Spring profiles and configuration properties for local MinIO and production S3-compatible object storage.
- `[ ]` Add Spring profiles and configuration properties for V1 connectors: upload, email, REST/webhook, database import, object storage, and manual entry.
- `[ ]` Add Spring profiles and configuration properties for authentication, authorization, development users, and production identity-provider integration.
- `[ ]` Add OpenTelemetry tracing after workflow, model invocation, and evidence flows are connected.
- `[ ]` Add health checks for source connectors, database adapters, object storage adapters, model adapters, security configuration, scalability-critical queues/executors, and configuration loading.
- `[ ]` Add metrics for ingestion throughput, connector lag, workflow queue depth, model latency, model fallback rate, evidence write rate, database query latency, object storage latency, UI endpoint latency, and authorization denial rate.
- `[~]` Add a README quickstart for running the bootstrap app.
- `[ ]` Add CI build instructions once the repository is under version control.

## Milestone 11: Dataset Curation and Management

Spec: `SPEC-016: Dataset Curation and Management`

- `[ ]` Add `kanon-dataset` module for dataset curation, versioning, split management, and export contracts.
- `[ ]` Define typed contracts for `DatasetDefinition`, `DatasetVersion`, `DatasetSplit`, `DatasetRecord`, `CurationRule`, and `ExportFormat`.
- `[ ]` Add dataset persistence with Flyway migration, audit columns, tenant id, source annotation set, curation rules, split ratios, label distribution, and data residency.
- `[ ]] Add curation rule engine to filter/select annotation records by confidence, review status, domain, tenant, policy, and quality criteria.
- `[ ]` Support train/val/test split strategies: random, stratified, temporal, and domain-aware.
- `[ ]` Add dataset versioning: each curation produces an immutable version snapshot.
- `[ ]` Add export adapters for JSONL, Parquet, Hugging Face Datasets format, and TFRecord.
- `[ ]` Stream large dataset exports directly to object storage instead of assembling in memory.
- `[ ]` Store export artifacts in object storage with tenant-aware paths and checksum verification.
- `[ ]` Add dataset metadata calculation: total records, per-split counts, label distribution, class balance, and data residency.
- `[ ]` Add evidence events for dataset version created, curated, exported, and deleted.
- `[ ]` Add API endpoints for dataset list, version history, curation trigger, export trigger, and metadata retrieval.
- `[ ]` Add command center views for dataset management: list, version history, curation configuration, and export status.
- `[ ]` Enforce dataset permissions: view, curate, export, delete.
- `[ ]` Add tests for curation rules, split strategies, dataset version immutability, and export format correctness.
- `[ ]` Add security tests for cross-tenant dataset access and export permission enforcement.

## Milestone 12: Model Training Orchestration

Spec: `SPEC-017: Model Training Orchestration`

- `[ ]` Add `kanon-training` module for training job contracts, compute backend abstraction, and job lifecycle management.
- `[ ]` Define typed contracts for `TrainingJob`, `TrainingRun`, `ComputeBackend`, `HyperParameterConfig`, `Checkpoint`, and `TrainingMetrics`.
- `[ ]` Add training job lifecycle: requested, queued, starting, running, checkpointing, completed, failed, cancelled.
- `[ ]` Add compute backend SPI with implementations for local GPU process, Kubernetes batch job, and cloud ML service.
- `[ ]` Add local GPU compute backend adapter for development and on-prem training.
- `[ ]` Add Kubernetes batch job compute backend adapter for GPU cluster training.
- `[ ]` Add Vertex AI compute backend adapter.
- `[ ]` Add SageMaker compute backend adapter.
- `[ ]` Add Azure ML compute backend adapter.
- `[ ]` Add training job persistence with Flyway migration, audit columns, tenant id, dataset version ref, model config, hyperparameters, compute backend ref, and status.
- `[ ]` Add checkpoint storage in object storage with references in the training job record.
- `[ ]` Add training metrics ingestion and progress tracking via evidence events.
- `[ ]` Add evidence events for training job requested, started, checkpointed, completed, failed, and cancelled.
- `[ ]` Add API endpoints for training job create, list, status, cancel, metrics, and log retrieval.
- `[ ]` Add command center views for training job list, detail, status, live metrics, and log viewer.
- `[ ]` Enforce training permissions: create, view, cancel, view-metrics.
- `[ ]` Add tests for training job lifecycle, compute backend SPI, checkpoint behavior, and failure recovery.
- `[ ]` Add security tests for cross-tenant training job isolation and permission enforcement.

## Milestone 13: Model Registry and Evaluation

Spec: `SPEC-018: Model Registry and Versioning`, `SPEC-019: Model Evaluation and Testing`, `SPEC-021: Model Serving and Deployment`

- `[ ]` Add `kanon-model-registry` module for model versioning, lineage, evaluation, and deployment contracts.
- `[ ]` Define typed contracts for `ModelEntry`, `ModelVersion`, `ModelArtifact`, `EvaluationRun`, `EvaluationMetric`, `DeploymentTarget`, and `DeploymentConfig`.
- `[ ]` Add structured model registry persistence with Flyway migration, audit columns, model name, version, framework, task type, domain, artifact URI, training run id, dataset version id, hyperparameters, evaluation metrics, compliance tags, and lifecycle stage.
- `[ ]` Support model lifecycle stages: development, staging, production, deprecated, archived.
- `[ ]` Add model lineage tracking from source traces through annotation evidence, dataset version, training run, and evaluation results.
- `[ ]` Add evaluation job runner that executes a model against a held-out test dataset and computes metrics.
- `[ ]` Support evaluation metrics: accuracy, precision, recall, F1, ROC-AUC, mean average precision, BLEU, ROUGE, perplexity, and custom domain metrics.
- `[ ]` Store evaluation results (metrics, per-class breakdown, confusion matrix, failure case samples) as part of the model version record.
- `[ ]` Add model comparison views: side-by-side metric comparison across versions on the same test set.
- `[ ]` Add configurable promotion gates requiring minimum evaluation thresholds for stage promotion.
- `[ ]` Add deployment target SPI for registering serving endpoints (local inference, Triton, ONNX, cloud API).
- `[ ]` Add deployment health checks that verify endpoint responsiveness and output format correctness.
- `[ ]` Support blue/green and canary deployment strategies through deployment configuration.
- `[ ]` Add model rollback support with evidence recording.
- `[ ]` Register production-deployed models as available `ModelProfile` entries in the Model Router.
- `[ ]` Add evidence events for model registered, version created, evaluated, promoted, deployed, rolled back, deprecated, and archived.
- `[ ]` Add API endpoints for model registry CRUD, evaluation trigger, comparison, deployment, rollback, and health status.
- `[ ]` Add command center views for model registry list, version detail, lineage graph, evaluation results, comparison matrix, deployment status, and promotion workflow.
- `[ ]` Enforce model registry permissions: view, register, evaluate, promote, deploy, deprecate, archive.
- `[ ]` Ensure serving endpoint credentials are stored as secret references.
- `[ ]` Add tests for model versioning, lineage tracking, evaluation metric computation, deployment lifecycle, and promotion gates.
- `[ ]` Add security tests for model registry access control, cross-tenant isolation, and production model protection.

## Milestone 14: Active Learning and Continuous Training

Spec: `SPEC-020: Active Learning and Continuous Training`

- `[ ]` Add active learning strategy engine in `kanon-training` with pluggable strategies.
- `[ ]` Implement uncertainty sampling strategy: select records where model confidence is below a configurable threshold.
- `[ ]` Implement diversity sampling strategy: select records that maximize coverage of the feature space.
- `[ ]` Implement query-by-committee strategy: select records with highest disagreement among an ensemble of models.
- `[ ]` Implement policy-defined strategy: tenant-configured rules for record selection.
- `[ ]` Add active learning cycle trigger: configurable schedule, min-new-records threshold, or manual.
- `[ ]` Link selected records from active learning back to the Review/Approval workflow for re-annotation.
- `[ ]` Add evidence events for active learning cycle started, records selected, review tasks created, dataset updated, retraining triggered, evaluation complete, and promotion decision.
- `[ ]` After re-annotation, increment dataset version automatically or on approval.
- `[ ]` Trigger retraining pipeline automatically when updated dataset meets policy criteria.
- `[ ]` Compare retrained model evaluation against current production model.
- `[ ]` Auto-promote retrained model to production if it meets all promotion criteria.
- `[ ]` Block promotion and notify admins if retrained model regresses on key metrics.
- `[ ]` Add API endpoints for active learning strategy configuration, cycle trigger, cycle history, and promotion review.
- `[ ]` Add command center views for active learning dashboard: cycle status, records selected, dataset impact, model comparison, and promotion queue.
- `[ ]` Enforce active learning permissions: configure strategy, trigger cycle, review promotion.
- `[ ]` Add tests for each active learning strategy, cycle lifecycle, automatic retraining trigger, and promotion/rejection logic.
- `[ ]` Add security tests for cross-tenant active learning configuration isolation.

## Milestone 15: Application Testing Manual

Spec: `docs/application-testing-manual.md`

- `[x]` Create `docs/application-testing-manual.md` covering manual and automated testing procedures.
- `[x]` Document manual smoke tests for every major platform capability.
- `[x]` Document integration test setup and execution procedures.
- `[x]` Document UI test execution and coverage expectations.
- `[x]` Document performance and scalability test scenarios.
- `[x]` Document training pipeline testing procedures (dataset, training, registry, active learning).
- `[x]` Document security testing procedures for access control, tenant isolation, and data protection.

## Future Complex Tasks

Spec: `SPEC-011: Annotation Intake and Bulk Upload`

- `[ ]` Add Kanon intake workspace for single-entry case/task creation before dispatch to any external annotation tool.
- `[ ]` Add single-entry form flow for tenant/domain selection, source metadata, policy context, attachments, and initial review mode.
- `[ ]` Add bulk upload UI for document, image, video, tabular, and dataset batches.
- `[ ]` Add upload validation preview showing accepted, rejected, duplicate, oversized, unsupported, and policy-blocked items before submission.
- `[ ]` Add object-storage handoff for large intake payloads so files do not move through UI/request memory.
- `[ ]` Add batch progress UI for upload, validation, ingestion, pre-annotation, external dispatch, sync, review, approval, and failure states.
- `[ ]` Add retry/cancel controls for failed intake items and failed external annotation dispatch.
- `[ ]` Route each intake item to Kanon-native review, Label Studio, CVAT, or auto-approval based on source type, asset type, policy, confidence, and configured annotation nodes.
- `[ ]` Create canonical Kanon case/task ids before creating Label Studio or CVAT tasks.
- `[ ]` Add external tool launch links from intake/task views after dispatch, while keeping final review, approval, export, and evidence in Kanon.
- `[ ]` Add evidence events for single-entry submission, bulk batch submission, validation failure, upload completed, external dispatch, retry, cancellation, sync completed, and sync failed.
- `[ ]` Add security tests for upload permission, payload visibility, tenant isolation, external task link visibility, and bulk upload partial-failure handling.

Spec: `SPEC-014: External Annotation Node Configuration`

- `[ ]` Add `ExternalAnnotationNode` entity to `kanon-annotation` module with support for provider types (`LABEL_STUDIO`, `CVAT`), base URLs, and tenant-id isolation.
- `[ ]` Add `ExternalAnnotationNodeRepository` with tenant-aware filtering and default-deny access controls.
- `[ ]` Add secret reference handling for external API Keys/Tokens to ensure raw credentials are never stored in plain text or exposed in UI logs.
- `[ ]` Add `AnnotationNodeVerificationService` using Java 25 `StructuredTaskScope` to execute parallel "Dry Run" checks (DNS resolve, Ping, and API Authentication).
- `[ ]` Add Label Studio provider implementation to verify connectivity via `/api/projects/` and retrieve system version metadata.
- `[ ]` Add CVAT provider implementation to verify connectivity via `/api/users/self` and retrieve system version metadata.
- `[ ]` Add Node Configuration UI in the Kanon Command Center for listing, creating, and editing external annotation nodes.
- `[ ]` Add "Test Connection" button to the UI that triggers a real-time dry run with a step-by-step progress indicator.
- `[ ]` Add node status management to mark configurations as `ACTIVE`, `OFFLINE`, or `UNAUTHORIZED` based on the latest verification result.
- `[ ]` Add evidence event emission for `ANNOTATION_NODE_CREATED`, `ANNOTATION_NODE_UPDATED`, and `ANNOTATION_NODE_TESTED` (including result/latency/version).
- `[ ]` Add validation logic to prevent deleting a node that is currently linked to active, non-synced workflow tasks.
- `[ ]` Add security tests to ensure `Tenant Admin` cannot see or test the connection of an `ExternalAnnotationNode` belonging to a different tenant.

Spec: `SPEC-015: Federated, Policy-Governed Orchestration`

- `[ ]` Add `FederatedNode` entity to `kanon-core` to manage remote worker registries, including geographic residency tags and compute capability profiles.
- `[ ]` Implement **Shard Manager** logic to partition large-scale datasets into policy-compliant "Work Shards" based on tenant constraints and data sensitivity.
- `[ ]` Add **Data Residency Guardrails** to the `ModelRouter` to ensure data payloads never leave defined sovereign boundaries (e.g., EU-restricted data stays on EU nodes).
- `[ ]` Add **Training Context Packager** to create immutable snapshots of normalized annotation records, model parameters, and policy versions for distributed training handoff.
- `[ ]` Implement **Global-Local Sync Service** using Java 25 `StructuredTaskScope` to orchestrate multi-node heartbeats and asynchronous progress updates.
- `[ ]` Add **Federated Lock Manager** to `kanon-workflow` to prevent "split-brain" scenarios where multiple distributed nodes attempt to modify the same data shard.
- `[ ]` Add **Model Lineage Tracker** in the `Evidence Ledger` to link generated model weights (stored in Object Storage) to the exact evidence audit trail used for training.
- `[ ]` Add **Federated Command Dashboard** in Vaadin to monitor global node health, shard completion rates, and cross-region training job status.
- `[ ]` Add **Training Result Normalizer** to ingest model metrics (accuracy, loss, latency) from distributed nodes and record them as `MODEL_TRAINED_EVIDENCE`.
- `[ ]` Add evidence events for `FEDERATED_SHARD_DISPATCH`, `DISTRIBUTED_TRAINING_START`, `REMOTE_SYNC_HEARTBEAT`, and `WEIGHTS_CERTIFIED`.
- `[ ]` Add security tests to verify that a Federated Node with "Low Trust" or "Restricted Domain" tags cannot request high-sensitivity data shards.
- `[ ]` Implement **Failure Recovery Logic** to automatically re-queue shards from dropped or timed-out nodes back into the Global Workflow pool.

## Near-Term Task Order

1. `[x]` Add `kanon-config` with typed configuration contracts.
2. `[x]` Add YAML template loader, validation service, in-memory registry, and sample Accounting/HR templates.
3. `[x]` Add active configuration version and seed/import contracts for later PostgreSQL persistence.
4. `[x]` Wire bootstrap startup loading for Accounting/HR packs and expose admin/read APIs for loaded configuration.
5. `[x]` Convert scalability rules from `docs/scalability-architecture.md` into persistence, API, connector, workflow, model, and UI contracts.
6. `[x]` Convert security roles, permissions, and access-control dimensions from `docs/security-access-control.md` into persistence and service contracts.
7. `[x]` Convert data source traceability fields from `docs/data-source-architecture.md` into persistence contracts.
8. `[x]` Convert MVP agent and workflow UI fields from `docs/mvp-agents-workflows.md` into persistence contracts.
9. `[~]` Add repository ports with PostgreSQL/MongoDB runtime implementations and unit-test fakes only where needed.
10. `[x]` Add PostgreSQL state adapter.
11. `[x]` Add MongoDB evidence adapter.
12. `[x]` Add object storage abstraction and local MinIO configuration.
13. `[x]` Add V1 connector contracts and source trace model.
14. `[x]` Implement Upload, Manual Entry, REST/Webhook, Object Storage, Email, and PostgreSQL Database Import connectors.
15. `[x]` Add media asset and video annotation persistence contracts.
16. `[x]` Expand model profile and model invocation contracts.
17. `[x]` Add LangChain4j adapter and fake-client tests.
18. `[x]` Model workflow actions/goals for GOAP.
19. `[x]` Add Embabel planner adapter.
20. `[x]` Expand API read endpoints.
21. `[x]` Add annotation execution node SPI in `kanon-annotation`.
22. `[x]` Add Label Studio annotation node adapter.
23. `[x]` Add CVAT annotation node adapter.
24. `[x]` Add annotation execution mode policy.
25. `[x]` Add annotation task sync service.
26. `[x]` Build read-only Vaadin command center views.
27. `[x]` Add mutation workflows for review, approval, escalation, and export.
28. `[x]` Add before/after annotation diff view.
29. `[x]` Add redacted display components for sensitive fields, prompts, responses, payload previews, and secrets.
30. `[x]` Add backend API authorization checks for tenant, organization, workspace, user, membership, role, and configuration administration mutations.
31. `[x]` Scope Administration UI lists, forms, lookups, actions, and API results by platform, tenant, organization, workspace, and permission.
32. `[x]` Make tenant-level and organization-level master data read-only for Tenant Admin and Organization Admin unless explicit edit permission is present.
33. `[x]` Hide unavailable Administration menus, submenus, views, grid actions, and primary actions based on permissions while keeping backend enforcement authoritative.
34. `[x]` Add security/audit views for denied access, sensitive reads, role changes, permission changes, and break-glass usage.
35. `[x]` Add UI tests that verify unauthorized menus/actions are hidden and unauthorized routes are denied.
36. `[x]` Add agent management UI using the fixed MVP agent list from `docs/mvp-agents-workflows.md`.
37. `[x]` Add workflow management UI using the fixed MVP workflow list from `docs/mvp-agents-workflows.md`.
38. `[x]` Add model configuration UI using `docs/llm-service-configuration.md`.
39. `[x]` Allow admins to configure local server models and API-backed models from the UI using secret references instead of raw visible API keys.
40. `[x]` Add model test connection and dry-run routing actions in the admin UI.
41. `[x]` Implement `SPEC-014: External Annotation Node Configuration` (entity/repository, secret references, verification service, Label Studio/CVAT checks, node config UI + test connection workflow, status management, evidence events, delete guardrails, and cross-tenant security tests).
42. `[x]` Add lightweight document field correction UI for extracted-field review in Kanon.
43. `[x]` Add linked or embedded specialist workbench handoff for Label Studio and CVAT where rich annotation editing is required.
44. `[x]` Add external annotation sync status and final review/approval/export views in Kanon after results are normalized.
45. `[ ]` Add `FederatedNode` entity to `kanon-core` to manage remote worker registries, including geographic residency tags and compute capability profiles.
46. `[ ]` Implement Shard Manager logic to partition datasets into policy-compliant work shards based on tenant constraints and data sensitivity.
47. `[ ]` Add data residency guardrails to `ModelRouter` so data payloads never leave defined sovereign boundaries.
48. `[ ]` Add Training Context Packager to create immutable snapshots of normalized annotation records, model parameters, and policy versions for distributed training handoff.
49. `[ ]` Implement Global-Local Sync Service using Java 25 `StructuredTaskScope` for multi-node heartbeats and async progress updates.
50. `[ ]` Add Federated Lock Manager in `kanon-workflow` to prevent split-brain updates on the same shard.
51. `[ ]` Add Model Lineage Tracker in the Evidence Ledger to link generated model weights (object storage) to their evidence audit trail.
52. `[ ]` Add Training Result Normalizer to ingest distributed-node model metrics (accuracy, loss, latency) and record `MODEL_TRAINED_EVIDENCE`.
53. `[ ]` Add evidence events for `FEDERATED_SHARD_DISPATCH`, `DISTRIBUTED_TRAINING_START`, `REMOTE_SYNC_HEARTBEAT`, and `WEIGHTS_CERTIFIED`.
54. `[ ]` Add security tests to verify low-trust/restricted federated nodes cannot request high-sensitivity data shards.
55. `[ ]` Implement failure recovery to re-queue shards from dropped or timed-out nodes back to the global workflow pool.
56. `[ ]` Add Federated Command Dashboard in Vaadin for node health, shard completion rates, and cross-region training status.
57. `[ ]` Add UI smoke tests for the command center route.
58. `[ ]` Add persistence for external annotation task ids, annotation node ids, sync status, sync failures, and retry metadata.
59. `[ ]` Persist normalized external annotation results as Kanon annotation records and corrections as immutable revisions.
60. `[ ]` Add evidence events for annotation execution mode selected, auto-approved, external annotation task created, pushed, synced, failed, corrected, reviewed, approved, rejected, retried, and cancelled.
61. `[ ]` Add API endpoints for annotation node configuration, annotation task creation, sync status, and manual sync retry.
62. `[ ]` Add MinIO/S3-compatible object storage adapter implementation.
63. `[ ]` Add `media_asset` Flyway migration and persistence code with audit columns, tenant id, source type, storage URI, checksum, content type, size, duration, frame rate, resolution, capture timestamp, data residency, and version.
64. `[ ]` Add multipart/direct upload support for large media assets where possible.
65. `[ ]` Add async media processing for normalization, thumbnail generation, frame extraction, and export.
66. `[ ]` Add command center views for annotation node status, external task links, sync state, lightweight review, video evidence playback, frame/time navigation, read-only annotation overlays, telemetry context, and evidence history.
67. `[ ]` Add Spring profiles and configuration properties for V1 connectors: upload, email, REST/webhook, database import, object storage, and manual entry.
68. `[ ]` Add health checks and metrics for source connectors, database adapters, object storage adapters, model adapters, security configuration, queues/executors, configuration loading, evidence writes, and authorization denials.
69. `[ ]` Add Kanon intake workspace for single-entry case/task creation and bulk upload before dispatch to Label Studio, CVAT, Kanon-native review, or auto-approval.
70. `[ ]` Add `kanon-dataset` module for dataset curation, versioning, split management, and export contracts.
71. `[ ]` Add dataset curation rule engine, versioning, and export adapters (JSONL, Parquet, Hugging Face, TFRecord).
72. `[ ]` Add `kanon-training` module for training job orchestration and compute backend abstraction.
73. `[ ]` Add local GPU, Kubernetes, Vertex AI, SageMaker, and Azure ML compute backend adapters.
74. `[ ]` Add `kanon-model-registry` module for model versioning, lineage, evaluation, deployment, and promotion.
75. `[ ]` Add evaluation job runner with configurable metric computation.
76. `[ ]` Add deployment target SPI and health checks for model serving endpoints.
77. `[ ]` Add active learning strategy engine with uncertainty, diversity, query-by-committee, and policy-driven strategies.
78. `[ ]` Connect active learning output to the Review/Approval workflow for re-annotation and automatic retraining pipeline triggers.
79. `[ ]` Add command center views for dataset management, training jobs, model registry, evaluation, deployments, and active learning dashboard.
80. `[ ]` Add training pipeline evidence events for dataset, training, registry, evaluation, deployment, and active learning lifecycle.
81. `[x]` Create `docs/application-testing-manual.md` covering manual smoke tests, integration tests, UI tests, performance tests, training pipeline tests, and security tests.
Ü
