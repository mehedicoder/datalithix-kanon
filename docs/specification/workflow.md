# Workflow Module Specification (`kanon-workflow`)

## Scope

Workflow planning and task command behavior, including planner fallback and evidence-backed human review actions.

## Baseline Requirements

### REQ-WORKFLOW-001: Policy-Aware Action Applicability

- AC-WORKFLOW-001: Planning only includes actions applicable to facts, permissions, and policy constraints.
- AC-WORKFLOW-002: Blocked actions are excluded from initially applicable action sets.
- AC-WORKFLOW-003: Goal satisfaction checks are deterministic based on accumulated facts.

### REQ-WORKFLOW-002: Planner Fallback Behavior

- AC-WORKFLOW-004: Embabel planner output is used when a valid plan is returned.
- AC-WORKFLOW-005: Planner falls back to default planning when Embabel cannot produce a compliant plan.
- AC-WORKFLOW-006: Fallback rationale is visible in returned plan metadata.

### REQ-WORKFLOW-003: Review Action State Transitions with Evidence

- AC-WORKFLOW-007: Approval command sets approval state and export readiness consistently.
- AC-WORKFLOW-008: Escalation command sets review and approval states to escalated and keeps export disabled.
- AC-WORKFLOW-009: Each command appends workflow evidence with action-specific rationale.
