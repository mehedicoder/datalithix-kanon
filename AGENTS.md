# Agent Operating Contract

This file is mandatory for coding agents operating in this repository.

## Required Reads Before Edits

1. `docs/contributing.md`
2. `docs/sdd/CONSTITUTION.md`
3. `docs/sdd/CHANGE_CONTROL.md`
4. `docs/sdd/TRACEABILITY.md`
5. Relevant files in `docs/specification/*`

## Required Per-Task Flow

1. Identify impacted modules/domains.
2. Classify change as behavior-changing or non-behavior-changing.
3. If behavior-changing:
   - Update `REQ-*` / `AC-*` entries.
   - Update affected module spec under `docs/specification/*` (or `docs/spec.md`).
   - Update `docs/sdd/TRACEABILITY.md`.
   - Update `docs/tasks.md` status if scope/progress changed.
4. Preserve tenant/security boundaries.
5. Add applicable tests.
6. Do not perform destructive actions without explicit approval.

## Required Pre-Edit Response Format

Policy files read:
- ...

Impacted module/domain:
- ...

Behavior-changing:
Yes/No

Required docs updates:
- ...

Required tests:
- ...
