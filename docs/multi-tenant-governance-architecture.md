# Multi-Tenant Governance Architecture

Kanon uses a four-level governance hierarchy:

```text
Platform
  -> Tenant
      -> Organization
          -> Workspace
              -> Workflows / Agents / Human Members / Tasks / Cases / Evidence
```

The workspace is the primary operational boundary. Tenant and organization provide ownership, administration, policy, and access-control boundaries.

Kanon administration must be available from the UI, but UI visibility is not authorization. Backend APIs and services must enforce the same platform, tenant, organization, and workspace permissions.

## Bootstrap Defaults

Every deployment starts with:

| Concept | Default |
| --- | --- |
| Tenant | `default` / Default Tenant |
| Organization | `default-org` / Default Organization |
| Workspace | `administration` / Administration |
| User | `superadmin` / Platform Super Admin |
| Platform membership | `PLATFORM_SUPER_ADMIN` |
| Workspace membership | `WORKSPACE_MANAGER`, `AUDITOR` in Administration |

The `Administration` workspace is the initial workspace used for bootstrap and platform administration.

The bootstrap super admin is platform-scoped and can manage all tenants. The super admin also receives an Administration workspace membership so the user appears naturally inside the workspace model.

## Core Model

### Tenant

A tenant is the top customer/account boundary. Tenant admins can manage only resources inside their tenant. Platform super admins can create and manage tenants.

### Organization

An organization belongs to one tenant and groups business units or legal entities. Organization admins can create and manage workspaces, organization users, and workspace memberships inside their allowed organization scope.

Super admins can create organization admin users from the UI. Organization admins are not platform-scoped and cannot manage other tenants unless they receive additional memberships.

### Workspace

A workspace belongs to one organization and one tenant. It owns operational work:

- workflow definitions and instances
- agent definitions and runtime assignments
- cases and tasks
- human annotator, reviewer, and approver assignments
- workspace policies
- workspace model routing preferences
- workspace-scoped audit and evidence visibility

Domain workspaces can represent Logistics, Finance, Medical, HR, Agriculture, Legal, or future domain-specific areas.

Workspace users are created and assigned through explicit workspace memberships. A workspace user is not globally an annotator, reviewer, or approver; those capabilities come from the roles attached to the user's workspace membership.

### User

A user is identity only. Authority is not stored as global flags on the user. Authority comes from explicit memberships and scoped roles.

### Membership

Membership connects a user to one scope:

- platform
- tenant
- organization
- workspace

A user may have multiple memberships. Human annotators, reviewers, and approvers must be assigned through workspace memberships, not global user flags.

### Role And Permission

Roles are scope-aware and map to permissions. Service and repository code must enforce permissions; Vaadin menu hiding is only a usability layer.

Recommended roles:

- `PLATFORM_SUPER_ADMIN`
- `PLATFORM_CONFIG_ADMIN`
- `TENANT_ADMIN`
- `TENANT_CONFIG_ADMIN`
- `ORGANIZATION_ADMIN`
- `WORKSPACE_MANAGER`
- `ANNOTATOR`
- `REVIEWER`
- `APPROVER`
- `AUDITOR`
- `VIEWER`
- `MODEL_OPERATOR`
- `INTEGRATION_SERVICE_ACCOUNT`

`ORGANIZATION_ADMIN` is the product role name for organization-scoped administration.

## Entity Relationships

```text
tenant
  1 -> many organization

organization
  many -> 1 tenant
  1 -> many workspace

workspace
  many -> 1 tenant
  many -> 1 organization
  1 -> many workflow_definition
  1 -> many workflow_instance
  1 -> many agent_definition
  1 -> many case_record
  1 -> many task_record
  1 -> many annotation_task
  1 -> many workspace_policy_binding
  1 -> many workspace_model_route_preference

user_account
  1 -> many membership

membership
  many -> 1 user_account
  optional -> tenant
  optional -> organization
  optional -> workspace
  many -> many role through membership_role

role
  many -> many permission, represented by persisted permission keys for MVP
```

## UI Administration Actions

### Super Admin UI

The platform super admin can create and manage:

- tenants
- organizations under any tenant
- workspaces under any organization
- organization admin users
- tenant admin users
- memberships and scoped roles
- global/platform configuration

Super admin screens should include tenant, organization, workspace, user, membership, role, and platform configuration administration. Super admin actions must call backend APIs that verify platform-scoped permissions.

All multi-tenant governance screens must live under the `Administration` parent menu in Kanon UI. The `Control Panel` menu is reserved for operational command-center work such as cases, workflows, reviews, evidence, models, and connector status.

Super admin has unrestricted platform-wide administration access and can list, create, update, and assign across all tenants, organizations, and workspaces.

### Tenant Admin UI

A tenant admin can access Administration views only inside their assigned tenant scope.

Tenant admin visibility rules:

- tenant admin can view only data belonging to their own tenant
- tenant-level master data is read-only by default
- tenant-level edits require an explicit edit permission
- tenant admin can create and manage organizations, workspaces, tenant users, and memberships only when scoped tenant permissions allow it
- tenant admin cannot manage platform/global configuration
- tenant admin cannot see or mutate another tenant's records

Administration UI access for tenant admin does not imply global access. Tenant-scoped filters must apply to every list, form lookup, action, and API result.

### Organization Admin UI

An organization admin can create and manage only resources allowed by their organization membership:

- organizations inside their assigned tenant when tenant policy grants that permission
- workspaces inside organizations they administer
- workspace users
- workspace memberships and workspace-scoped roles
- organization-level configuration where allowed

Organization admins cannot manage platform/global configuration and cannot access other tenants without additional memberships.

Organization-admin screens also live under the `Administration` parent menu, with menu items hidden or disabled according to backend-authorized permissions.

Organization admin visibility rules:

- organization admin can view only data belonging to their own organization
- organization-level master data is read-only by default
- organization-level edits require an explicit edit permission
- organization admin can create and manage workspaces, workspace users, and workspace memberships only inside their authorized organization scope
- organization admin cannot see sibling organizations unless separately granted membership
- organization admin cannot manage tenant-level or platform/global configuration unless separately granted higher scoped membership

Administration UI access for organization admin does not imply tenant-wide access. Organization-scoped filters must apply to every list, form lookup, action, and API result.

### Workspace User UI

Workspace users can see and work only inside workspaces where they have explicit membership. Their actions are determined by workspace roles:

- annotator
- reviewer
- approver
- auditor
- viewer
- workspace manager
- model operator
- integration service account

## Role And Permission Matrix

| Role | Scope | Core permissions |
| --- | --- | --- |
| `PLATFORM_SUPER_ADMIN` | Platform | Create tenants, create organizations, create workspaces, create tenant/admin users, assign roles, manage platform configuration, read platform audit. |
| `PLATFORM_CONFIG_ADMIN` | Platform | Manage platform configuration, platform model defaults, connector templates, and global health. |
| `TENANT_ADMIN` | Tenant | Manage tenant organizations, workspaces, tenant users, tenant memberships, tenant roles, tenant configuration, and tenant audit. |
| `TENANT_CONFIG_ADMIN` | Tenant | Manage tenant model, policy, connector, and configuration settings. |
| `ORGANIZATION_ADMIN` | Organization | Manage allowed organizations, organization workspaces, workspace users, workspace memberships, and organization configuration. |
| `WORKSPACE_MANAGER` | Workspace | Manage workspace workflows, agents, cases, tasks, human assignments, policy bindings, and model routing preferences. |
| `ANNOTATOR` | Workspace | Perform assigned annotation tasks and submit annotation output. |
| `REVIEWER` | Workspace | Review assigned work, correct results, complete review, reject, or escalate where allowed. |
| `APPROVER` | Workspace | Approve, reject, and mark approved work as export-ready. |
| `AUDITOR` | Workspace, organization, or tenant | Read authorized evidence, audit trails, decisions, and lineage. |
| `VIEWER` | Workspace | Read allowed workspace summaries and assigned case/task metadata. |
| `MODEL_OPERATOR` | Workspace or tenant | Manage model route preferences and run model health/test actions where allowed. |
| `INTEGRATION_SERVICE_ACCOUNT` | Workspace | Ingest source data, create tasks, execute connector flows, and append evidence. |

## Workspace Scoping Rules

All workspace-owned records should carry:

```text
tenant_id
organization_id
workspace_id
```

This includes workflow definitions, workflow instances, agents, cases, tasks, annotation tasks, model route preferences, policy bindings, and evidence events where applicable.

Workflow definitions, agent definitions, and human approval/review operations are workspace-scoped. Assignment should eventually reference workspace membership ids so audit can show which scoped authority a user acted under.

### Workflow, Agent, And Human Task Impact

Workflow definitions must belong to a workspace. The same workflow key may exist in Logistics and Finance with different policies, model routing, and approver roles.

Agent definitions must belong to a workspace unless they are platform or tenant templates. Workspace activation controls which configured agents can operate on cases and tasks.

Human annotators, reviewers, and approvers must be assigned through workspace memberships. Task assignment should prefer `assigned_membership_id` over a raw user id so audit can show the exact authority used for that task.

Cases and tasks must belong to one workspace. Users without membership in that workspace cannot list, open, mutate, approve, or export those records unless a higher scoped role explicitly grants access.

## Persistence Rules

All governance and workspace-owned tables must include audit columns:

```text
created_at
created_by
updated_at
updated_by
audit_version
```

Repository code must write these columns on insert and update. Migrations alone are not enough.

Indexes should be tenant-first and match operational access patterns, especially:

```text
tenant_id
organization_id
workspace_id
status
case_id
workflow_id
assigned_membership_id
updated_at
```

## Security Rule

Kanon is default-deny. A user can only see and manage workspace resources where membership and role permissions allow access, unless elevated tenant or platform authority applies.

Platform super admin can manage platform-wide configuration and create tenants. Tenant admins can manage tenant-level configuration and resources inside their assigned tenant, but cannot manage platform/global configuration. Organization admins manage organization and workspace administration only within their granted organization scope.

Administration UI access never widens the user's scope. Backend APIs must enforce platform, tenant, organization, and workspace predicates on every query and mutation. UI menus may hide unavailable actions, but security lives in API, service, and repository checks.

## Recommended Spring Boot / JPA Approach

The current implementation uses JDBC repositories behind repository ports. If JPA is introduced later, keep the same boundaries:

- keep domain records in `kanon-tenant`
- keep authorization services in `kanon-policy`
- keep persistence adapters in `kanon-bootstrap` or a dedicated persistence adapter module
- keep UI administration views in `kanon-ui`
- keep API administration endpoints in `kanon-api`

Recommended JPA entities:

- `TenantEntity`
- `OrganizationEntity`
- `WorkspaceEntity`
- `UserAccountEntity`
- `MembershipEntity`
- `RoleEntity`
- `MembershipRoleEntity`

Every JPA entity for governance or workspace-owned state must include:

```text
created_at
created_by
updated_at
updated_by
audit_version / @Version
```

Service methods should resolve the current user context, verify permission for the requested scope, perform the mutation, persist audit fields, and append security/evidence events for sensitive administrative changes.
