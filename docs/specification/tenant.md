# Tenant Module Specification (`kanon-tenant`)

## Scope

Tenant, organization, workspace, membership, role context, and user profile behavior exposed by tenant services and consumed by API/UI flows.

## Baseline Requirements

### REQ-TENANT-001: Tenant-Scoped Access Context

- AC-TENANT-001: Tenant-scoped operations require tenant context before data access.
- AC-TENANT-002: Missing context fails closed (no implicit broad access).
- AC-TENANT-003: Behavior is testable through service/repository access checks.

### REQ-TENANT-002: Membership and Role Scope Handling

- AC-TENANT-004: Membership scope is explicit (tenant/organization/workspace) in access decisions.
- AC-TENANT-005: Role assignments are resolved with scope context, not global assumptions.
- AC-TENANT-006: Cross-tenant membership leakage is denied.

## Open Items

- NEEDS_DECISION: confirm whether workspace-level overrides should always supersede organization defaults in all admin flows.
- ASSUMPTION: current baseline follows default-deny when scope resolution is ambiguous.
