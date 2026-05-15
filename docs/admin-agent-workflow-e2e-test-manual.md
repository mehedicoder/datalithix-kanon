# Administration Agent and Workflow E2E Test Manual

## Scope

This manual verifies the Administration lifecycle for tenants, tenant administration, agents, and workflows.

The intended operating model is:

- Platform admin creates tenants and tenant admins only.
- Tenant admin manages tenant-scoped administration and delegates organization/workspace membership.
- Organization or workspace administrators manage operational assets only inside their authorized organization/workspace.
- Workflows are workspace-scoped.
- Agents are expected to be workspace-scoped by product design, but the current `AgentProfile` model is still tenant-scoped.

## Current Design Mismatches

1. `PLATFORM_SUPER_ADMIN` currently has broad tenant, organization, workspace, user, agent, workflow, role, and audit permissions. That does not match a limited "Platform Admin creates only tenant and tenant admin" role.
2. `ORGANIZATION_ADMIN` currently does not have agent/workflow permissions by default.
3. `WorkflowDefinition` has `tenantId`, `organizationId`, and `workspaceId`, so workflow lifecycle can be safely filtered by workspace.
4. `AgentProfile` has `tenantId` only. Organization admin or workspace admin access to agents cannot be correctly enforced until agents carry `organizationId` and `workspaceId`.
5. Because agents are tenant-scoped today, granting organization admins `agent.*` permissions would expose all tenant agents to that organization admin.

## Required Follow-Up Design Work

### DB Migrations

No migration is required for the current archive/unarchive/delete UI changes.

Required before organization/workspace-scoped Agent administration:

1. Add `organization_id` and `workspace_id` to `agent_profile`.
2. Backfill existing rows to a safe default workspace or require explicit migration mapping.
3. Set both columns `NOT NULL`.
4. Add a tenant-first workspace index:
   - `(tenant_id, organization_id, workspace_id, agent_type, enabled)`
   - `(tenant_id, organization_id, workspace_id, status, updated_at DESC)`

### API and ACL Changes

Implemented now:

- Agent archive and restore use existing `AgentAdministrationService.archiveAgent` and `restoreAgent`.
- Workflow archive updates status to `RETIRED`.
- Workflow permanent delete uses `WorkflowDefinitionRepository.deleteById`.
- Workflow route/list/actions use existing workflow permissions and workspace row filtering.

Required before organization-scoped Agent administration:

- Extend `AgentProfile` with organization/workspace scope.
- Extend `AgentAdministrationService.createAgent` and repository mappings to persist organization/workspace scope.
- Add organization-scoped agent permissions only after the data model can enforce them.
- Add backend service authorization checks, not just UI visibility checks.

### UI Updates

Implemented now:

- Agent view supports "Show archived".
- Agent view supports restore for archived agents.
- Agent view supports permanent delete for archived agents.
- Agent view keeps create/update/enable/disable/archive.
- Workflow view supports "Show archived".
- Workflow view supports archive for active workflows.
- Workflow view supports restore and permanent delete for archived workflows.
- Workflow view keeps create/update/enable/disable.

### i18n Keys

Implemented now:

- `action.archive`
- `admin.agent.dialog.archive`
- `admin.agent.archived`
- `admin.agent.restored`
- `admin.agent.confirm.restore.title`
- `admin.agent.confirm.restore.message`
- `admin.workflow.dialog.archive`
- `admin.workflow.archived`
- `admin.workflow.deleted`

## Local Commands

Run UI and workflow tests:

```powershell
mvn -pl kanon-ui -am test
```

Compile the application:

```powershell
mvn -pl kanon-bootstrap -am -DskipTests compile
```

Run the application from the bootstrap module:

```powershell
cd C:\projects\datalithix-kanon\kanon-bootstrap
mvn spring-boot:run
```

## Test Data

Use unique names so cleanup is easy:

- Tenant key: `e2e-tenant-<date>`
- Organization key: `e2e-org-<date>`
- Workspace key: `e2e-workspace-<date>`
- Tenant admin username: `e2e-tenant-admin-<date>`
- Organization admin username: `e2e-org-admin-<date>`
- Agent name: `E2E Agent <date>`
- Workflow name: `E2E Workflow <date>`

## E2E Steps

### 1. Platform Admin Creates Tenant

Steps:

1. Log in as platform super admin.
2. Open `Administration > Tenants`.
3. Create a tenant with the E2E tenant key and name.
4. Verify the tenant appears in the tenant grid.

Expected result:

- Tenant is created.
- Tenant appears as active, not archived.
- No unrelated tenant data changes.

Rollback:

- Archive or delete the E2E tenant from `Administration > Tenants`.

### 2. Platform Admin Creates Tenant Admin

Steps:

1. Open `Administration > Users`.
2. Create the tenant admin user.
3. Open `Administration > Memberships`.
4. Assign the user a tenant-scoped `Tenant Admin` membership for the E2E tenant.

Expected result:

- Tenant admin can authenticate.
- Tenant admin sees tenant-scoped Administration views only.

Rollback:

- Archive the tenant admin user.
- Remove or archive the membership if a membership archive action is available; otherwise delete test rows from the database in a disposable local environment.

### 3. Tenant Admin Creates Organization and Workspace

Steps:

1. Log out.
2. Log in as the tenant admin.
3. Open `Administration > Organizations`.
4. Create the E2E organization.
5. Open `Administration > Workspaces`.
6. Create the E2E workspace under the E2E organization.

Expected result:

- Tenant admin sees only data in the E2E tenant.
- Organization and workspace are created under the correct tenant.
- Workspace appears in the workspace selector after refresh/login.

Rollback:

- Archive the E2E workspace.
- Archive the E2E organization.

### 4. Tenant Admin Creates Organization Admin

Steps:

1. Open `Administration > Users`.
2. Create the organization admin user.
3. Open `Administration > Memberships`.
4. Assign the user an organization-scoped `Organization Admin` membership for the E2E organization.

Expected result:

- Organization admin can authenticate.
- Organization admin sees only organization-authorized Administration views.

Known limitation:

- Current default `Organization Admin` does not have Agent or Workflow lifecycle permissions.
- Workflow lifecycle can be safely enabled for organization admins because workflows have organization/workspace scope.
- Agent lifecycle must not be enabled for organization admins until agents have organization/workspace scope.

Rollback:

- Archive the organization admin user.
- Remove or archive the membership if supported.

### 5. Tenant Admin Tests Agent Lifecycle

Steps:

1. Log in as tenant admin.
2. Open `Administration > Agents`.
3. Create `E2E Agent <date>`.
4. Verify the agent appears as draft/disabled.
5. Enable the agent.
6. Verify enabled status changes.
7. Disable the agent.
8. Verify disabled status changes.
9. Edit the agent name.
10. Archive the agent by typing its name in the confirmation dialog.
11. Verify the agent disappears from the default list.
12. Check `Show archived`.
13. Verify the agent appears.
14. Restore the agent.
15. Verify it returns as draft/disabled.
16. Archive the agent again.
17. Check `Show archived`.
18. Delete the archived agent by typing its name in the confirmation dialog.
19. Verify it no longer appears, even with `Show archived` selected.

Expected result:

- Create, update, enable, disable, archive, show archived, restore, and permanent delete all work.
- Archived agents are hidden unless `Show archived` is selected.
- Permanent delete is available only after archive.

Rollback:

- If the agent was not permanently deleted, archive it.
- If it was permanently deleted, no rollback is required.

### 6. Tenant Admin Tests Workflow Lifecycle

Steps:

1. Open `Administration > Workflows`.
2. Create `E2E Workflow <date>`.
3. Set workflow type, planner type, domain, task type, goal, and model route policy.
4. Verify the workflow appears as draft/disabled.
5. Enable the workflow.
6. Verify status changes to active/enabled.
7. Disable the workflow.
8. Verify status changes to disabled.
9. Edit workflow metadata.
10. Archive the workflow by typing its name in the confirmation dialog.
11. Verify it disappears from the default list.
12. Check `Show archived`.
13. Verify the archived workflow appears as retired/disabled.
14. Restore the workflow.
15. Verify it returns as draft/disabled.
16. Archive the workflow again.
17. Check `Show archived`.
18. Delete the archived workflow by typing its name in the confirmation dialog.
19. Verify it no longer appears, even with `Show archived` selected.

Expected result:

- Create, update, enable, disable, archive, show archived, restore, and permanent delete all work.
- Permanent delete is available only after archive.

Rollback:

- If the workflow was not permanently deleted, archive it.
- If it was permanently deleted, no rollback is required.

### 7. Organization Admin Verification

Steps:

1. Log in as organization admin.
2. Confirm the organization admin does not see tenant-wide data.
3. Try opening `Administration > Agents`.
4. Try opening `Administration > Workflows`.

Expected result today:

- Agent lifecycle should not be available to organization admin because agents are tenant-scoped in the current model.
- Workflow lifecycle may not be visible unless organization-level workflow permissions are added.

Expected result after follow-up ACL/model work:

- Organization admin sees workflows only for authorized organization/workspace scope.
- Organization admin sees agents only after agents are migrated to organization/workspace scope.

### 8. External Annotation Node Administration (SPEC-014)

Steps:

1. Log in as tenant admin (or platform config admin).
2. Open `Administration > Annotation Nodes`.
3. Create one Label Studio node and one CVAT node using secret references (`env:...` or `secret:...`).
4. Verify both nodes appear with tenant-scoped visibility.
5. Use `Test connection` for each node.
6. Confirm the dry run shows DNS, ping, and API authentication steps and a resulting status.
7. Edit one node (display name or base URL) and save.
8. Delete one node only when no active non-synced workflow tasks are linked.

Expected result:

- Nodes are created, updated, listed, and deleted only within authorized tenant scope.
- Secret fields remain secret references and are never exposed as raw credentials.
- Test connection reports detailed step output and updates node status (`ACTIVE`, `OFFLINE`, or `UNAUTHORIZED`).
- Evidence events are emitted for create/update/test actions.

Security check:

- Log in as a different tenant admin and verify they cannot list or test nodes from another tenant.

### 9. Command Center Lightweight Field Correction (Task 42)

Steps:

1. Open the command center route.
2. Locate `Document Field Correction`.
3. Verify extracted values and suggested values are displayed in a compact review grid.
4. Use `Apply correction` on one row.

Expected result:

- Reviewers can perform lightweight extracted-field corrections directly in Kanon without leaving the command center.
- A confirmation notification appears after applying correction.

### 10. Specialist Workbench Handoff (Task 43)

Steps:

1. Open the command center route.
2. Locate `Specialist Workbench Handoff`.
3. Confirm rows show external task identifiers and node type (Label Studio/CVAT).
4. Use `Open workbench` link from at least one row.

Expected result:

- Kanon provides explicit handoff links for rich external annotation editing.
- Handoff rows remain visible from Kanon while Kanon stays the operational source of truth.

### 11. External Sync + Final Review/Approval/Export View (Task 44)

Steps:

1. Open the command center route.
2. Locate `External Sync and Final Review`.
3. Verify each row shows external sync status with review/approval/export progression.
4. Cross-check one row against workflow board/review inbox status for the same case.

Expected result:

- Users can track normalized external annotation sync status and final decision flow in a single command center view.
- Final review, approval, and export visibility remains in Kanon.

## Verification Queries

Use these only in a disposable local database.

Check archived agent:

```sql
SELECT tenant_id, agent_id, name, status, enabled
FROM agent_profile
WHERE name LIKE 'E2E Agent%';
```

Check archived/deleted workflow:

```sql
SELECT tenant_id, organization_id, workspace_id, workflow_id, name, status, enabled
FROM workflow_definition
WHERE name LIKE 'E2E Workflow%';
```

## Honest Status

- Agent archive/unarchive UI is implemented.
- Workflow archive/delete UI is implemented.
- No database migration was required for those two UI fixes.
- Organization-admin Agent lifecycle remains blocked by the tenant-scoped `AgentProfile` model.
- Platform admin least-privilege role remains a design gap. The current `PLATFORM_SUPER_ADMIN` is intentionally broad and should not be treated as the limited platform admin role described here.
