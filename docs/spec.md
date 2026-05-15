# KANON Behavior Spec

This document is the behavior-level companion to `docs/tasks.md`.

Use it to document what a task must accomplish before implementation details are chosen. Each spec entry should stay brief and answer:

- What exactly happens?
- What if it fails?
- What are edge cases?

Do not use this document to explain how to code the behavior, which libraries to use, or the internal class design. Architecture documents can explain system structure; this file records expected behavior.

## Related Architecture Docs

- `README.md`: product-level architecture and positioning.
- `docs/tasks.md`: implementation tracker and task ordering.
- `docs/module-structure.md`: module boundaries.
- `docs/configuration-architecture.md`: configuration activation and runtime source-of-truth rules.
- `docs/data-source-architecture.md`: source categories, upload/manual entry connectors, and traceability.
- `docs/security-access-control.md`: authorization, tenant isolation, redaction, and audit rules.
- `docs/scalability-architecture.md`: pagination, async work, queues, object storage, and external annotation tool scaling.
- `docs/llm-service-configuration.md`: configurable model services.
- `docs/mvp-agents-workflows.md`: MVP agent and workflow taxonomy.

## Maintenance Rule

Every new task in `docs/tasks.md` must either:

- reference an existing spec id from this document, or
- add a new spec entry before the task is added.

When a task changes behavior, update the linked spec in the same change. When a task is purely internal cleanup and has no behavior impact, mark it as `Spec: none`.

## SDD Traceability Rule

For behavior-changing work, use module-scoped IDs and traceability updates:

- Requirement IDs: `REQ-<MODULE>-###`
- Acceptance IDs: `AC-<MODULE>-###`

Update mappings in `docs/sdd/TRACEABILITY.md` and relevant module spec files under `docs/specification/*`.

## Spec Template

```markdown
## SPEC-000: Short Behavior Name

Task links: `docs/tasks.md#section-name`

Exactly happens:
- User/system-visible outcome.

Failure behavior:
- What the platform does when the outcome cannot be completed.

Edge cases:
- Important boundary conditions, invalid states, or unusual inputs.

Out of scope:
- Implementation details and unrelated behavior.
```

## SPEC-001: Configuration Activation

Task links: `docs/tasks.md#milestone-1-configuration-contracts`, `docs/tasks.md#near-term-task-order`

Exactly happens:
- Domain, tenant, workflow, agent, model routing, connector, and policy configuration is loaded as typed platform configuration.
- Reusable YAML packs provide default templates.
- Activated tenant configuration is versioned and available to runtime services as the current effective configuration.
- Admin/read endpoints expose loaded packs, active versions, and configuration summaries without exposing secrets.

Failure behavior:
- Invalid configuration is rejected before activation.
- Missing required fields, duplicate identifiers, invalid enum values, and broken references produce actionable validation issues.
- Runtime services continue using the last valid active configuration when a new configuration cannot be activated.

Edge cases:
- Multiple tenants may activate different versions of the same domain pack.
- A disabled or inactive configuration is visible for audit/admin inspection but is not used for runtime decisions.
- Secret values are never returned as normal visible configuration fields.

Out of scope:
- Persistence schema design, YAML parser selection, and Java class layout.

## SPEC-002: Runtime Persistence

Task links: `docs/tasks.md#milestone-2-persistence-adapters`, `docs/tasks.md#backend-persistence-rules`

Exactly happens:
- Runtime state is stored in PostgreSQL and immutable evidence/audit events are stored in the evidence ledger.
- Tenant-owned records are queryable by tenant-aware filters and operational views.
- Inserts and updates preserve audit metadata such as actor, timestamps, and revision/version where updates are allowed.

Failure behavior:
- Failed writes do not produce partial business state that appears complete to users.
- Read/list operations fail closed when tenant scope or authorization context is missing.
- Evidence append failures are surfaced as operational failures unless the calling behavior has an explicitly documented degraded mode.

Edge cases:
- Append-only evidence records are not updated through normal mutation paths.
- Large payloads are referenced by storage location and checksum rather than stored directly in relational or evidence records.
- List queries must remain bounded through pagination, filtering, and sorting.

Out of scope:
- Database migration syntax, repository implementation choices, and object-relational mapping details.

## SPEC-003: Security and Access Control

Task links: `docs/tasks.md#security-and-access-control-rules`, `docs/tasks.md#milestone-3-security-foundation`

Exactly happens:
- Access is denied by default unless tenant, role, permission, assignment, domain, classification, residency, and purpose checks allow it.
- APIs, services, repositories, and UI actions enforce authorization consistently.
- Sensitive values such as secrets, prompts, responses, raw payloads, and restricted fields are redacted unless explicit permission allows visibility.
- Sensitive reads, mutations, denied access, exports, role changes, permission changes, and break-glass access produce security/evidence events.

Failure behavior:
- Missing or invalid access context results in denial.
- Denied access is recorded without exposing the protected content.
- Break-glass access requires a reason, expiry, and approver path before protected content is shown.

Edge cases:
- UI menu hiding is only a visibility aid; direct API/service access must still be denied.
- Metadata access and payload/download access are separate permissions.
- Cross-tenant queries must not leak existence, counts, identifiers, or redacted payload details.

Out of scope:
- Identity-provider integration details and Spring Security configuration mechanics.

## SPEC-004: Data Source Ingestion

Task links: `docs/tasks.md#data-source-rules`, `docs/tasks.md#milestone-4-data-source-connectors`

Exactly happens:
- Every ingestion path creates a source trace with source category, source type, source system, source identifier, source URI, ingestion timestamp, original payload hash, tenant, actor, retention policy, compliance classification, data residency, consent reference, case id, correlation id, and evidence event id.
- Source traces preserve source-specific fields for file, email, API/webhook, enterprise record, machine/sensor, and streaming sources.
- V1 connectors cover upload, manual entry, REST/webhook, object storage, email, and PostgreSQL database import.
- Accepted ingestion can trigger workflow planning and produces evidence for received, stored, rejected, retried, completed, and workflow-triggered states.

Failure behavior:
- Invalid or unauthorized ingestion is rejected with an auditable reason.
- Retryable failures preserve enough trace state to retry idempotently.
- Connector health records expose last success, last failure, failure reason, retry count, and lag where applicable.

Edge cases:
- Manual UI entry is treated as a data source and audited like external ingestion.
- Replayed email, webhook, database, or object storage events are deduplicated by idempotency identity where available.
- Payload metadata can be visible while payload content remains restricted.

Out of scope:
- Vendor-specific connector SDK details and file parsing implementation.

## SPEC-005: AI Model Routing and Invocation

Task links: `docs/tasks.md#milestone-5-ai-routing-adapters`

Exactly happens:
- A task is routed to a configured model profile based on task type, domain, tenant preference, capabilities, cost, latency, locality, and compliance constraints.
- Model invocation uses the selected model profile and records route, invocation summary, model metadata, fallback use, and relevant evidence.
- Admin users with model permissions can create, update, enable, disable, test, and dry-run routing for model profiles.

Failure behavior:
- Unavailable or unhealthy primary models use the configured fallback profile when policy allows it.
- If no compliant model is available, invocation is rejected rather than silently using a non-compliant model.
- Prompt, response, and secret data are redacted in summaries unless explicit access permits visibility.

Edge cases:
- Local server models and API-backed models are both first-class profiles.
- A tenant policy can restrict a model even if the model technically supports the task.
- Expensive or long-running invocations must not block interactive request paths.

Out of scope:
- Provider client implementation details and prompt engineering internals.

## SPEC-006: Workflow Planning

Task links: `docs/tasks.md#milestone-6-workflow-planner--goap`

Exactly happens:
- Workflow planning produces an executable plan for MVP workflow types: data ingestion, annotation/extraction, and human review/approval.
- Planning considers source category, source trace, tenant, domain, policy decision, task descriptor, and access-control context.
- Plans expose ordered actions, required inputs, resumable state, retryable boundaries, and evidence for plan creation.

Failure behavior:
- If dynamic planning cannot produce a valid plan, the system uses the documented fallback planner when it can still satisfy policy.
- If neither planner can produce a valid policy-compliant plan, the workflow is not started and the failure is recorded.
- Planner fallback decisions are visible in evidence.

Edge cases:
- Plans must not embed large payloads in orchestration state.
- A plan may be blocked by policy even when all technical inputs are present.
- Retrying a workflow step must not duplicate non-idempotent external effects.

Out of scope:
- GOAP algorithm implementation and planner library integration details.

## SPEC-007: Agent Runtime and Evidence

Task links: `docs/tasks.md#milestone-7-agent-runtime-and-evidence`

Exactly happens:
- MVP agents execute only the documented MVP roles: ingestion, policy, extraction/annotation, and review orchestration.
- Agent lifecycle states include requested, started, source ingested, model invoked, completed, failed, reviewed, approved, and exported where applicable.
- Every agent action records correlation ids for tenant, case, source trace, workflow, agent, and model invocation when available.
- Annotation changes preserve immutable revision history.

Failure behavior:
- Agent failures record failed state, reason, and evidence without overwriting prior successful evidence.
- Retryable failures follow retry/backoff/timeout rules and preserve idempotency.
- Unauthorized users cannot view agent outputs or evidence beyond their allowed scope.

Edge cases:
- Evidence Ledger remains a shared service in MVP, not a separate autonomous agent.
- Agent execution should avoid keeping large state in memory or orchestration records.
- Human correction after model output must record before/after state.

Out of scope:
- Agent implementation framework details and queue/executor internals.

## SPEC-008: Command Center UI and APIs

Task links: `docs/tasks.md#milestone-8-api-and-vaadin-command-center`

Exactly happens:
- APIs and UI expose operational views for configuration, connectors, source traces, workflows, human tasks, evidence, model visibility, annotation diffs, and security/audit activity.
- Kanon UI is the primary operational workspace for case lifecycle, task routing, autonomy state, human review decisions, approval flow, audit trail, model routing, escalation, and evidence history.
- Users leave Kanon only when a task needs a specialist annotation workbench. Label Studio and CVAT may be opened through links or embedded handoff views, but they do not become the main UI.
- The command center provides case inbox, workflow board, agent timeline, confidence/autonomy state, approval queue, evidence history, model selection information, human task list, diff viewer, escalation controls, and lightweight document field correction.
- Final review, evidence inspection, approval, export, and source-of-truth annotation state are completed in Kanon after external annotation results are synced back.
- Lists and grids are tenant-scoped, paginated, sortable, filterable, and permission-aware.
- Users can see what was ingested, what is running, what needs review, what changed, which source/model/agent acted, and what evidence was recorded.

Failure behavior:
- Unauthorized actions and routes are denied even if a UI control is manually accessed.
- Large payload previews load only after explicit authorized demand.
- Dashboard summaries use bounded aggregate data rather than loading raw unbounded records.

Edge cases:
- Hidden menus do not imply security; backend enforcement remains authoritative.
- Redacted components must preserve context without leaking protected values.
- Status indicators must distinguish pending, running, failed, blocked, reviewed, approved, and completed states where relevant.

Out of scope:
- Vaadin component implementation details, styling, and icon library choices.

## SPEC-009: Media, Drone, and Robotics Data

Task links: `docs/tasks.md#media-drone-and-robotics-storage-rules`, `docs/tasks.md#milestone-9-video-drone-and-robotics-annotation`

Exactly happens:
- Video, image sequences, drone footage, robot camera streams, sensor captures, and derived media are treated as platform data assets.
- Large media bytes are stored in object storage while metadata, workflow state, annotations, review state, telemetry links, and asset relationships are stored as structured records.
- Media annotations support labels over frame/time ranges plus bounding boxes, polygons, masks, keypoints, tracks, and scene-level labels.
- Annotation UI execution is delegated to pluggable open source annotation nodes: Label Studio for text, document, audio, and tabular annotation; CVAT for image, video, LiDAR, drone, robotics, and medical imaging annotation.
- CVAT is used as a deep vision editing workbench for video, frame-by-frame review, object tracking, bounding boxes, polygons, segmentation, and masks. It is not the main operational UI.
- Label Studio is used as an optional specialist editor for advanced text, document, audio, tabular, span, sequence, and structured labeling tasks. Basic extracted-field review can happen directly in Kanon UI.
- Kanon decides whether an annotation task runs in full autonomous, human review, or mandatory human mode based on policy, confidence, risk, tenant configuration, and workflow context.
- In full autonomous mode, Kanon can generate, validate, auto-approve, and record evidence without creating an external Label Studio or CVAT task.
- In human review mode, Kanon creates AI pre-annotations, pushes a review task to Label Studio or CVAT, syncs human corrections back, normalizes the result, and records evidence.
- In mandatory human mode, Kanon may create AI suggestions but does not mark final approval until human signoff is synced back from the selected annotation node.
- Kanon creates annotation tasks, pushes them to the selected node, syncs completed results back into Kanon annotation records, and records evidence for task creation, sync, correction, review, approval, and failure.
- Kanon remains the source of truth for task ids, case state, workflow state, final annotation state, approval state, policy decisions, and evidence. External task ids are references only.
- Evidence records media upload, normalization, frame extraction, annotation creation/correction, review, approval, model invocation, and export.

Failure behavior:
- Failed media upload, checksum verification, normalization, frame extraction, or export leaves an auditable failed state.
- Failed annotation node push or result sync leaves the Kanon task in a failed/retryable state with the external node id, external task id when available, and failure reason.
- Missing or restricted payload access does not block authorized metadata views.
- Failed async processing exposes retry/failure status without losing the original media reference.

Edge cases:
- Drone and robotics telemetry may align by timestamp, frame number, or time offset.
- Media assets require tenant-aware storage paths or buckets.
- Derived media must remain traceable to the original media asset.
- External annotation tools are execution nodes only; they do not become the system of record for tenant policy, evidence, final annotation state, or approval state.
- External annotation tools assist with annotation interaction only. Kanon owns decisions.
- A task can be routed away from Label Studio or CVAT when policy allows auto-approval, or when the task type is unsupported by the configured annotation nodes.
- A human correction from an external node must be stored as a revision, not as a silent overwrite.

Out of scope:
- Codec/transcoding tool choices, storage SDK details, annotation tool SDK details, and UI overlay rendering implementation.

## SPEC-010: Bootstrap and Operations

Task links: `docs/tasks.md#milestone-10-bootstrap-and-operations`

Exactly happens:
- Local runtime starts with PostgreSQL and MongoDB defaults and supports production-like configuration through explicit environment variables.
- Operational configuration covers databases, object storage, connectors, authentication, authorization, model services, health checks, metrics, and tracing as those capabilities are introduced.
- Health and metrics expose runtime readiness, connector state, queue depth, latency, failure rate, fallback rate, evidence write rate, storage latency, UI endpoint latency, and authorization denial rate where applicable.

Failure behavior:
- Missing required runtime configuration prevents startup or marks the affected capability unhealthy.
- Optional integrations can be disabled only when the resulting degraded behavior is explicit.
- Health checks report failed dependencies without exposing secrets.

Edge cases:
- Docker/cloud profiles use environment-provided service locations rather than local defaults.
- Development users and local credentials must not become production defaults.
- Runtime docs must stay aligned with the active profiles and required environment variables.

Out of scope:
- Deployment platform scripts and CI implementation details.

## SPEC-011: Annotation Intake and Bulk Upload

Task links: `docs/tasks.md#future-complex-tasks`

Exactly happens:
- Kanon provides a native intake workspace for single-entry case/task creation and bulk upload.
- Single-entry intake supports manual metadata entry, source trace creation, tenant/domain selection, policy context, and optional attachment upload.
- Bulk upload supports multiple files or datasets, validation before submission, progress/status visibility, retryable failures, and batch-level evidence.
- Intake routes each item into Kanon-native review, Label Studio, or CVAT based on asset type, policy, configured annotation nodes, and execution mode.
- Kanon creates canonical case/task ids before dispatching anything to Label Studio or CVAT.
- External tool task links are available from Kanon after dispatch, and final review/approval/export remain in Kanon after sync.

Failure behavior:
- Failed validation blocks the affected item and reports actionable errors without losing the rest of the batch.
- Failed upload, object storage handoff, annotation node push, or result sync leaves an auditable retryable state.
- Duplicate or retried uploads are handled idempotently where checksums/source identifiers allow it.

Edge cases:
- A mixed bulk upload can contain document, image, video, tabular, and unsupported items.
- A user may have permission to upload metadata but not payload bytes, or to view metadata but not media previews.
- Large files require object storage handoff instead of moving full payloads through request/UI memory.
- A policy decision can skip Label Studio/CVAT and auto-approve or keep the task in Kanon-native review.

Out of scope:
- Rebuilding Label Studio or CVAT editing surfaces in Kanon.
- Vendor-specific import/export formats beyond normalized Kanon task/result records.

## SPEC-012: Localization

Task links: `docs/tasks.md#milestone-8-vaadin-command-center-and-api-surface`

Exactly happens:
- Kanon UI supports English and German translations.
- English is the default and fallback locale.
- The selected UI language is stored per Vaadin session.
- Navigation labels, shared dialogs, shared actions, shared validation messages, and application shell text are resolved through translation keys.
- Backend APIs return stable codes where practical; localized wording is applied in the UI.

Failure behavior:
- Missing translation keys render as visible key markers so gaps are obvious during development.
- Unsupported locales fall back to English.
- A failed or missing session locale does not block application startup.

Edge cases:
- Domain data loaded from the database is not translated by the UI bundle unless it is explicitly modeled as configurable display text.
- Tenant default locale and user locale preferences may be added later, with user preference taking precedence over tenant default and English as platform fallback.
- Audit event types and evidence event types remain stable machine-readable values; localized descriptions are presentation concerns.

Out of scope:
- Full localization of every admin field label in the first pass.
- Machine translation, translation management platforms, and database-backed translation editing.

## SPEC-013: Agent Management UI

Task links: `docs/tasks.md#milestone-8-api-and-vaadin-command-center`

Exactly happens:
- Admin users can list, search, filter, view, create, edit, enable/disable, and delete agents.
- The UI exposes agents conforming to the MVP list (Ingestion Agent, Policy Agent, Extraction/Annotation Agent, Review Orchestration Agent).
- The UI uses compact lists or grids prioritizing icons over text for primary actions and status (e.g. status dots, action icons).
- Agents are configurable by capability (domains, task types, asset types, schemas) and execution rules (mode, node type, routing, retry).
- Bulk actions allow toggling the enabled status or deleting multiple agents.

Failure behavior:
- Backend denies operations if the user lacks the correct permissions.
- Forms highlight validation failures when saving (e.g. missing required capabilities or bad model routing policy reference).
- UI displays localized error notification if backend calls fail.

Edge cases:
- Read-only agents or system-provisioned MVP defaults can only be inspected, not fully deleted or renamed.
- Large lists of agents use server-side pagination and lazy data loading rather than boundless fetching.
- Form fields dynamically adjust depending on the agent type (e.g., annotation threshold is hidden for ingestion agents).

Out of scope:
- Arbitrary custom agent definition beyond the MVP taxonomy.
- Drag-and-drop node graph canvas for building agent execution flows (handled as forms/properties).

## SPEC-014: External Annotation Node Configuration

Task links: `docs/tasks.md#milestone-8-api-and-vaadin-command-center`, `docs/tasks.md#milestone-9-video-drone-and-robotics-annotation`

Exactly happens:
- Admins can register, update, list, and delete external annotation nodes (Label Studio or CVAT).
- Each node configuration captures the provider type, instance URL, authentication secret reference, and associated storage bucket.
- A "Dry Run" test can be triggered from the UI to verify connectivity, authentication, and API compatibility without creating a real annotation task.
- Successful dry runs retrieve and display the external tool's version and health status.
- Every configuration change or test execution emits an evidence event to the ledger.

Failure behavior:
- If a Dry Run fails, the UI provides specific diagnostic feedback (e.g., "Connection Timed Out," "401 Unauthorized," or "Incompatible Tool Version").
- Nodes that fail verification are marked as "Offline" or "Unauthorized" and are filtered out of the Workflow Planner's available execution targets.
- Saving a configuration with invalid URL formats or missing required secrets is blocked by UI/API validation.

Edge cases:
- Secrets (API Keys) are masked in the UI and never returned in plain text after the initial save.
- A single tenant may have multiple nodes of the same type (e.g., two separate CVAT instances for different departments).
- Deleting a node that is currently linked to active, non-synced tasks is blocked or requires a force-acknowledgment.
- Tenant isolation ensures a "Platform Admin" can see all nodes, while a "Tenant Admin" sees only those assigned to their tenant.

Out of scope:
- Automatic installation or provisioning of Label Studio/CVAT instances.
- Management of user accounts inside the external tools (handled via the external tool's own admin UI).

## SPEC-015: Federated, Policy-Governed Orchestration

Task links: `docs/tasks.md#milestone-11-federated-orchestration`, `docs/tasks.md#future-complex-tasks`

Exactly happens:
- Kanon acts as a Global Control Plane to orchestrate data annotation and training across distributed `FederatedNodes`.
- Large-scale datasets are partitioned into "Work Shards" and dispatched to nodes based on compute capability and specialized domain expertise.
- **Data Residency Guardrails** enforce that data payloads never leave defined sovereign boundaries (e.g., restricted data is only processed by nodes within the correct geographic region).
- Training jobs are initiated by preparing an immutable "Training Context Package" (data snapshots + policy rules + model config).
- Remote progress, heartbeats, and metrics are synchronized back to the central Evidence Ledger to maintain a unified source of truth.
- Final model weights are linked back to the specific evidence audit trail and training parameters used to create them.

Failure behavior:
- **Node Dropout:** If a federated node fails heartbeats, its active shards are automatically returned to the global pool for re-assignment.
- **Sync Conflict:** The system uses a federated locking mechanism to prevent two nodes from modifying or training on the same data shard simultaneously.
- **Compliance Violation:** If a routing request violates a residency or domain policy, the dispatch is blocked and a security evidence event is emitted.

Edge cases:
- Handling partial syncs where a node completes work but loses connectivity before the final evidence push.
- Managing heterogeneous nodes with vastly different compute profiles (e.g., a local GPU cluster vs. a cloud spot instance).
- Version mismatch between the central control plane and remote federated worker versions.

Out of scope:
- Real-time federated learning (local training without centralizing metadata).
- Implementing the low-level compute protocols (Ray, Spark, etc.) directly—Kanon orchestrates the handoff to these systems.

## SPEC-016: Dataset Curation and Management

Task links: `docs/tasks.md#milestone-11-dataset-curation-and-management`

Exactly happens:
- Normalized annotation records are curated into versioned training datasets with train/val/test splits.
- Dataset curation supports inclusion/exclusion rules based on annotation confidence, review status, domain, tenant policy, and data quality criteria.
- Each dataset version records source annotation set, curation rules, split ratios, and evidence links to the source annotation evidence.
- Dataset export produces standardized formats: JSONL, Parquet, Hugging Face Datasets format, and TFRecord.
- Large payloads (images, video frames) are referenced by object storage URIs rather than embedded in dataset records.
- Dataset metadata includes total records, per-split counts, label distribution, class balance, and data residency classification.
- Dataset creation, export, and deletion produce evidence events.
- Datasets are tenant-isolated and permission-controlled.

Failure behavior:
- Curation with invalid or incomplete annotation records is rejected with actionable error details.
- Export failure leaves the dataset version in a failed state without losing the curated record set.
- Cross-tenant dataset access is denied even if annotation record ids are known.

Edge cases:
- A dataset may reference annotations from multiple cases, domains, or workflows.
- Incremental dataset updates produce new versions rather than mutating existing versions.
- Annotation corrections after dataset export do not retroactively update exported snapshots.
- Large datasets require streaming export rather than in-memory assembly.

Out of scope:
- Dataset storage format implementation details and cloud storage SDK specifics.

## SPEC-017: Model Training Orchestration

Task links: `docs/tasks.md#milestone-12-model-training-orchestration`

Exactly happens:
- Training jobs consume a curated dataset version, model configuration, hyperparameters, and compute backend target.
- Supported compute backends: local GPU, on-prem Slurm cluster, Kubernetes GPU pool, cloud ML services (Vertex AI, SageMaker, Azure ML).
- Training job lifecycle states: requested, queued, starting, running, checkpointing, completed, failed, cancelled.
- Training job progress, metrics (loss, accuracy), and status are reported back to Kanon as evidence events.
- Checkpoints are stored in object storage with references in the training job record.
- Completed training produces a model artifact referenced by object storage URI.
- Each training run records immutable evidence linking dataset version, model configuration, hyperparameters, compute backend, and output model artifact.
- Admins can view training job status, logs, and metrics from the Kanon UI.

Failure behavior:
- Compute backend unavailability prevents job start and is reported with diagnostic information.
- Training failure mid-run preserves the last checkpoint and records the failure reason.
- Resource exhaustion (OOM, disk full) is detected and reported as a distinguishable failure cause.
- Cancelled jobs clean up temporary compute resources.

Edge cases:
- Training jobs may run for hours or days and must survive platform restarts.
- Multiple training jobs may run concurrently against different compute backends.
- Hyperparameter search jobs produce multiple child training runs from a single parent.

Out of scope:
- Container image building and training framework installation logistics.
- Low-level GPU scheduling and distributed training topology.

## SPEC-018: Model Registry and Versioning

Task links: `docs/tasks.md#milestone-13-model-registry-and-evaluation`

Exactly happens:
- Trained models are registered as versioned model entries with unique model id and version.
- Model registry stores: model name, version, framework (PyTorch, TensorFlow), task type, domain, artifact location (object storage URI), training run id, dataset version id, hyperparameters used, evaluation metrics, and compliance tags.
- Models can be tagged with lifecycle stages: development, staging, production, deprecated, archived.
- Model lineage is fully traceable from raw source traces through annotation evidence, dataset curation, training run, and evaluation results.
- Model promotion (e.g., staging → production) requires configurable approval gates and records evidence.
- Registry supports model comparison by evaluation metrics across versions.
- Old model versions are never deleted without explicit archival and configurable retention.

Failure behavior:
- Registering a model with missing required metadata is rejected.
- Promoting a model without passing minimum evaluation thresholds is blocked unless overridden by authorized admin.
- Access to production models is restricted to authorized roles.

Edge cases:
- Multiple model versions may be in production simultaneously for A/B testing.
- A model may be rolled back to a previous version with evidence recorded.
- Models trained outside Kanon can be manually registered with lineage metadata.

Out of scope:
- Container registry management and model serving infrastructure.

## SPEC-019: Model Evaluation and Testing

Task links: `docs/tasks.md#milestone-13-model-registry-and-evaluation`

Exactly happens:
- Evaluation jobs run a registered model against a held-out test dataset to produce evaluation metrics.
- Supported metrics: accuracy, precision, recall, F1, ROC-AUC, mean average precision, BLEU, ROUGE, perplexity, and custom domain metrics.
- Evaluation results are stored as part of the model registry record for the model version.
- Evaluation output includes per-class metrics, confusion matrix, and failure case samples (where policy allows).
- Models can be compared side-by-side on the same test set through the Kanon UI.
- Evaluation must pass minimum thresholds before a model can be promoted to production.
- Evaluation runs produce evidence events linking model version, test dataset, and metric results.

Failure behavior:
- Evaluation against an incompatible test set (wrong task type, missing labels) is rejected.
- Evaluation framework failure preserves partial results if available.
- Test dataset containing restricted data is subject to the same access controls as the source annotations.

Edge cases:
- Evaluation may require GPU compute for large models and is routed through the same compute backend abstraction as training.
- A single model may be evaluated against multiple test sets for different domains or slices.
- Human evaluation (reviewing model outputs manually) is handled through the existing Review/Approval workflow.

Out of scope:
- Adversarial robustness testing and bias audit framework implementation.

## SPEC-020: Active Learning and Continuous Training

Task links: `docs/tasks.md#milestone-14-active-learning-and-continuous-training`

Exactly happens:
- Models in production can trigger active learning cycles based on confidence thresholds, uncertainty scores, or policy rules.
- Low-confidence predictions or uncertain classifications are flagged for human review and re-annotation.
- Re-annotated records are incorporated into updated dataset versions.
- Updated datasets automatically trigger retraining pipelines based on configurable policies (e.g., min new records threshold, scheduled interval, manual approval).
- Retrained models are evaluated against the test set and compared to the current production model.
- If the retrained model meets promotion criteria, it can be automatically or manually promoted to production.
- The full active learning cycle is recorded as linked evidence: prediction → review → dataset update → retraining → evaluation → promotion.
- Platform admins can configure active learning strategies: uncertainty sampling, diversity sampling, query-by-committee, or policy-defined rules.

Failure behavior:
- Active learning cycle is skipped if the configured strategy cannot select qualifying records.
- Retraining failure preserves the current production model unchanged.
- Evaluation regression (new model worse than current) blocks automatic promotion and notifies admins.

Edge cases:
- Active learning may span multiple tenants or domains with different strategies per context.
- A tenant may disable active learning entirely and rely on manual retraining triggers.
- High-volume production systems may rate-limit active learning cycles to control compute cost.
- Human reviewers must not be able to distinguish between active-learning-sampled records and routine review tasks.

Out of scope:
- Real-time online learning and streaming model updates.

## SPEC-021: Model Serving and Deployment

Task links: `docs/tasks.md#milestone-13-model-registry-and-evaluation`

Exactly happens:
- Models promoted to production are available as deployable serving endpoints or inference targets.
- Deployed models are registered in the Model Router as available model profiles for inference routing.
- Deployment targets include: local inference server, cloud API endpoint, ONNX runtime, Triton Inference Server, or custom serving infrastructure.
- Each deployment records the model version, deployment target, configuration, and timestamp as evidence.
- Rollback to a previous model version is supported and recorded as evidence.
- Deployment health checks verify the serving endpoint is responsive and returns correct output format.
- Inference through a deployed model routes through the existing ModelRouter respecting tenant policy, cost, latency, and compliance constraints.

Failure behavior:
- Deployment to an unreachable or misconfigured serving target is rejected with diagnostic feedback.
- Health check failure marks the deployment as unhealthy and can trigger automatic rollback.
- Routing to an unhealthy deployment is blocked; the router falls back to the configured fallback model.

Edge cases:
- A model may be deployed to multiple targets (e.g., staging and production) simultaneously.
- Blue/green or canary deployment strategies are configurable through deployment configuration.
- Model serving infrastructure credentials are stored as secret references, not plain values.

Out of scope:
- Autoscaling and load-balancing configuration for serving infrastructure.
- Custom inference optimization (quantization, batching, caching).
