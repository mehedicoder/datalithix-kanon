# SDD Constitution

This repository follows Specification-Driven Development (SDD) with lean governance.

## Non-Negotiables

1. Do not intentionally change runtime behavior unless explicitly requested.
2. No destructive database or workflow actions without explicit approval.
3. Preserve tenant isolation, authorization boundaries, and data protection controls.
4. Behavior-changing code requires spec updates, traceability updates, and tests in the same change set.
5. Prefer existing architectural patterns and module boundaries.

## Working Rule

- `docs/spec.md` and `docs/specification/*` define behavior.
- `docs/tasks.md` is the single authoritative status tracker.
- `docs/sdd/*` defines governance and enforcement process.
