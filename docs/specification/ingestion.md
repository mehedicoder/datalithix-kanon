# Ingestion Module Specification (`kanon-ingestion`)

## Scope

Source intake orchestration, connector selection/execution, source trace persistence, connector health updates, and ingestion audit/evidence emission.

## Baseline Requirements

### REQ-INGESTION-001: Connector-Driven Ingestion Outcome

- AC-INGESTION-001: Ingestion uses a matching connector for the request source type/category.
- AC-INGESTION-002: When no matching connector is available, result is failed with a concrete reason.
- AC-INGESTION-003: Successful ingestion returns completed status with a source trace and evidence identifier.

### REQ-INGESTION-002: Traceability and Audit Emission

- AC-INGESTION-004: Source descriptor and source trace records are persisted for successful ingestion.
- AC-INGESTION-005: Ingestion emits evidence events containing source metadata.
- AC-INGESTION-006: Security audit events are published for successful and failed ingestion attempts.

### REQ-INGESTION-003: Connector Health Tracking

- AC-INGESTION-007: Connector health is updated after successful ingestion attempts.
- AC-INGESTION-008: Connector health is updated after failed ingestion attempts.
- AC-INGESTION-009: Health state is retrievable by tenant and connector identifier.
