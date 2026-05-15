# Policy Module Specification (`kanon-policy`)

## Scope

Authorization, redaction, and security decision behavior across policy services.

## Baseline Requirements

### REQ-POLICY-001: Default-Deny Authorization

- AC-POLICY-001: Access is denied unless explicit permission and context checks pass.
- AC-POLICY-002: Missing or invalid access-control context results in denial.
- AC-POLICY-003: Denial behavior is observable at API/service boundaries.

### REQ-POLICY-002: Security-Aware Redaction

- AC-POLICY-004: Restricted fields are redacted unless explicit read permission allows visibility.
- AC-POLICY-005: Redaction preserves non-sensitive context required for operations.
- AC-POLICY-006: Redaction behavior is test-covered in component/service tests.

## Open Items

- NEEDS_DECISION: define the minimum audit payload for denied-read events where sensitive attributes are present.
- ASSUMPTION: security events are emitted for denied access and sensitive access decisions.
