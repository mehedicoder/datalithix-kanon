# Scalability Architecture

Scalability is a primary design constraint for KANON. Every implementation should assume tenant growth, larger datasets, more connectors, more model invocations, longer evidence history, and higher UI concurrency.

## Principle

- Design for scale from the first implementation.
- Keep synchronous request paths short.
- Move heavy work to asynchronous jobs, queues, or workflow execution.
- Store large payloads in object storage.
- Query with tenant-aware indexes.
- Page, stream, and filter data instead of loading everything.
- Make operations idempotent and retryable.
- Measure and observe before optimizing blindly.

## Coding Rules

- Do not load unbounded lists into memory.
- Every list API must support pagination, sorting, and filtering.
- Every grid-backed query must be server-side paginated.
- Every tenant-owned table must have indexes aligned with tenant, case, status, timestamp, and lookup patterns.
- Long-running operations must run asynchronously.
- Connectors must support idempotency keys or source identifiers to handle retries.
- Model invocation must use timeouts, retries, rate limits, concurrency limits, and fallback handling.
- Evidence writes should be append-only and optimized for high write volume.
- File/video/media payloads must not pass through memory unnecessarily.
- Avoid database transactions that wrap slow external calls.
- Avoid chatty service loops that trigger N+1 queries.
- Use batch operations where appropriate.

## Data and Storage

### PostgreSQL

Use PostgreSQL for structured state:

- tenants
- users and roles
- source traces
- connector configuration
- workflow state
- annotation metadata
- media metadata
- model profiles

Scalability requirements:

- tenant-first indexes
- status and timestamp indexes for operational dashboards
- optimistic locking for mutable rows
- pagination for all operational views
- migration review for every new table and index
- avoid storing large binary payloads

### MongoDB / Evidence Ledger

Use the evidence store for append-oriented audit events.

Scalability requirements:

- index by tenant id, case id, event type, actor type, and event timestamp
- support time-range queries
- support append-only writes
- support archive/retention strategy
- avoid rewriting historical records

### Object Storage

Use object storage for media and large source payloads.

Scalability requirements:

- tenant-aware buckets or key prefixes
- checksum validation
- multipart upload for large assets
- presigned URLs or controlled proxy downloads
- lifecycle/retention policies
- avoid moving large payloads through the application when direct upload is possible

## External Annotation Tool Scaling

Kanon must scale around Label Studio and CVAT without making either tool the system of record.

Requirements:

- store large annotation payloads and media in object storage, not in external tool metadata fields
- keep canonical task ids, case state, workflow state, approval state, and evidence in Kanon databases
- store external Label Studio and CVAT ids as references only
- partition annotation tasks by tenant, domain, media type, and workload where practical
- use asynchronous jobs for task push, result pull, import, export, and retry
- use queues for high-volume pre-annotation, external task creation, and result sync
- keep separate workers for AI pre-annotation and external result synchronization
- make external task creation and result sync idempotent
- avoid wrapping Label Studio or CVAT API calls in long database transactions
- record sync lag, queue depth, retry count, failure rate, and external tool health metrics

## Workflow and Agent Runtime

- Workflows should be resumable.
- Agent execution should be retryable where safe.
- Workflow steps should be idempotent.
- Use correlation ids for tracing.
- Separate orchestration state from payload storage.
- Keep agent execution stateless where possible.
- Use queues or executor pools for heavy processing.
- Apply backpressure for connector ingestion, model invocation, media processing, and export.

## Data Source Connectors

Connectors must be built for scale:

- idempotent ingestion
- batch import support
- incremental sync support where possible
- last checkpoint tracking
- retry with backoff
- dead-letter or failed-ingestion state
- connector health and lag metrics
- source-specific rate limits

## LLM and Model Routing

Model execution is a scaling bottleneck.

Requirements:

- configurable timeout
- configurable retry policy
- configurable concurrency limit
- configurable rate limit
- model health check
- fallback model profile
- local/API backend separation
- queue or async execution for expensive tasks
- evidence event for routing and fallback decisions

## UI Scalability

Vaadin views must stay responsive as data grows.

Requirements:

- lazy-loaded grids
- server-side pagination
- explicit filters for large datasets
- no unbounded in-memory grid data providers
- summary cards backed by aggregate queries
- reactive status updates without reloading entire views
- avoid large payload previews by default
- show metadata first and load payload only on authorized demand

## Security at Scale

Security must scale with data volume:

- tenant filters must be enforced at query level
- authorization checks must be reusable and testable
- avoid per-row authorization loops over large result sets where query predicates can enforce scope
- redaction should happen before data reaches UI components
- sensitive reads should be audited without blocking main workflows unnecessarily

## Observability

Scalable systems need measurement.

Track:

- ingestion throughput
- connector lag
- workflow queue depth
- model invocation latency
- model error rate
- fallback rate
- evidence write rate
- database query latency
- object storage latency
- UI endpoint latency
- authorization denial rate

## MVP Scalability Requirements

For MVP, implement:

- pagination/filtering for list APIs and grids
- tenant-aware indexes for new Flyway tables
- idempotency for upload, email, REST/webhook, database import, and object storage connectors
- async execution boundary for ingestion, model invocation, and media processing
- model timeout, retry, rate limit, and concurrency settings
- evidence indexes for tenant/case/time queries
- object storage for large payloads
- health metrics for connectors, model profiles, databases, object storage, and security configuration
