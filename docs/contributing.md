# 🛠 Contributing to Kanon Platform

This document defines the standards for human and agentic contributors. Kanon is a **Java 25, Spring Boot 4.0, and Embabel-powered** system. We prioritize **Structured Concurrency**, **Domain-Agnostic Configuration**, and **Immutable Evidence**.

---

## 🏗 Architectural Principles

### 1. The "Agent-First" Rule
Do not write imperative "Manager" classes for business logic. If a task involves a decision, it must be implemented as an **Embabel Action** or **Goal**.
* **Hardcoded logic** = Technical debt.
* **Configurable Policy** = The Kanon Way.

### 2. High-Performance Concurrency (Project Loom)
Kanon targets **Java 25**.
* **NEVER** use `synchronized` or manual `ThreadPoolExecutor`.
* **ALWAYS** favor **Virtual Threads** for I/O-bound tasks (LLM calls, DB queries, API syncs) to maximize system **throughput**.
* **USE Parallel Streams** or **Platform Threads** for CPU-bound tasks (model training, video processing, heavy math, complex GOAP planning) to minimize **latency**.
* **USE** `StructuredTaskScope` for fan-out/fan-in operations to ensure clean lifecycle management and automatic cancellation.
* **USE** `ScopedValue` for cross-cutting context (Tenant, Trace ID) instead of `ThreadLocal`.

### 3. Module Boundaries
Respect the modular monolithic boundaries. Do not introduce circular dependencies.
1. `kanon-common`: Zero-dependency utilities, Records, and Constants.
2. `kanon-tenant / kanon-domain`: Multi-tenancy and Metadata definitions.
3. `kanon-policy`: The decision engine.
4. `kanon-ai-routing`: Model abstraction and cost/latency logic.
5. `kanon-workflow`: GOAP planning and state management.
6. `kanon-training`: Dataset snapshotting and training job orchestration.
7. `kanon-evidence`: Immutable MongoDB ledger.
8. `kanon-annotation`: Adapters for Label Studio and CVAT.
9. `kanon-api / kanon-ui`: Vaadin-based Command UI.
10. `kanon-bootstrap`: Spring Application entry point.

---

## 💻 Implementation Standards

### Java Style
* **Records over Classes:** Use `record` for all DTOs, events, and configuration.
* **Sealed Hierarchies:** Use `sealed interface` for domain entities (Source types, Agent types) to leverage pattern matching.
* **Pattern Matching:** Use `switch` expressions with pattern matching for logic branching.
* **Var:** Use `var` for local variables where the type is obvious.

### Agent & LLM Logic
* All LLM interactions must go through `LlmService`.
* Prompt templates live in `kanon-ai-routing` or configuration, never hardcoded in logic.
* **Embabel Actions:** The `@Description` annotation is treated as **functional code**. Write descriptions so a Planner understands exactly when and why to use the action.

### Data & Persistence
* **PostgreSQL:** Stores the "Active Truth" (State, Configuration, Tenants).
* **MongoDB:** Stores the "Historical Truth" (Evidence Ledger, Audit Events).
* **Object Storage:** Stores the "Payloads" (Videos, PDFs, Images).

---

## 🔄 Workflow & PRs

### 1. Evidence Emissions
Any service method that modifies state **MUST** emit an `EvidenceEvent`. If a method doesn't log to the ledger, it doesn't exist for audit purposes.

### 2. Testing
* **Unit Tests:** High coverage for `kanon-policy` and `kanon-common`.
* **Architecture Tests:** Use ArchUnit to ensure `kanon-core` modules never import `kanon-ui`.
* **Agent Simulation:** Verify GOAP plans in `kanon-workflow` using the Embabel test harness.

### 3. Agentic Contributor Instruction
> **AI Agent:** When implementing a task:
> 1. Identify the correct module based on the **Module Structure**.
> 2. Check for an existing `sealed interface` in `kanon-common`.
> 3. Use `StructuredTaskScope.ShutdownOnFailure()` for concurrent operations.
> 4. Distinguish between I/O throughput (Virtual Threads) and CPU latency (Parallel Streams).
> 5. Ensure your solution is **domain-agnostic** (it should work for both Accounting and HR via YAML config).

---

## 🚀 Getting Started
```bash
# Apply code formatting
./mvnw spotless:apply

# Run local infrastructure
docker compose up -d postgres mongodb minio

# Start the bootstrap module
mvn -pl kanon-bootstrap spring-boot:run
```

---

## SDD Governance (Lean)

Kanon uses a lean Specification-Driven Development workflow.

Source-of-truth governance files:

- `docs/sdd/CONSTITUTION.md`
- `docs/sdd/CHANGE_CONTROL.md`
- `docs/sdd/TRACEABILITY.md`
- `docs/spec.md` and `docs/specification/*`
- `docs/tasks.md` as the authoritative implementation status file

### Behavior-Change Gate

Any behavior-changing PR must include, in the same change set:

1. Updated `REQ-*` and `AC-*` entries in spec docs.
2. Updated mapping in `docs/sdd/TRACEABILITY.md`.
3. Tests that verify the changed behavior.
4. Scope/progress update in `docs/tasks.md` where applicable.

### Required Verification Levels

- Unit tests for changed logic.
- Integration tests for workflow, persistence, and security boundaries.
- Vaadin UI integration tests for UI behavior changes.
- Full `verify` for shared-path or cross-module changes.

PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```
