# Data Source Architecture

KANON should think in data source categories, not just files. Enterprise work starts in many systems, and each source must be traceable, auditable, tenant-aware, and policy-aware.

## Source Category Model

Use these platform-level source categories instead of modeling every vendor or tool as a separate concept.

| Category | Purpose | Examples |
| --- | --- | --- |
| Interactive sources | Human-created work inside KANON. | Manual case creation, UI forms, review tasks, corrections, notes, approvals, rejections |
| Document sources | Files and document-like assets. | PDF, DOCX, XLSX, CSV, scanned images, ZIP uploads, user-uploaded forms, email attachments |
| Communication sources | Business communication channels. | IMAP inboxes, shared mailboxes, forwarded messages, email body text, chat, tickets |
| System sources | Enterprise systems and structured systems of record. | SAP, DATEV, Oracle ERP, Microsoft Dynamics, Workday, SuccessFactors, Salesforce, ServiceNow, HRMS, CRM |
| API sources | External or partner APIs. | REST APIs, GraphQL, webhooks, partner systems, government APIs |
| Machine sources | Vision, sensor, robotics, and machine-generated data. | Drone images, CCTV, LiDAR, IoT sensor feeds, GPS streams, machine telemetry, DICOM medical images |
| Storage sources | Existing databases, file shares, and object stores. | PostgreSQL, MySQL, Oracle, SQL Server, MongoDB, network drives, SharePoint, S3, Google Drive, OneDrive, Dropbox |
| Streaming sources | Event streams and queues. | Kafka, RabbitMQ, cloud pub/sub, internal event bus |

## V1 Source Connectors

Build these first because they unlock early HR and Accounting use cases while keeping the platform broadly useful.

| Connector | Status | Covers |
| --- | --- | --- |
| Upload Connector | V1 | File uploads, scans, ZIP uploads, PDFs, DOCX, XLSX, CSV, images |
| Email Connector | V1 | IMAP inboxes, shared mailboxes, forwarded messages, attachments, body text |
| Manual Entry Connector | V1 | Manual case creation, forms, notes, corrections, approvals, rejections |
| REST/Webhook Connector | V1 | REST ingestion, webhook callbacks, partner APIs, status callbacks |
| Database Import Connector | V1 | PostgreSQL or existing database import as first target, expandable to other databases |
| Object Storage Connector | V1 | S3-compatible object storage and MinIO for local/on-prem deployments |

Defer specialized enterprise connectors, streaming connectors, and advanced machine connectors until the generic ingestion contracts are stable.

## Intake UI Direction

The upload and manual-entry connectors should eventually surface through a Kanon intake workspace. The detailed future behavior is tracked in `docs/spec.md#spec-011-annotation-intake-and-bulk-upload` and `docs/tasks.md#future-complex-tasks`.

Planned intake modes:

- single-entry case/task creation for manual work, metadata, and optional attachments
- bulk upload for documents, images, videos, tabular files, and datasets
- validation preview before submission
- batch progress, retry, and cancellation visibility
- routing into Kanon-native review, Label Studio, CVAT, or auto-approval based on policy and task type

Kanon still creates canonical case/task ids and source traces before dispatching work to external tools.

## Domain Source Examples

### HR

- CV PDFs
- job descriptions
- email applications
- HRIS / ATS data
- interview notes
- candidate forms

### Accounting

- invoice PDFs
- scanned receipts
- ERP master data
- vendor CSVs
- email attachments
- accounting system APIs

### Agriculture

- drone images
- soil sensor data
- field inspection forms
- weather API data
- satellite imagery
- farm management software exports

## Mandatory Traceability Fields

Every source ingestion path must capture these fields.

- `source_type`
- `source_category`
- `source_system`
- `source_identifier`
- `source_uri`
- `ingestion_timestamp`
- `original_payload_hash`
- `tenant_id`
- `actor_type`
- `actor_id`
- `retention_policy`
- `compliance_classification`
- `data_residency`
- `consent_ref`
- `case_id`
- `correlation_id`
- `evidence_event_id`

For file-like sources, also capture:

- `original_filename`
- `content_type`
- `size_bytes`
- `storage_uri`
- `checksum_sha256`

For email sources, also capture:

- `mailbox`
- `message_id`
- `thread_id`
- `from_address`
- `to_addresses`
- `cc_addresses`
- `subject`
- `received_at`
- `attachment_count`

For API/webhook sources, also capture:

- `http_method`
- `endpoint`
- `external_request_id`
- `idempotency_key`
- `callback_url`
- `response_status`

For enterprise system and database sources, also capture:

- `connector_name`
- `external_record_id`
- `external_record_version`
- `query_ref`
- `import_batch_id`

For machine, sensor, and vision sources, also capture:

- `source_device_id`
- `mission_id`
- `capture_timestamp`
- `latitude`
- `longitude`
- `altitude`
- `telemetry_ref`

For streaming sources, also capture:

- `topic`
- `partition`
- `offset`
- `event_id`
- `event_timestamp`

## Connector Design

Use a connector interface instead of hardcoding ingestion logic per source.

Recommended contracts:

- `DataSourceConnector`
- `DataSourceConnectorRegistry`
- `IngestionRequest`
- `IngestionResult`
- `SourceDescriptor`
- `SourcePayload`
- `SourceTrace`

Each connector should:

- validate tenant and policy context
- normalize source metadata
- compute or verify payload hashes
- store large binary payloads in object storage
- persist structured metadata in PostgreSQL
- create immutable evidence events
- support idempotency where source systems can retry

## V1 Build Order

1. Define common source category and source type enums.
2. Define mandatory source traceability model.
3. Add ingestion connector interfaces.
4. Implement Upload Connector.
5. Implement Manual Entry Connector.
6. Implement REST/Webhook Connector.
7. Implement Object Storage Connector.
8. Implement Email Connector.
9. Implement Database Import Connector for PostgreSQL first.
10. Add admin UI for connector configuration and source health.

## Non-Goals For V1

- Vendor-specific SAP, DATEV, Workday, Salesforce, or ServiceNow connectors
- Kafka/RabbitMQ production streaming connector
- Real-time CCTV/LiDAR ingestion
- Full DICOM PACS integration
- Google Drive, OneDrive, Dropbox, or SharePoint production integrations

These should be added only after the generic connector model, source traceability fields, and evidence flow are stable.
