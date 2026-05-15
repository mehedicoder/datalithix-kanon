# Traceability

Use this format to map requirements to acceptance, tests, and code.

## ID Format

- Requirement: `REQ-<MODULE>-###`
- Acceptance criteria: `AC-<MODULE>-###`

`<MODULE>` should match repository module naming in uppercase without `kanon-` (example: `TENANT`, `POLICY`, `UI`).

## Mapping Template

```markdown
REQ-<MODULE>-###
- Spec section: docs/specification/<module>.md#...
- Acceptance criteria: AC-<MODULE>-###, AC-<MODULE>-###
- Tests: <test class>#<method>, <test class>#<method>
- Implementation: <module>/<package>/<class>
```

## Initial Priority Domains

- `TENANT` (`kanon-tenant`)
- `POLICY` (`kanon-policy`)
- `UI` (`kanon-ui`)

Remaining modules should be added incrementally.

## Initial Mapping Snapshot

REQ-TENANT-001
- Spec section: `docs/specification/tenant.md#req-tenant-001-tenant-scoped-access-context`
- Acceptance criteria: `AC-TENANT-001`, `AC-TENANT-002`, `AC-TENANT-003`
- Tests:
  - `kanon-policy/src/test/java/ai/datalithix/kanon/policy/security/SecurityFoundationTest.java#deniesAccessWhenTenantMismatch`
  - `kanon-policy/src/test/java/ai/datalithix/kanon/policy/security/SecurityFoundationTest.java#deniesWhenContextIsNull`
  - Coverage gap: no direct `kanon-tenant` service/repository test currently validates tenant-scoped query filtering.
- Implementation: `kanon-tenant/src/main/java/ai/datalithix/kanon/tenant/service/*`

REQ-TENANT-002
- Spec section: `docs/specification/tenant.md#req-tenant-002-membership-and-role-scope-handling`
- Acceptance criteria: `AC-TENANT-004`, `AC-TENANT-005`, `AC-TENANT-006`
- Tests:
  - `kanon-policy/src/test/java/ai/datalithix/kanon/policy/security/SecurityFoundationTest.java#deniesWhenReviewerNotAssigned`
  - `kanon-policy/src/test/java/ai/datalithix/kanon/policy/security/SecurityFoundationTest.java#allowsWhenReviewerIsAssignedToCase`
  - `kanon-policy/src/test/java/ai/datalithix/kanon/policy/security/SecurityFoundationTest.java#deniesWhenUserNotAssignedToTask`
  - Coverage gap: no direct `kanon-tenant` tests currently verify membership scope persistence/retrieval behavior.
- Implementation: `kanon-tenant/src/main/java/ai/datalithix/kanon/tenant/model/*`, `kanon-tenant/src/main/java/ai/datalithix/kanon/tenant/service/*`

REQ-POLICY-001
- Spec section: `docs/specification/policy.md#req-policy-001-default-deny-authorization`
- Acceptance criteria: `AC-POLICY-001`, `AC-POLICY-002`, `AC-POLICY-003`
- Tests:
  - `kanon-policy/src/test/java/ai/datalithix/kanon/policy/security/SecurityFoundationTest.java#deniesWhenMissingPermission`
  - `kanon-policy/src/test/java/ai/datalithix/kanon/policy/security/SecurityFoundationTest.java#deniesWhenContextIsNull`
  - `kanon-policy/src/test/java/ai/datalithix/kanon/policy/security/SecurityFoundationTest.java#deniesWhenResourceIsNull`
  - `kanon-policy/src/test/java/ai/datalithix/kanon/policy/security/SecurityFoundationTest.java#allowsWhenPermissionPresent`
- Implementation: `kanon-policy/src/main/java/ai/datalithix/kanon/policy/security/*`

REQ-POLICY-002
- Spec section: `docs/specification/policy.md#req-policy-002-security-aware-redaction`
- Acceptance criteria: `AC-POLICY-004`, `AC-POLICY-005`, `AC-POLICY-006`
- Tests: `kanon-ui/src/test/java/ai/datalithix/kanon/ui/component/RedactedTextTest.java`
- Implementation: `kanon-policy/src/main/java/ai/datalithix/kanon/policy/security/DefaultRedactionService.java`, `kanon-ui/src/main/java/ai/datalithix/kanon/ui/component/RedactedText.java`

REQ-UI-001
- Spec section: `docs/specification/ui.md#req-ui-001-permission-aware-route-and-action-access`
- Acceptance criteria: `AC-UI-001`, `AC-UI-002`, `AC-UI-003`
- Tests: `kanon-ui/src/test/java/ai/datalithix/kanon/ui/security/AdminRouteAccessTest.java`
- Implementation: `kanon-ui/src/main/java/ai/datalithix/kanon/ui/security/AdminRouteAccess.java`

REQ-UI-002
- Spec section: `docs/specification/ui.md#req-ui-002-tenant-scoped-operational-views`
- Acceptance criteria: `AC-UI-004`, `AC-UI-005`, `AC-UI-006`
- Tests: `kanon-ui/src/test/java/ai/datalithix/kanon/ui/view/admin/TenantAdminViewTest.java`
- Implementation: `kanon-ui/src/main/java/ai/datalithix/kanon/ui/view/admin/*`

REQ-INGESTION-001
- Spec section: `docs/specification/ingestion.md#req-ingestion-001-connector-driven-ingestion-outcome`
- Acceptance criteria: `AC-INGESTION-001`, `AC-INGESTION-002`, `AC-INGESTION-003`
- Tests:
  - `kanon-ingestion/src/test/java/ai/datalithix/kanon/ingestion/service/IngestionOrchestrationServiceTest.java#ingestsSourceDataThroughConnector`
  - `kanon-ingestion/src/test/java/ai/datalithix/kanon/ingestion/service/IngestionOrchestrationServiceTest.java#returnsFailedResultWhenNoConnectorFound`
- Implementation: `kanon-ingestion/src/main/java/ai/datalithix/kanon/ingestion/service/IngestionOrchestrationService.java`, `kanon-ingestion/src/main/java/ai/datalithix/kanon/ingestion/service/*Connector.java`

REQ-INGESTION-002
- Spec section: `docs/specification/ingestion.md#req-ingestion-002-traceability-and-audit-emission`
- Acceptance criteria: `AC-INGESTION-004`, `AC-INGESTION-005`, `AC-INGESTION-006`
- Tests:
  - `kanon-ingestion/src/test/java/ai/datalithix/kanon/ingestion/service/IngestionOrchestrationServiceTest.java#savesSourceDescriptor`
  - `kanon-ingestion/src/test/java/ai/datalithix/kanon/ingestion/service/IngestionOrchestrationServiceTest.java#savesSourceTrace`
  - `kanon-ingestion/src/test/java/ai/datalithix/kanon/ingestion/service/IngestionOrchestrationServiceTest.java#appendsEvidenceEvent`
  - `kanon-ingestion/src/test/java/ai/datalithix/kanon/ingestion/service/IngestionOrchestrationServiceTest.java#publishesAuditEvent`
  - `kanon-ingestion/src/test/java/ai/datalithix/kanon/ingestion/service/IngestionOrchestrationServiceTest.java#publishesFailureAuditEventOnConnectorError`
  - `kanon-ingestion/src/test/java/ai/datalithix/kanon/ingestion/service/IngestionOrchestrationServiceTest.java#evidenceEventHasSourceMetadata`
- Implementation: `kanon-ingestion/src/main/java/ai/datalithix/kanon/ingestion/service/IngestionOrchestrationService.java`

REQ-INGESTION-003
- Spec section: `docs/specification/ingestion.md#req-ingestion-003-connector-health-tracking`
- Acceptance criteria: `AC-INGESTION-007`, `AC-INGESTION-008`, `AC-INGESTION-009`
- Tests:
  - `kanon-ingestion/src/test/java/ai/datalithix/kanon/ingestion/service/IngestionOrchestrationServiceTest.java#updatesConnectorHealthOnSuccess`
  - `kanon-ingestion/src/test/java/ai/datalithix/kanon/ingestion/service/IngestionOrchestrationServiceTest.java#updatesConnectorHealthOnFailure`
- Implementation: `kanon-ingestion/src/main/java/ai/datalithix/kanon/ingestion/service/IngestionOrchestrationService.java`, `kanon-ingestion/src/main/java/ai/datalithix/kanon/ingestion/service/ConnectorHealthRepository.java`

REQ-WORKFLOW-001
- Spec section: `docs/specification/workflow.md#req-workflow-001-policy-aware-action-applicability`
- Acceptance criteria: `AC-WORKFLOW-001`, `AC-WORKFLOW-002`, `AC-WORKFLOW-003`
- Tests:
  - `kanon-workflow/src/test/java/ai/datalithix/kanon/workflow/model/WorkflowPlanningProblemTest.java#filtersInitiallyApplicableActionsUsingFactsPermissionsAndPolicy`
- Implementation: `kanon-workflow/src/main/java/ai/datalithix/kanon/workflow/model/WorkflowPlanningProblem.java`

REQ-WORKFLOW-002
- Spec section: `docs/specification/workflow.md#req-workflow-002-planner-fallback-behavior`
- Acceptance criteria: `AC-WORKFLOW-004`, `AC-WORKFLOW-005`, `AC-WORKFLOW-006`
- Tests:
  - `kanon-workflow/src/test/java/ai/datalithix/kanon/workflow/service/EmbabelWorkflowPlannerTest.java#usesEmbabelClientPlanWhenGoalIsSatisfied`
  - `kanon-workflow/src/test/java/ai/datalithix/kanon/workflow/service/EmbabelWorkflowPlannerTest.java#fallsBackWhenEmbabelCannotPlan`
- Implementation: `kanon-workflow/src/main/java/ai/datalithix/kanon/workflow/service/EmbabelWorkflowPlanner.java`, `kanon-workflow/src/main/java/ai/datalithix/kanon/workflow/service/DefaultWorkflowPlanner.java`

REQ-WORKFLOW-003
- Spec section: `docs/specification/workflow.md#req-workflow-003-review-action-state-transitions-with-evidence`
- Acceptance criteria: `AC-WORKFLOW-007`, `AC-WORKFLOW-008`, `AC-WORKFLOW-009`
- Tests:
  - `kanon-workflow/src/test/java/ai/datalithix/kanon/workflow/service/DefaultWorkflowTaskCommandServiceTest.java#approveUpdatesWorkflowAndAppendsEvidence`
  - `kanon-workflow/src/test/java/ai/datalithix/kanon/workflow/service/DefaultWorkflowTaskCommandServiceTest.java#escalateKeepsTaskAuditableAndNotExportReady`
- Implementation: `kanon-workflow/src/main/java/ai/datalithix/kanon/workflow/service/DefaultWorkflowTaskCommandService.java`

REQ-API-001
- Spec section: `docs/specification/api.md#req-api-001-health-and-metrics-contract-stability`
- Acceptance criteria: `AC-API-001`, `AC-API-002`, `AC-API-003`
- Tests:
  - `kanon-api/src/test/java/ai/datalithix/kanon/api/health/HealthControllerTest.java#healthReturnsUp`
  - `kanon-api/src/test/java/ai/datalithix/kanon/api/health/HealthControllerTest.java#detailedReturnsOverallHealth`
  - `kanon-api/src/test/java/ai/datalithix/kanon/api/health/HealthControllerTest.java#componentsReturnsAllComponents`
  - `kanon-api/src/test/java/ai/datalithix/kanon/api/health/DefaultHealthServiceTest.java#overallHealthIsDownWhenAnyDown`
  - `kanon-api/src/test/java/ai/datalithix/kanon/api/metrics/MetricsControllerTest.java#metricsReturnsSnapshot`
- Implementation: `kanon-api/src/main/java/ai/datalithix/kanon/api/health/*`, `kanon-api/src/main/java/ai/datalithix/kanon/api/metrics/MetricsController.java`

REQ-API-002
- Spec section: `docs/specification/api.md#req-api-002-annotation-task-endpoint-delegation-and-error-translation`
- Acceptance criteria: `AC-API-004`, `AC-API-005`, `AC-API-006`
- Tests:
  - `kanon-api/src/test/java/ai/datalithix/kanon/api/annotation/AnnotationTaskControllerTest.java#createTaskDelegatesToService`
  - `kanon-api/src/test/java/ai/datalithix/kanon/api/annotation/AnnotationTaskControllerTest.java#syncTaskReturnsSyncRecord`
  - `kanon-api/src/test/java/ai/datalithix/kanon/api/annotation/AnnotationTaskControllerTest.java#syncTaskThrows404OnUnknown`
  - `kanon-api/src/test/java/ai/datalithix/kanon/api/annotation/AnnotationTaskControllerTest.java#retrySyncReturnsSyncRecord`
  - `kanon-api/src/test/java/ai/datalithix/kanon/api/annotation/AnnotationTaskControllerTest.java#retrySyncThrows404OnUnknown`
- Implementation: `kanon-api/src/main/java/ai/datalithix/kanon/api/annotation/AnnotationTaskController.java`

REQ-API-003
- Spec section: `docs/specification/api.md#req-api-003-intake-and-media-upload-session-behavior`
- Acceptance criteria: `AC-API-007`, `AC-API-008`, `AC-API-009`, `AC-API-010`, `AC-API-011`
- Tests:
  - `kanon-api/src/test/java/ai/datalithix/kanon/api/intake/IntakeControllerTest.java#createSessionReturnsSessionWithId`
  - `kanon-api/src/test/java/ai/datalithix/kanon/api/intake/IntakeControllerTest.java#listSessionsReturnsTenantSessions`
  - `kanon-api/src/test/java/ai/datalithix/kanon/api/intake/IntakeControllerTest.java#uploadFilesAddsToSession`
  - `kanon-api/src/test/java/ai/datalithix/kanon/api/intake/IntakeControllerTest.java#dispatchRemovesSessionAndReturnsResult`
  - `kanon-api/src/test/java/ai/datalithix/kanon/api/media/MediaUploadControllerTest.java#uploadReturnsResponseWithObjectKey`
  - `kanon-api/src/test/java/ai/datalithix/kanon/api/media/MediaUploadControllerTest.java#presignedUrlUsesWriteMethod`
- Implementation: `kanon-api/src/main/java/ai/datalithix/kanon/api/intake/IntakeController.java`, `kanon-api/src/main/java/ai/datalithix/kanon/api/media/MediaUploadController.java`
