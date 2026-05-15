# UI Module Specification (`kanon-ui`)

## Scope

Vaadin operational UI behavior for navigation, admin views, and permission-aware interactions.

## Baseline Requirements

### REQ-UI-001: Permission-Aware Route and Action Access

- AC-UI-001: Unauthorized routes are denied even if users navigate directly.
- AC-UI-002: UI visibility controls do not replace backend authorization checks.
- AC-UI-003: Grid/list actions align with permission scope.

### REQ-UI-002: Tenant-Scoped Operational Views

- AC-UI-004: Lists and detail views operate on tenant-scoped data.
- AC-UI-005: Redacted components avoid leaking restricted values.
- AC-UI-006: UI behavior changes are covered by integration/component tests where present.

## Open Items

- NEEDS_DECISION: define minimum manual verification evidence format for UI behavior changes.
- ASSUMPTION: existing admin UI tests are baseline evidence for route/action constraints.
