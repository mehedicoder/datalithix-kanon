# Module Structure

## Recommended implementation order

1. `kanon-common`
2. `kanon-tenant` + `kanon-domain`
3. `kanon-policy`
4. `kanon-ai-routing`
5. `kanon-workflow`
6. `kanon-evidence`
7. `kanon-annotation`
8. `kanon-api` + `kanon-ui`
9. `kanon-bootstrap`
10. `kanon-dataset` (post-MVP)
11. `kanon-training` (post-MVP)
12. `kanon-model-registry` (post-MVP)
13. `kanon-active-learning` (post-MVP)

## High-value future adapters

- PostgreSQL repositories
- MongoDB evidence store
- LangChain4j chat models
- Spring AI abstractions
- Embabel planner bridge
- Label Studio annotation node adapter
- CVAT annotation node adapter
- Annotation execution mode policy
- Annotation task sync service
- OpenTelemetry tracing

## Training Pipeline Modules (Post-MVP)

| Module | Purpose |
| --- | --- |
| `kanon-dataset` | Dataset curation, versioning, split management, metadata calculation, and export adapters. |
| `kanon-training` | Training job orchestration, compute backend SPI, job lifecycle management, and metrics ingestion. |
| `kanon-model-registry` | Model versioning, lineage tracking, evaluation runner, deployment management, and promotion gates. |
| `kanon-active-learning` | Active learning strategy engine, candidate selection, cycle management, and retraining trigger. |
