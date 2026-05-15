# MVP Agents and Workflows

This document is the source of truth for the MVP agent and workflow taxonomy. New agent or workflow types should not be introduced casually in future sessions. Prefer extending configuration, capabilities, or task types before adding a new agent or workflow category.

## MVP Rule

- Use a small fixed set of generic agents.
- Use a small fixed set of generic workflows.
- Treat domain-specific behavior, such as accounting, HR, drones, robotics, medical, logistics, or agriculture, as configuration.
- Treat media-specific behavior, such as video, image, drone footage, robot streams, and telemetry, as data asset capability, not as separate agent families.
- Treat annotation UI execution as a pluggable external tool capability. Kanon orchestrates and governs annotation tasks; Label Studio and CVAT execute specialized human annotation interfaces.
- Treat Kanon UI as the operational workspace. Label Studio and CVAT are optional specialist workbenches for annotation interaction, not where users manage workflow, approval, escalation, evidence, or final case state.
- Keep final annotation truth in Kanon. External annotation task ids are references only; task identity, case state, workflow state, final annotation state, approval state, and evidence remain owned by Kanon.
- Support three annotation execution modes: full autonomous, human review, and mandatory human signoff.
- Add a new agent type only when there is a distinct lifecycle, permission model, operational owner, or execution contract.
- Add a new workflow type only when the state machine is meaningfully different from the existing workflows.

## MVP Agent List

The MVP should start with four core agents and one evidence ledger service.

| Agent | Status | Purpose | Notes |
| --- | --- | --- | --- |
| Ingestion Agent | MVP | Registers incoming data assets and prepares them for processing. | Handles documents, images, videos, drone footage, robot streams, metadata, checksums, object storage handoff, and basic technical metadata extraction. |
| Policy Agent | MVP | Applies tenant, domain, compliance, residency, and model-use rules. | Determines whether processing is allowed, which constraints apply, and whether annotation runs full-autonomous, human-review, or mandatory-human mode. |
| Extraction / Annotation Agent | MVP | Produces initial AI-generated structured output and creates annotation tasks when human labeling is required. | Handles document extraction, image annotation, video annotation, frame/time-range labels, object tracks, normalized draft annotations, auto-approval when policy allows it, and dispatch to Label Studio or CVAT through annotation nodes. |
| Review Orchestration Agent | MVP | Manages human review, correction, approval, rejection, and escalation. | Creates human tasks, dispatches external annotation tasks, syncs results, records corrections as revisions, and transitions review states. |
| Evidence Ledger Service | MVP service | Records immutable evidence for every important action. | Starts as a shared service instead of a fully autonomous agent. It can become an agent later if operational needs justify it. |

## Deferred Agent (Post-MVP)

| Agent | Status | Purpose | Notes |
| --- | --- | --- | --- |
| Training Agent | Post-MVP | Orchestrates dataset curation, training jobs, model evaluation, and promotion. | Deferred until annotation pipeline is stable. Handles dataset versioning, compute backend dispatch, training monitoring, evaluation, model registry updates, and active learning cycle management. |

## Do Not Add For MVP

Do not create these as separate MVP agents:

- Drone Agent
- Robot Agent
- Video Agent
- Invoice Agent
- Resume Agent
- Medical Agent
- Export Agent
- Compliance Agent separate from Policy Agent
- Model Agent separate from Extraction / Annotation Agent
- Training Agent (deferred to post-MVP)

These should be represented by configuration, source type, task type, model route, policy rule, or workflow step.

## MVP Workflow List

The MVP should start with three core workflows.

| Workflow | Status | Purpose | Example states |
| --- | --- | --- | --- |
| Data Ingestion Workflow | MVP | Safely registers data assets into the platform. | `REGISTER_ASSET`, `VALIDATE_TENANT`, `STORE_OBJECT`, `EXTRACT_METADATA`, `CREATE_ASSET_RECORD`, `APPEND_EVIDENCE` |
| Annotation / Extraction Workflow | MVP | Produces AI-generated structured output or annotations, then either auto-approves, creates human-review tasks, or requires mandatory human signoff. | `LOAD_ASSET`, `CHECK_POLICY`, `ROUTE_MODEL`, `EXECUTE_MODEL`, `NORMALIZE_RESULT`, `CREATE_ANNOTATION_DRAFT`, `EVALUATE_CONFIDENCE_AND_RISK`, `AUTO_APPROVE_OR_CREATE_EXTERNAL_TASK`, `APPEND_EVIDENCE` |
| Human Review / Approval Workflow | MVP | Converts draft output or external annotation results into trusted, reviewed output. | `CREATE_REVIEW_TASK`, `PUSH_TO_ANNOTATION_NODE`, `HUMAN_REVIEW`, `SYNC_ANNOTATION_RESULT`, `NORMALIZE_EXTERNAL_RESULT`, `APPLY_CORRECTION_REVISION`, `POLICY_CHECK`, `APPROVE_OR_ESCALATE`, `APPEND_EVIDENCE`, `MARK_EXPORT_READY` |

## Deferred Workflow

| Workflow | Status | Purpose | Notes |
| --- | --- | --- | --- |
| Export Workflow | Deferred | Packages approved data and evidence for downstream targets. | Keep export as a step in Human Review / Approval for MVP. Split it out after real export targets are defined. |

## Deferred Workflows (Post-MVP)

| Workflow | Status | Purpose | Example states |
| --- | --- | --- | --- |
| Dataset Curation Workflow | Post-MVP | Curates annotated records into versioned training datasets with configurable splits and exports. | `SELECT_ANNOTATION_RECORDS`, `APPLY_CURATION_RULES`, `COMPUTE_SPLITS`, `GENERATE_METADATA`, `EXPORT_DATASET`, `STORE_EXPORT_ARTIFACT`, `APPEND_EVIDENCE` |
| Model Training Workflow | Post-MVP | Orchestrates training jobs from dataset to trained model artifact. | `PREPARE_TRAINING_CONTEXT`, `SELECT_COMPUTE_BACKEND`, `DISPATCH_TRAINING_JOB`, `MONITOR_PROGRESS`, `COLLECT_METRICS`, `STORE_CHECKPOINTS`, `REGISTER_MODEL_ARTIFACT`, `APPEND_EVIDENCE` |
| Model Evaluation and Promotion Workflow | Post-MVP | Evaluates trained models and manages lifecycle stage transitions. | `LOAD_TEST_DATASET`, `RUN_EVALUATION`, `COMPUTE_METRICS`, `COMPARE_WITH_PRODUCTION`, `PROMOTE_OR_BLOCK`, `DEPLOY_ENDPOINT`, `APPEND_EVIDENCE` |
| Active Learning Cycle Workflow | Post-MVP | Selects uncertain records, creates review tasks, triggers retraining, and promotes improved models. | `SELECT_CANDIDATES`, `CREATE_REVIEW_TASKS`, `COLLECT_REANNOTATIONS`, `UPDATE_DATASET_VERSION`, `TRIGGER_RETRAINING`, `EVALUATE_NEW_MODEL`, `PROMOTE_IF_BETTER`, `APPEND_EVIDENCE` |

## Do Not Add For MVP

Do not create these as separate MVP workflows:

- Drone Workflow
- Robot Workflow
- Video Workflow
- Invoice Workflow
- Resume Workflow
- Medical Workflow
- Compliance Workflow
- Model Routing Workflow
- Dataset Curation Workflow (deferred to post-MVP)
- Model Training Workflow (deferred to post-MVP)
- Model Evaluation and Promotion Workflow (deferred to post-MVP)
- Active Learning Cycle Workflow (deferred to post-MVP)

These should be represented as domain configuration, media metadata, policy rules, workflow goals, task descriptors, or workflow action parameters.

## Agent UI Fields

When building the agent management UI and persistence model, include these fields early to reduce later schema changes.

### Identity and Ownership

- `agent_id`
- `tenant_id`
- `name`
- `agent_type`
- `description`
- `status`
- `enabled`
- `created_at`
- `created_by`
- `updated_at`
- `updated_by`
- `version`

### Capability and Scope

- `supported_domains`
- `supported_task_types`
- `supported_asset_types`
- `supported_source_types`
- `supported_annotation_types`
- `required_policies`
- `required_permissions`
- `input_schema_ref`
- `output_schema_ref`

### Execution

- `execution_mode`
- `annotation_execution_mode`
- `annotation_node_type`
- `annotation_node_id`
- `external_annotation_task_id`
- `annotation_confidence_threshold`
- `human_signoff_required`
- `timeout_seconds`
- `retry_policy`
- `max_attempts`
- `concurrency_limit`
- `priority`
- `queue_name`
- `runtime_profile`
- `configuration_ref`

### AI Routing

- `model_route_policy`
- `preferred_model_profile`
- `fallback_model_profile`
- `allowed_model_profile_ids`
- `required_llm_service_capabilities`
- `allow_cloud_models`
- `allow_local_models`
- `max_cost_class`
- `max_latency_class`
- `compliance_tags`

### Observability and Evidence

- `evidence_required`
- `trace_enabled`
- `last_run_at`
- `last_success_at`
- `last_failure_at`
- `last_failure_reason`

## Workflow UI Fields

When building the workflow management UI and persistence model, include these fields early.

### Identity and Ownership

- `workflow_id`
- `tenant_id`
- `name`
- `workflow_type`
- `description`
- `status`
- `enabled`
- `created_at`
- `created_by`
- `updated_at`
- `updated_by`
- `version`

### Scope

- `domain_type`
- `task_type`
- `asset_type`
- `source_type`
- `policy_profile`
- `regulatory_act`
- `data_residency`

### Planning

- `goal`
- `planner_type`
- `planner_version`
- `action_set_ref`
- `preconditions`
- `constraints`
- `fallback_workflow_ref`
- `model_route_policy`
- `allowed_model_profile_ids`

### Execution State

- `case_id`
- `media_asset_id`
- `current_step`
- `current_state`
- `assigned_agent_id`
- `assigned_user_id`
- `priority`
- `due_at`
- `started_at`
- `completed_at`
- `failed_at`
- `failure_reason`

### Review and Approval

- `review_required`
- `review_status`
- `reviewer_id`
- `approval_status`
- `approved_by`
- `approved_at`
- `escalation_reason`
- `export_ready`

### Evidence and Traceability

- `evidence_event_ids`
- `trace_id`
- `correlation_id`
- `model_invocation_ids`
- `input_asset_ids`
- `output_asset_ids`

## Media-Specific Workflow Fields

For video, drone, and robotics workflows, use optional fields on assets, annotations, and workflow instances rather than creating separate workflow types.

- `media_asset_id`
- `frame_start`
- `frame_end`
- `start_time_ms`
- `end_time_ms`
- `annotation_geometry_type`
- `annotation_execution_mode`
- `annotation_node_type`
- `annotation_node_id`
- `external_annotation_task_id`
- `track_id`
- `telemetry_ref`
- `source_device_id`
- `mission_id`
- `capture_timestamp`
- `latitude`
- `longitude`
- `altitude`
- `heading`
- `speed`

## Change Control

Before adding a new MVP agent or workflow type:

1. Check whether the need can be represented by configuration, task type, source type, or policy.
2. Check whether the existing agent or workflow lifecycle already covers it.
3. Document the reason in this file.
4. Update `docs/tasks.md`.
5. Add migration and UI field tasks if persistence changes are required.
