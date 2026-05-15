# Security and Access Control

This document defines who can see what in KANON. Security must be designed into every source, asset, annotation, workflow, model, evidence, and admin view.

## Principle

- Default deny.
- Tenant isolation is mandatory.
- Users see only data allowed by tenant, role, assignment, domain, policy, and data classification.
- Every sensitive read and every mutation must be auditable.
- UI visibility is not security by itself. APIs and repositories must enforce access too.

## Access Model

Use a combined model:

- RBAC for coarse permissions: what role can do.
- ABAC for contextual rules: which tenant, domain, case, source, classification, region, assignment, or purpose allows access.
- Ownership/assignment for operational work: reviewer sees assigned tasks and allowed case context.
- Policy engine for regulated constraints: PII, medical, HR, financial, EU-only, local-model-only, retention.

## Core Security Dimensions

Every protected record should be evaluated with these dimensions where applicable.

- `tenant_id`
- `organization_unit`
- `domain_type`
- `case_id`
- `workflow_id`
- `source_trace_id`
- `media_asset_id`
- `data_classification`
- `compliance_classification`
- `data_residency`
- `retention_policy`
- `owner_id`
- `assigned_user_id`
- `assigned_group_id`
- `purpose`
- `role`
- `permission`

## MVP Roles

| Role | Can see | Can do |
| --- | --- | --- |
| Platform Admin | Platform configuration metadata across tenants, but not tenant data payloads unless explicitly granted break-glass access. | Manage platform settings, tenant setup, connector templates, global health, and system-level configuration. |
| Tenant Admin | Only records belonging to their assigned tenant, subject to compliance restrictions. Tenant master data is read-only unless explicit edit permission is granted. | Manage tenant users, roles, organizations, workspaces, connectors, model profiles, policies, retention, and workflow settings only when scoped permissions allow. |
| Organization Admin | Only records belonging to their assigned organization, subject to compliance restrictions. Organization master data is read-only unless explicit edit permission is granted. | Manage organization workspaces, workspace users, workspace memberships, and organization-level configuration where scoped permissions allow. |
| Domain Manager | Cases, workflows, source traces, annotations, evidence summaries, and dashboards for assigned domains. | Assign work, review domain performance, manage domain workflow settings. |
| Reviewer / Annotator | Assigned cases/tasks and the minimum source/asset context required to review or correct work. | Review, correct, approve, reject, escalate, and add notes. |
| Auditor | Read-only evidence, lineage, decisions, access history, and approved outputs for authorized tenant/domain/cases. | Inspect and export audit records where policy allows. |
| Integration Service Account | Connector-specific ingestion or export scope. | Create source traces, upload payloads, trigger workflows, read callback status. |
| Model Operator | Model profile metadata, health, routing tests, and invocation summaries. | Configure model profiles if tenant policy allows; cannot view sensitive prompts/responses unless explicitly permitted. |
| Viewer | Read-only dashboards and assigned case summaries. | View allowed summaries; no mutation. |

## Suggested Permissions

Use action/resource permissions. Examples:

- `tenant.manage`
- `organization.manage`
- `workspace.manage`
- `user.manage`
- `role.manage`
- `policy.manage`
- `connector.configure`
- `connector.view`
- `source.ingest`
- `source.view`
- `asset.view`
- `asset.download`
- `annotation.view`
- `annotation.edit`
- `review.assign`
- `review.perform`
- `review.approve`
- `workflow.view`
- `workflow.execute`
- `evidence.view`
- `evidence.export`
- `model.configure`
- `model.invoke`
- `model.view_health`
- `admin.break_glass`

## Visibility Rules

### Source Traces

- Tenant Admin can see all source traces inside the tenant.
- Domain Manager can see source traces linked to assigned domains.
- Reviewer can see source traces only for assigned cases.
- Auditor can see source trace metadata and evidence links, but payload access depends on classification.
- Integration Service Account can see traces created by that connector or integration.

### Data Assets and Payloads

- Metadata access and payload access must be separate permissions.
- `asset.view` allows metadata.
- `asset.download` allows original payload access.
- Sensitive or regulated payloads require policy approval and evidence logging.
- Object storage access must be via short-lived presigned URLs or controlled proxy endpoints after authorization.

### Annotations and Reviews

- Human annotators, reviewers, and approvers must receive access through workspace memberships and workspace-scoped roles.
- Reviewer can see assigned annotations and necessary source context.
- Reviewer cannot see unrelated tenant data.
- Domain Manager can see annotations for assigned domain/cases.
- Auditor can see final/approved annotations and revision history where authorized.
- Draft, rejected, or sensitive annotations require explicit permission.

### Evidence Ledger

- Evidence is append-only.
- Evidence visibility is still permissioned.
- Auditor and Tenant Admin can see authorized evidence records.
- Reviewer sees evidence relevant to assigned case/task.
- Prompt, response, secret, and raw payload fields must be redacted unless the user has explicit sensitive-data permission.

### Model Configuration and Invocation

- Tenant Admin and Model Operator can view model profile metadata for their tenant.
- Only authorized admins can create, update, enable, disable, or test model profiles.
- API keys and secrets are never visible after save.
- Prompt/response logs are hidden or redacted by default.
- Model invocation summaries can be visible without exposing sensitive input/output.

### Connectors

- Tenant Admin can configure tenant connectors.
- Integration Service Accounts can perform connector-specific ingestion.
- Connector credentials must be secret references.
- Connector health can be visible to admins and operators without exposing payloads.

### Cross-Tenant Access

- No user can access another tenant's data by default.
- Platform Admin access to tenant data requires explicit break-glass flow.
- Break-glass requires reason, time limit, approval if configured, and immutable evidence.

### Administration Scope

- Super admin is platform-scoped and can create tenants, organizations, workspaces, tenant admins, organization admins, workspace users, memberships, roles, and platform configuration.
- Tenant admin can access Administration views, but all data, lists, forms, lookups, actions, and API results must be tenant-filtered to the tenant memberships and permissions they hold.
- Organization admin can access Administration views, but all data, lists, forms, lookups, actions, and API results must be organization-filtered to the organization memberships and permissions they hold.
- Organization admin is organization-scoped and can manage only organizations, workspaces, workspace users, and workspace memberships allowed by their memberships and permissions.
- Workspace users can only see and mutate resources in workspaces where they have explicit membership.
- Backend APIs must reject unauthorized administration mutations even when UI actions are hidden.
- Access to the Administration UI does not imply global access.
- Super admin has unrestricted platform-wide access.

### Master Data Write Rules

- Tenant-level master data is visible to Tenant Admin in read-only mode by default.
- Tenant-level master data editing requires explicit tenant-scoped edit permissions.
- Organization-level master data is visible to Organization Admin in read-only mode by default.
- Organization-level master data editing requires explicit organization-scoped edit permissions.
- Read-only visibility must still respect tenant, organization, workspace, classification, residency, and purpose filters.

## Data Classification

Use classification to drive visibility and model routing.

| Classification | Examples | Default handling |
| --- | --- | --- |
| Public | Public docs, public metadata | Broadest tenant-level read access |
| Internal | Internal workflow data | Tenant-scoped access |
| Confidential | contracts, invoices, HR records | Role and domain restricted |
| Restricted | PII, payroll, medical, legal, secrets | Explicit permission, redaction, strict evidence |
| Regulated | GDPR, HIPAA, HGB, EU AI Act relevant data | Policy-controlled access and model routing |

## UI Requirements

- Menus and views must hide unavailable actions based on permissions.
- Disabled actions should explain missing permission when appropriate.
- Grids must not load unauthorized rows.
- Detail views must not fetch unauthorized records.
- Sensitive columns must be redacted or omitted unless permission allows visibility.
- Every view should show tenant context.
- Admin views should show who last changed configuration and when.

## API and Repository Requirements

- Enforce authorization at API/service level.
- Enforce tenant filters in repository queries.
- Never rely only on Vaadin route/menu hiding.
- Add integration tests for cross-tenant isolation.
- Add tests for unauthorized reads, unauthorized writes, and sensitive field redaction.

## Audit Requirements

Record evidence/security events for:

- login success/failure
- role assignment changed
- permission changed
- connector configuration changed
- model profile changed
- source viewed
- asset downloaded
- annotation changed
- review approved/rejected/escalated
- evidence exported
- break-glass requested
- break-glass approved/denied
- break-glass access used
- unauthorized access denied

## MVP Security Scope

For MVP, implement:

- tenant isolation
- RBAC roles and permissions
- ABAC checks for tenant, domain, case assignment, data classification, and source/asset ownership
- Spring Security method-level checks for APIs/services
- Vaadin menu/action visibility from permissions
- redaction for secrets, prompts, responses, and sensitive payload fields
- evidence events for mutations and sensitive reads
- tests for cross-tenant isolation and unauthorized access
