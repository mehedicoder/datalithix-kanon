# Change Control

## Behavior Change Classification

A change is behavior-changing if user-visible output, API behavior, security decisions, workflow transitions, or persistence side effects change.

If uncertain, classify as behavior-changing until clarified.

## Required in Behavior-Changing PRs

1. `REQ-*` and `AC-*` references in spec files (`docs/spec.md` or `docs/specification/*`).
2. Spec diffs describing the changed behavior.
3. Traceability updates in `docs/sdd/TRACEABILITY.md`.
4. Tests covering the changed behavior.
5. `docs/tasks.md` status/scope update when scope or progress changed.
6. Completed `.github/PULL_REQUEST_TEMPLATE.md` checklist.

## Required in Non-Behavior-Changing PRs

1. Mark task entry as `Spec: none` where applicable.
2. Keep traceability unchanged or note why no mapping changed.
3. Include tests if refactor risk warrants it.
