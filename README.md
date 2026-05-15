# 🧠 Kanon Platform

## The Sovereign Agentic Data & Workflow Operating System

## About Kanon Platform

**Kanon Platform** is a universal, enterprise-grade, domain-configurable agentic platform for data annotation, workflow orchestration, AI training pipelines, and model lifecycle management.

Kanon Platform is by **Datalithix**.

It is not a labeling tool.

It is a **Policy-First, Data-Centric, Agentic Operating System** that transforms raw enterprise data into **structured, auditable, and continuously improving intelligence**.

Kanon orchestrates and governs data annotation workflows by integrating best-in-class open source annotation tools, provides dataset curation and export for model training, orchestrates training jobs on distributed compute backends, manages model registry and evaluation, and closes the loop with active learning — all with full auditability, domain configurability, and intelligent agent-driven automation.

---

# 🚀 Core Vision

> **Define your domain. Configure your agents. Control your policies. Trace every decision.**

KANON dynamically adapts its behavior based on:

* **Domain** (HR, Accounting, Agriculture, Medical, Logistics)
* **Regulatory Act** (EU AI Act 2026, GDPR, HIPAA, HGB)
* **Tenant Policy**
* **Task Type (AI workload)**
* **Data Sensitivity & Location**

---

# 🧩 Core Pillars

1. 🌐 Domain-Configurable Platform
2. 🤖 Task-Aware Multi-Model Orchestration
3. ⚖️ Policy-Driven Workflow Planning
4. 🔍 Full Audit Traceability & Evidence Ledger
5. 🖥️ Agentic Command UI
6. 🧬 Dataset Curation & Model Training Pipeline
7. ♻️ Active Learning & Continuous Improvement Loop

---

# 📚 Documentation Map

The README gives the product-level architecture. Use these docs as the detailed sources of truth:

| Document | Purpose |
| --- | --- |
| [`docs/spec.md`](docs/spec.md) | Behavior requirements, failure behavior, and edge cases for implementation tasks. |
| [`docs/tasks.md`](docs/tasks.md) | Implementation tracker, milestone order, and near-term task sequencing. |
| [`docs/module-structure.md`](docs/module-structure.md) | Module boundaries and integration points. |
| [`docs/configuration-architecture.md`](docs/configuration-architecture.md) | YAML templates, active tenant configuration, validation, and activation model. |
| [`docs/data-source-architecture.md`](docs/data-source-architecture.md) | Source categories, connectors, traceability, upload, manual entry, and ingestion rules. |
| [`docs/security-access-control.md`](docs/security-access-control.md) | RBAC, ABAC, tenant isolation, redaction, break-glass, and security evidence rules. |
| [`docs/multi-tenant-governance-architecture.md`](docs/multi-tenant-governance-architecture.md) | Tenant, organization, workspace, user, membership, role, and bootstrap administration model. |
| [`docs/scalability-architecture.md`](docs/scalability-architecture.md) | Pagination, async work, queues, object storage, external annotation scaling, and observability. |
| [`docs/llm-service-configuration.md`](docs/llm-service-configuration.md) | Configurable local/API model services and model profile administration. |
| [`docs/mvp-agents-workflows.md`](docs/mvp-agents-workflows.md) | Fixed MVP agent/workflow taxonomy and rules for not adding unnecessary agent families. |
| [`docs/localization-architecture.md`](docs/localization-architecture.md) | UI localization model, language fallback, translation key rules, and English/German support. |
| [`docs/dataset-curation-architecture.md`](docs/dataset-curation-architecture.md) | Dataset curation, versioning, splits, export formats, and training data governance. |
| [`docs/model-training-architecture.md`](docs/model-training-architecture.md) | Training job orchestration, compute backends, hyperparameters, and checkpointing. |
| [`docs/model-registry-architecture.md`](docs/model-registry-architecture.md) | Model versioning, lineage, metadata, evaluation, and deployment lifecycle. |
| [`docs/active-learning-architecture.md`](docs/active-learning-architecture.md) | Active learning strategies, uncertainty sampling, and continuous training feedback loop. |
| [`docs/application-testing-manual.md`](docs/application-testing-manual.md) | Manual and automated testing procedures for the Kanon platform. |

---

# ✅ SDD Governance

Kanon uses a lean Specification-Driven Development workflow for behavior-changing work.

Primary governance files:

| Document | Purpose |
| --- | --- |
| [`docs/contributing.md`](docs/contributing.md) | Contributor workflow, behavior-change gate, and required verification levels. |
| [`docs/sdd/CONSTITUTION.md`](docs/sdd/CONSTITUTION.md) | Non-negotiable SDD operating principles. |
| [`docs/sdd/CHANGE_CONTROL.md`](docs/sdd/CHANGE_CONTROL.md) | Required artifacts for behavior-changing vs non-behavior-changing PRs. |
| [`docs/sdd/TRACEABILITY.md`](docs/sdd/TRACEABILITY.md) | REQ/AC to tests/code traceability mapping. |
| [`docs/specification/README.md`](docs/specification/README.md) | Module-level specification baseline rules. |
| [`AGENTS.md`](AGENTS.md) | Mandatory coding-agent pre-read and pre-edit contract. |
| [`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md) | Enforced PR checklist aligned with SDD change-control. |

Behavior-changing pull requests must include spec updates, traceability updates, tests, and status updates in the same change set.

---

# 🌍 Domain-Agnostic Configuration Engine

KANON enables runtime domain definition via metadata.

## Example: Accounting

```yaml
domain: ACCOUNTING
country: DE
regulatory_act: HGB

entities:
  - Invoice
  - LedgerEntry
  - Tax

agents:
  - InvoiceExtractionAgent
  - VATCalculationAgent
  - AuditAgent

rules:
  - VAT_DE_19_PERCENT
  - HGB_COMPLIANCE_CHECK
```

## Example: HR

```yaml
domain: HR

entities:
  - Resume
  - Candidate

agents:
  - ResumeParserAgent
  - SkillExtractionAgent
  - CandidateScoringAgent

rules:
  - GDPR_PII_MASKING
  - BiasDetectionCheck
```

---

## 🔑 Principle

> **The domain is not coded — it is configured.**

---

# ⚙️ Configuration Model

KANON uses layered configuration.

## Configuration Principle

> **YAML defines reusable templates. PostgreSQL stores activated tenant configuration.**

Use YAML for:

* default domain packs
* workflow templates
* agent definitions
* model routing templates
* connector type defaults
* policy templates
* local development and test fixtures

Use database-backed active configuration for:

* tenant overrides
* admin edits
* active versions
* activation/deactivation state
* connector credentials and secret references
* model preferences
* role mappings
* workflow activation state

Runtime services should consume typed configuration objects, not raw YAML maps.

---

# Local Runtime

KANON uses PostgreSQL and MongoDB for runtime state. Do not use in-memory stores for application runtime.

Local defaults:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/datalithix_kanon
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.data.mongodb.uri=mongodb://localhost:27017/datalithix_kanon
```

Start the app against local database installations:

```bash
mvn -pl kanon-bootstrap -am spring-boot:run
```

The default Spring profile is `local-db`. For cloud or container deployment, use the `cloud` profile and provide:

```bash
KANON_POSTGRES_URL
KANON_POSTGRES_USERNAME
KANON_POSTGRES_PASSWORD
KANON_MONGODB_URI
```

Docker assets are kept at the repository root:

```bash
docker compose up --build
```

The Docker image runs with `SPRING_PROFILES_ACTIVE=cloud` and expects PostgreSQL and MongoDB connection details through environment variables.

---

# 🔄 Unified Data Supply Chain

## Source → Intelligence → Training → Feedback

### Data Sources

KANON thinks in source categories, not just files.

* Interactive sources: manual case creation, UI forms, review tasks, corrections, approvals
* Document sources: PDFs, DOCX, XLSX, CSV, scans, ZIP uploads, attachments
* Communication sources: email inboxes, shared mailboxes, forwarded messages, chat, tickets
* System sources: ERP, HRMS, CRM, ATS, ServiceNow-style systems
* API sources: REST, GraphQL, webhooks, partner systems, government APIs
* Machine sources: drone images, CCTV, LiDAR, IoT feeds, GPS streams, telemetry, DICOM
* Storage sources: databases, network drives, SharePoint, S3/object storage, cloud drives
* Streaming sources: Kafka, RabbitMQ, cloud pub/sub, internal event buses

### V1 Connectors

The first version should focus on:

* Upload Connector
* Email Connector
* Manual Entry Connector
* REST/Webhook Connector
* Database Import Connector
* Object Storage Connector

### Source Traceability

Every ingestion path must capture source type, source category, source system, source identifier, ingestion timestamp, original payload hash, tenant, actor, retention policy, compliance classification, data residency, case id, correlation id, and evidence event id.

### Processing & Annotation Execution

* OCR
* AI extraction
* CV labeling

### Annotation Execution Layer

Kanon does not implement complex annotation interfaces from scratch. It integrates specialized annotation systems as pluggable execution nodes.

Supported open source nodes:

* Label Studio: text, document, audio, and tabular annotation
* CVAT: image, video, LiDAR, drone, robotics, and medical imaging annotation

Architectural principle:

> **Kanon orchestrates. External annotation tools execute.**

Control-plane rule:

> **Final truth lives in Kanon, not in Label Studio or CVAT.**

Responsibility split:

| Layer | Responsibility |
| --- | --- |
| Kanon | tenant/domain configuration, workflow state, policy, model routing, AI pre-annotation, confidence/risk decisions, final approval state, normalized annotation records, audit, evidence, agentic UI |
| Label Studio | document, text, audio, and tabular annotation UI |
| CVAT | image, video, LiDAR, drone, robotics, and medical imaging annotation UI |

UI and workbench split:

* Kanon UI is the default operational workspace for case lifecycle, workflow control, policy decisions, model visibility, approval, escalation, evidence history, and lightweight field correction.
* Label Studio is an optional specialist editor for text, document, audio, tabular, sequence labeling, span annotation, and complex structured labeling tasks.
* CVAT is an optional specialist editor for vision-heavy tasks such as video review, frame navigation, object tracking, bounding boxes, polygons, segmentation, and masks.
* Users leave Kanon only when the selected task needs a richer annotation workbench than Kanon should own in v1.
* Final review, approval, export, normalized annotation state, and evidence history return to Kanon after external tool sync.

Annotation execution modes:

1. Full autonomous mode

Kanon runs the model, generates annotations, validates them, auto-approves when policy and confidence thresholds allow it, and records evidence. Label Studio or CVAT may be skipped entirely, or used only as optional storage/export targets.

```text
Source
   ↓
Kanon AI Agent
   ↓
Validation
   ↓
Auto-Approve
   ↓
Evidence Ledger
```

2. Human review mode

Kanon generates pre-annotations, sends the task to Label Studio or CVAT for human correction, syncs the result back, normalizes it into Kanon records, and records evidence.

```text
Source
   ↓
Kanon AI Agent
   ↓
Pre-Annotation
   ↓
Label Studio or CVAT
   ↓
Human Annotation
   ↓
Result Sync Back to Kanon
   ↓
Evidence Ledger Updated
```

3. Mandatory human mode

Kanon may generate AI suggestions, but final approval is blocked until human signoff is completed in the selected annotation node and synced back into Kanon.

```text
Source
   ↓
Kanon AI Suggestion
   ↓
Label Studio or CVAT Review
   ↓
Human Approval
   ↓
Sync Back to Kanon
   ↓
Evidence Ledger Updated
```

Canonical ownership:

* task ids originate in Kanon
* case and workflow state live in Kanon databases
* audit events live in the Kanon evidence ledger
* external task ids are references, not primary identity
* external tool results are synced back and normalized into Kanon annotation records
* Label Studio and CVAT do not own policy, evidence, workflow state, final annotation state, or approval state

This avoids rebuilding proven annotation UIs, accelerates development, and keeps Kanon focused on orchestration, governance, intelligence, and traceability.

Scaling rule:

* Store large payloads in object storage, keep metadata and workflow state in Kanon databases, partition external annotation tasks, use async push/pull sync jobs, use queue-based imports/exports, and keep separate workers for AI pre-annotation and external result sync.

### Validation

* Human-in-the-loop
* Policy-aware checks

### Dataset Curation

* Normalized annotation records curated into training datasets
* Dataset versioning, splits (train/val/test), and metadata
* Export to Hugging Face Datasets, JSONL, Parquet, TFRecord
* Policy-governed data inclusion and exclusion rules

### Model Training

* Training job orchestration on configurable compute backends
* Local GPU, on-prem cluster, cloud ML services (Vertex AI, SageMaker, Azure ML)
* Hyperparameter configuration, checkpointing, and job monitoring
* Immutable training run evidence linking dataset + config → model artifact

### Model Registry & Evaluation

* Versioned model storage with full lineage (dataset, training run, parameters)
* Evaluation against held-out test sets with tracked metrics
* A/B comparison and promotion gates

### Active Learning & Continuous Training

* Model uncertainty drives targeted re-annotation
* Updated training datasets trigger retraining pipelines
* Full evidence trail from raw data → annotation → training → deployment

### Training Targets

* LLM fine-tuning
* RAG systems
* ML pipelines
* Autonomous agents
* Custom embedding models

---

# 🤖 Task-Aware Multi-Model Orchestration

KANON dynamically selects AI models based on:

* task type
* domain
* tenant policy
* latency / cost
* compliance requirements

## Model Roles

* Extraction Models → high accuracy
* Classification Models → low latency
* Utility Models → local-first tasks
* Compliance Models → regulated workflows
* Reasoning Models → complex decisions
* Fallback Models → resilience

---

## Example Routing Logic

```
EXTRACTION → high-quality cloud model
CLASSIFICATION → local small model
COMPLIANCE → trusted model
UTILITY → local model
```

---

## Result

> **Right model for the right task — per tenant, per domain, per policy**

---

# 🧠 Configurable LLM Services

LLM models are platform services, not hardcoded clients.

## Service Principle

> **LLMs are Spring-managed services with admin-configurable local and API-backed model profiles.**

KANON should expose model access through service contracts such as:

* `LlmService`
* `LlmServiceRegistry`
* `LlmServiceFactory`
* `ModelInvocationService`
* `ModelRouter`

The router selects a configured model profile. The invocation service calls the selected model through a Spring-managed LLM service bean.

## Supported Backends

* Local server models: Ollama, llama.cpp server, vLLM, or OpenAI-compatible local endpoints
* API models: OpenAI-compatible APIs, Azure OpenAI-style deployments, or provider adapters

## Admin Configuration

Admins should be able to configure model profiles from the UI:

* backend type: local server or API
* provider
* base URL
* model name or deployment name
* capabilities and modalities
* task/domain/tenant restrictions
* cost and latency class
* compliance tags
* fallback profile
* health-check status
* credential reference

Secrets must be stored as secret references, encrypted values, environment references, or deployment secrets. Raw API keys must not be exposed as normal UI-visible fields.

---

# ⚖️ Policy-Driven Workflow Planning

Using **GOAP (Goal-Oriented Planning)**:

* workflows are NOT hardcoded
* dynamically generated
* based on domain + policy + context

---

# 🔍 Full Audit Traceability & Evidence Ledger

Audit is a **first-class system**, not logging.

Every action is recorded as immutable evidence.

## Tracked Events

* data ingestion
* agent execution
* model invocation
* annotation generation
* human correction
* approvals
* escalations
* exports

---

## Each Event Captures

* who (human / agent)
* what action
* when
* why
* model used
* prompt version
* policy version
* before/after state

---

## Example Evidence Record

```json
{
  "eventType": "ANNOTATION_CORRECTED",
  "actorType": "HUMAN",
  "model": "gpt-oss-120b",
  "before": "1000",
  "after": "1190",
  "reason": "VAT correction"
}
```

---

## Principle

> **No silent action. No invisible decision. No overwrite without revision.**

---

# 🧾 Data Provenance & Trust Layer

* JSON-LD evidence certificates
* full lineage tracking
* replayable workflows

👉 Result:

**Verifiable AI training data**

---

# 🎥 Video, Drone & Robotics Data

KANON treats video, drone footage, robot camera streams, sensor captures, and derived media as first-class data assets.

## Storage Principle

> **Large media belongs in object storage. Metadata, workflow state, annotations, and evidence belong in databases.**

## Recommended Storage Split

* Object storage: original videos, normalized/transcoded media, thumbnails, extracted frames, masks, overlays, and export files
* PostgreSQL: media metadata, tenant ownership, workflow state, annotation state, review state, telemetry links, and asset relationships
* MongoDB / evidence ledger: immutable events for upload, transcoding, model invocation, annotation, correction, review, approval, and export
* Search index: optional later for large-scale discovery and analytics

## Object Storage

Use S3-compatible object storage as the default abstraction.

* MinIO for local and on-prem deployments
* S3-compatible cloud or sovereign storage for managed environments
* Store only object URIs, checksums, content types, sizes, and technical metadata in relational records
* Keep tenant-aware buckets or storage paths for isolation

## Drone & Robot Metadata

Media assets should support optional source metadata:

* source type: upload, drone, robot, camera stream, sensor stream, external import
* source device id
* mission id
* capture timestamp
* frame rate, duration, codec, resolution
* geospatial data: latitude, longitude, altitude, heading, speed
* telemetry synchronized by timestamp, frame number, or time offset

## Video Annotation

Video annotations should support:

* labels over frame ranges and time ranges
* bounding boxes, polygons, masks, keypoints, tracks, and scene-level labels
* human correction and approval flows
* immutable evidence for every model-generated or human-modified annotation

For annotation UI execution, use CVAT as the preferred open source execution node for image, video, LiDAR, drone, robotics, and medical imaging workflows. Kanon stores synchronized annotation state and evidence; CVAT provides the specialized annotation interface.

---

# 🖥️ Agentic Command UI

KANON is a **live operational command center**.

Kanon UI is the primary workspace. Label Studio and CVAT are specialist workbenches opened only when annotation interaction requires them.

## UI Levels

1. Native Kanon UI

Default experience for case handling, workflow control, audit, model visibility, approval, escalation, evidence review, and lightweight field correction.

2. Linked or embedded specialist tool

Label Studio handles advanced text, document, audio, tabular, span, sequence, and structured annotation. CVAT handles rich image, video, LiDAR, tracking, polygon, mask, and segmentation annotation.

3. Synced result in Kanon

Final review, evidence, approval, export, and source-of-truth annotation state are completed in Kanon after external results are normalized.

## Core UI Capabilities

### 📥 Intake Workspace

* single-entry case/task creation
* bulk upload for files and datasets
* upload status, validation, and retry visibility
* routing into Kanon-native review, Label Studio, or CVAT based on task type and policy

### 🧭 Workflow Board

* live case tracking

### 🕒 Agent Timeline

* full action visibility

### 👥 Human Task Inbox

* review / approve / escalate

### 🔍 Diff Viewer

* before vs after changes

### 🤖 Model Visibility

* which model was used and why

### 📜 Evidence Explorer

* full audit history

---

## UX Principle

> **Users see what AI did, why it did it, and what changed after human intervention.**

---

# 🧠 Architecture Overview

```
Tenant Config
   ↓
Policy Engine
   ↓
Agent Orchestrator (GOAP)
   ↓
Model Router
   ↓
AI + HITL Execution
   ↓
Evidence Ledger
   ↓
Agentic UI
```

---

# 📈 Scalability

Scalability is a primary design constraint for KANON.

## Scalability Principle

> **Every implementation must assume tenant growth, large datasets, high ingestion volume, long evidence history, and concurrent human/agent activity.**

Core rules:

* keep synchronous request paths short
* run heavy ingestion, model invocation, media processing, and export work asynchronously
* store large payloads in object storage
* use tenant-aware indexes and operational query indexes
* paginate, filter, and sort every list API and grid-backed query
* make connector and workflow steps idempotent and retryable
* enforce timeouts, retries, rate limits, and concurrency limits for model calls
* avoid unbounded in-memory loads and N+1 query patterns
* measure ingestion throughput, connector lag, workflow queue depth, model latency, evidence write rate, database latency, object storage latency, UI latency, and authorization denials

---

# 🏗️ Technology Stack

## Core Platform

* Java 25 (Structured Concurrency, Scoped Values)
* Spring Boot 4.0
* Vaadin 25
* LangChain4j
* Embabel (GOAP)
* PostgreSQL (state)
* MongoDB (evidence logs)
* MinIO / S3-compatible object storage

## Model Training Pipeline

* Ray or Spring Cloud Task (training orchestration)
* PyTorch / TensorFlow (training frameworks — invoked externally)
* Hugging Face Datasets / custom Parquet (dataset interchange)
* MLflow (experiment tracking, optional integration)
* Docker + Kubernetes (compute backend abstraction)

---

# 🔐 Sovereign Data Control

* multi-tenant isolation
* EU-compliant deployment
* on-prem support

---

# 🛡️ Security & Access Control

KANON uses default-deny access control.

## Access Principle

> **Users see only data allowed by tenant, role, assignment, domain, policy, and data classification.**

Security is enforced through:

* RBAC for role-based permissions
* ABAC for tenant, domain, case, assignment, source, classification, residency, and purpose checks
* tenant isolation on every tenant-owned record
* separate metadata and payload/download permissions
* redaction for secrets, prompts, responses, raw payloads, and sensitive fields
* evidence events for sensitive reads, mutations, denied access, exports, and break-glass access

## MVP Roles

* Platform Admin
* Tenant Admin
* Domain Manager
* Reviewer / Annotator
* Auditor
* Integration Service Account
* Model Operator
* Viewer

The UI may hide unavailable menus and actions, but APIs, services, and repositories must still enforce authorization.

---

# 🚀 Positioning

> **Kanon Platform is a domain-neutral agentic operating system for enterprise data workflows and AI training, enabling configurable AI-driven annotation, dataset curation, model training, and continuous improvement with full auditability, open source tool integration, and intelligent orchestration. Built by Datalithix.**

---

# 🧠 Vision

**Verifiable Truth. Configurable Intelligence. Sovereign Control. Continuous Improvement.**
