# API Module Specification (`kanon-api`)

## Scope

HTTP API behavior for health, annotation task orchestration endpoints, intake session workflows, media upload, and metrics snapshot endpoints.

## Baseline Requirements

### REQ-API-001: Health and Metrics Contract Stability

- AC-API-001: Health endpoint returns product identity and aggregate status from component health.
- AC-API-002: Detailed/component health views expose structured per-component status.
- AC-API-003: Metrics endpoint returns a structured snapshot payload with collection timestamp.

### REQ-API-002: Annotation Task Endpoint Delegation and Error Translation

- AC-API-004: Annotation task creation delegates to orchestration service and returns created task linkage data.
- AC-API-005: Sync and retry endpoints return sync records for known tasks/nodes.
- AC-API-006: Unknown task/node sync operations return HTTP 404 responses.

### REQ-API-003: Intake and Media Upload Session Behavior

- AC-API-007: Intake session create/list/get operations are tenant scoped.
- AC-API-008: Uploading files mutates session state with stored upload metadata.
- AC-API-009: Dispatch removes session and returns dispatch result summary.
- AC-API-010: Media upload returns object key, storage URI, checksum, and size.
- AC-API-011: Presigned upload URL endpoint uses write URL generation path.
