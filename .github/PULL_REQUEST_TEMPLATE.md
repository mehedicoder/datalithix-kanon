## Summary

- What changed:
- Why:

## Change Classification

- [ ] Behavior-changing
- [ ] Non-behavior-changing (`Spec: none`)

## SDD Change-Control Checklist

- [ ] Updated `REQ-*` / `AC-*` references in `docs/spec.md` or `docs/specification/*` (if behavior-changing)
- [ ] Updated `docs/sdd/TRACEABILITY.md` mappings (if behavior-changing)
- [ ] Updated `docs/tasks.md` status/scope (if scope or progress changed)
- [ ] Preserved tenant isolation and security boundaries
- [ ] No destructive DB/workflow actions were performed without explicit approval

## Verification

- [ ] Unit tests for changed logic
- [ ] Integration tests for workflow/persistence/security boundaries (as applicable)
- [ ] Vaadin UI integration tests for UI behavior changes (as applicable)
- [ ] Ran `.\mvnw.cmd test`
- [ ] Ran `.\mvnw.cmd verify` (required for shared-path or cross-module changes)

### Test Results

- Command(s):
- Result:
- Key failures (if any):

## UI Manual Verification (UI changes only)

- [ ] Added concise manual verification note
- [ ] Added screenshots/evidence reference
