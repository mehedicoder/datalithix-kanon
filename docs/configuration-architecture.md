# Configuration Architecture

KANON uses a layered configuration model. YAML is useful for bootstrap and reusable templates, but the database must become the runtime source of truth for tenant-specific active configuration.

## Principle

> YAML defines reusable templates. PostgreSQL stores activated tenant configuration.

Do not bind business logic directly to raw YAML maps. Load YAML into typed contracts, validate them, register them, and later persist activated versions in the database.

## Configuration Layers

### Layer 1: Code Contracts

Java records, classes, interfaces, and enums define the configuration shape.

Examples:

- `DomainConfiguration`
- `TenantConfiguration`
- `WorkflowTemplate`
- `AgentDefinition`
- `ModelRoutingPolicy`
- `ConnectorDefinition`
- `PolicyTemplate`

Benefits:

- type safety
- validation
- stable UI contracts
- stable persistence contracts

### Layer 2: YAML Templates

Use YAML for reusable, Git-versioned, bootstrap-friendly configuration packs.

Good YAML candidates:

- default domain packs
- workflow templates
- agent definitions
- model routing templates
- connector type defaults
- policy templates
- local development configs
- test fixtures
- seed data

Example paths:

- `config/templates/domains/accounting.yml`
- `config/templates/domains/hr.yml`
- `config/templates/workflows/invoice-processing.yml`
- `config/templates/policies/eu-ai-act.yml`
- `config/templates/models/accounting-routing.yml`
- `config/templates/connectors/email-ingestion.yml`

### Layer 3: Database-Backed Active Configuration

Use PostgreSQL for active tenant configuration.

Database-backed configuration should store:

- tenant overrides
- active versions
- admin edits
- activation/deactivation state
- who changed what and when
- which config version was active for a workflow run

These should move to DB-backed runtime configuration early:

- tenant secrets and credential references
- connector credentials and endpoint references
- active tenant model preferences
- human role mappings
- tenant-specific thresholds
- environment-specific endpoints
- workflow activation state
- UI customizations per tenant

## Recommended Modules

Start with one module and split later only if complexity requires it.

Recommended first module:

- `kanon-config`

Possible future split:

- `kanon-config-contracts`
- `kanon-config-loader`
- `kanon-config-registry`
- `kanon-config-persistence`

## Runtime Flow

Startup and local development flow:

1. Read YAML templates.
2. Parse into typed contract objects.
3. Validate references and required fields.
4. Register templates in an in-memory registry.
5. Optionally seed database-backed active config versions.

Runtime flow:

1. Resolve active tenant configuration from database.
2. Fall back to registered templates only for bootstrap/defaults.
3. Pass typed configuration objects to routing, workflow, connector, policy, and UI services.
4. Record active configuration version ids in workflow and evidence records.

## Validation Rules

Validate at load/import time:

- required ids and display names
- known domain types
- known task types
- known source categories and source types
- workflow steps reference known agents/actions
- model routes reference existing model profiles or template ids
- connector definitions use valid source types
- policy ids are known
- tenant overrides reference existing templates
- no duplicate ids inside a configuration pack

## Audit and Versioning

Active configuration must be versioned and auditable.

Record:

- configuration id
- tenant id
- config type
- template id
- active version
- activation state
- created/updated audit columns
- activated by
- activated at
- deactivated by
- deactivated at
- change reason

Every admin change to active configuration must create an evidence/security event.

## MVP Scope

For MVP, implement:

- typed configuration contracts
- YAML template loader
- validation service
- in-memory template registry
- sample Accounting and HR domain templates
- sample workflow, agent, model routing, connector, and policy templates
- repository ports for active config versions
- seed/import service contract for later database persistence

Concrete PostgreSQL persistence can follow after the contracts and YAML loader are stable.

