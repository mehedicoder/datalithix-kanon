# SDD Migration Prompt (Production-Safe, Repo-Tailored)

```text
You are a senior engineer working in this repository:

- Root: datalithix-kanon
- Build: Maven multi-module
- Language/runtime: Java 25
- Frameworks: Spring Boot 4, Vaadin 25
- Persistence in current codebase: PostgreSQL (Flyway migrations) + Mongo usage in bootstrap components
- Existing docs baseline:
  - docs/contributing.md
  - docs/spec.md
  - docs/tasks.md
  - docs/module-structure.md
  - docs/security-access-control.md
  - docs/multi-tenant-governance-architecture.md
  - other architecture/testing docs under docs/

Goal:
Introduce an enforceable Specification-Driven Development (SDD) workflow (Fr8ix-style governance) without breaking existing runtime behavior unless explicitly approved.

Hard constraints (non-negotiable):
1) Do not intentionally change runtime behavior unless explicitly requested.
2) No destructive DB/workflow actions without explicit approval.
3) Preserve tenant isolation, authz/authn boundaries, and security controls.
4) Any behavior-changing code must include spec updates + traceability + tests in the same change set.
5) Prefer existing repo patterns and structure over introducing duplicate governance artifacts.
6) Keep changes reviewable and incremental (no big-bang rewrite).
7) Minimize documentation verbosity; governance docs must stay lean and operational.

Execution model:
- Work in phases.
- At each phase: show plan, execute, then report outputs and unresolved decisions.
- If uncertain, record:
  - NEEDS_DECISION: ...
  - ASSUMPTION: ...
- Never invent current behavior; if proposing future behavior, label it PROPOSED.

================================================================
PHASE 0 — REPO DISCOVERY + GAP ANALYSIS (MANDATORY FIRST)
================================================================

1) Analyze:
   - root pom.xml and module list
   - docs/ current governance/spec/task files
   - test distribution (unit/integration/UI)
   - security and tenant-related modules/docs
2) Produce:
   - current-state governance map
   - gap analysis vs strict SDD
   - risk list for migration
   - recommended rollout order by module

Output required:
- Discovery Report
- Proposed rollout order
- Files to create vs files to extend (with rationale)

Do not edit files before this report.

================================================================
PHASE 1 — LEAN FOUNDATION (NO DUPLICATE SOURCE OF TRUTH)
================================================================

Create only missing assets; extend existing ones whenever equivalent already exists.

A) Extend existing files:
- docs/contributing.md  (primary contributor policy; do not create duplicate root CONTRIBUTING.md unless explicitly requested)
- docs/spec.md          (or split with clear migration links)
- docs/tasks.md         (or convert into authoritative backlog/status file)

B) Create minimal SDD governance folder (required, lean only):
- docs/sdd/CONSTITUTION.md
- docs/sdd/CHANGE_CONTROL.md
- docs/sdd/TRACEABILITY.md

Rules for docs/sdd:
- Keep each file short, enforceable, and non-overlapping.
- Do not add extra SDD policy files unless a clear gap is demonstrated and approved.

C) Specification structure (module-aligned):
- docs/specification/README.md
- docs/specification/<module>.md for prioritized modules first (not all modules at once)

D) Agent policy:
- AGENTS.md at repo root (only if missing; otherwise update existing)
- Must reference:
  - docs/contributing.md
  - docs/sdd/*
  - relevant docs/specification/*

E) Skills (only if this repo actually uses in-repo skills as workflow artifacts):
- skills/README.md
- skills/sdd-docs/SKILL.md
- skills/behavior-change-gate/SKILL.md
- skills/test-verification/SKILL.md
- skills/pr-readiness/SKILL.md
- skills/changelog/SKILL.md (optional)
If skills framework is not active here, document as PROPOSED and skip file creation.

================================================================
PHASE 2 — BASELINE CURRENT BEHAVIOR INTO SPECS (INCREMENTS)
================================================================

Do not attempt full-repo baseline in one pass.
Process modules in rollout order from Phase 0.

For each module:
1) Read code + tests + relevant docs.
2) Document CURRENT behavior only.
3) Add IDs:
   - REQ-<MODULE>-###
   - AC-<MODULE>-###
4) ACs must be observable and testable.
5) Link uncertain points with NEEDS_DECISION / ASSUMPTION.

No target-state behavior unless labeled PROPOSED.

================================================================
PHASE 3 — TRACEABILITY + CHANGE CONTROL ENFORCEMENT
================================================================

Implement in docs/sdd/TRACEABILITY.md:
For each REQ:
- linked AC IDs
- linked spec section/file
- linked tests (class/method)
- linked implementation areas (module/package/class)

Implement in docs/sdd/CHANGE_CONTROL.md:
Behavior-changing PR must include:
- REQ/AC IDs
- spec diffs
- tests
- backlog/status update when scope/progress changes

Add PR checklist snippet to docs/contributing.md.

================================================================
PHASE 4 — TEST POLICY + VERIFICATION GATES
================================================================

Define required test levels in docs/contributing.md (primary) and reference from docs/sdd/CONSTITUTION.md:

Policy:
- Unit tests for changed logic
- Integration tests for workflow/persistence/security boundaries
- Vaadin UI integration tests for UI behavior changes
- Full mvn verify for shared-path or cross-module changes

Commands (PowerShell):
- .\mvnw.cmd test
- .\mvnw.cmd verify

If UI changes:
- require short manual verification note
- include screenshots or equivalent evidence references

================================================================
PHASE 5 — AGENT OPERATING CONTRACT
================================================================

In AGENTS.md enforce:

Mandatory pre-edit reads:
- docs/contributing.md
- docs/sdd/CONSTITUTION.md
- docs/sdd/CHANGE_CONTROL.md
- docs/sdd/TRACEABILITY.md
- relevant docs/specification/*
- relevant skills/* (only if skills are active in this repo)

Per-task flow:
1) Identify impacted modules/domains
2) Determine behavior-changing vs non-behavior-changing
3) If behavior-changing:
   - update REQ/AC
   - update docs/specification/*
   - update status/backlog doc
   - update traceability map
4) Preserve tenant/security boundaries
5) Add/adjust tests
6) No destructive actions without explicit approval

Required pre-edit response template:
Policy files read:
- ...
Impacted modules/domains:
- ...
Behavior-changing:
Yes/No
Required docs updates:
- ...
Required tests:
- ...

================================================================
PHASE 6 — BACKLOG/STATUS NORMALIZATION
================================================================

Use one authoritative status file only, chosen from existing structure:
- Prefer updating docs/tasks.md to become authoritative
  OR create docs/BACKLOG_AND_STATUS.md and deprecate docs/tasks.md with explicit pointer.

Must include:
- active initiatives
- scope
- status by initiative
- completed checklist items
- progress metrics (done/total)
- risks
- next actions

No hidden status tracking outside this file.

================================================================
PHASE 7 — DELIVERY OUTPUT (EACH EXECUTION CYCLE)
================================================================

After each implementation cycle, output:

1) File tree of created/updated SDD assets
2) Summary of enforced policies
3) REQ/AC coverage snapshot by module
4) Test baseline status (pass/fail + key failures)
5) Open NEEDS_DECISION items
6) Next 3 recommended migration tasks

Quality bar:
- precise, auditable language
- every requirement testable
- no vague ACs
- no duplicate governance sources without migration intent
- focused diffs
- minimal policy verbosity

Start now with:
1) Phase 0 discovery report for this repo.
2) A repo-specific phased rollout plan.
3) Execute Phase 1 only after presenting and confirming create-vs-extend decisions.
```
