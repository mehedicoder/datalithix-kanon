package ai.datalithix.kanon.ui.security;

import ai.datalithix.kanon.agentruntime.model.AgentProfile;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.model.Organization;
import ai.datalithix.kanon.tenant.model.Tenant;
import ai.datalithix.kanon.tenant.model.UserAccount;
import ai.datalithix.kanon.tenant.model.Workspace;
import ai.datalithix.kanon.workflow.model.WorkflowDefinition;
import java.util.Arrays;

public final class AdminAccess {
    private AdminAccess() {
    }

    public static boolean has(CurrentUserContext context, String permission) {
        return context.permissions().contains(permission);
    }

    public static boolean hasAny(CurrentUserContext context, String... permissions) {
        return Arrays.stream(permissions).anyMatch(permission -> has(context, permission));
    }

    public static boolean canViewTenants(CurrentUserContext context) {
        return hasAny(context, "platform.tenant.create", "platform.tenant.update", "tenant.read", "tenant.update");
    }

    public static boolean canCreateTenant(CurrentUserContext context) {
        return has(context, "platform.tenant.create");
    }

    public static boolean canUpdateTenant(CurrentUserContext context, Tenant tenant) {
        return has(context, "platform.tenant.update")
                || has(context, "tenant.update") && context.activeTenantId().equals(tenant.tenantId());
    }

    public static boolean canDeleteTenant(CurrentUserContext context) {
        return has(context, "platform.tenant.delete");
    }

    public static boolean canRestoreTenant(CurrentUserContext context) {
        return has(context, "platform.tenant.restore");
    }

    public static boolean canViewOrganizations(CurrentUserContext context) {
        return hasAny(context, "platform.organization.create", "platform.organization.update", "tenant.organization.create", "tenant.organization.update", "organization.read", "organization.update");
    }

    public static boolean canCreateOrganization(CurrentUserContext context) {
        return hasAny(context, "platform.organization.create", "tenant.organization.create");
    }

    public static boolean canUpdateOrganization(CurrentUserContext context, Organization organization) {
        return has(context, "platform.organization.update")
                || has(context, "tenant.organization.update") && context.activeTenantId().equals(organization.tenantId())
                || has(context, "organization.update")
                && context.activeTenantId().equals(organization.tenantId())
                && context.activeOrganizationId().equals(organization.organizationId());
    }

    public static boolean canDeleteOrganization(CurrentUserContext context) {
        return has(context, "platform.organization.delete");
    }

    public static boolean canRestoreOrganization(CurrentUserContext context) {
        return has(context, "platform.organization.restore");
    }

    public static boolean canViewWorkspaces(CurrentUserContext context) {
        return hasAny(context, "platform.workspace.create", "platform.workspace.update", "tenant.workspace.create", "tenant.workspace.update", "organization.workspace.create", "organization.workspace.update", "workspace.read", "workspace.update");
    }

    public static boolean canCreateWorkspace(CurrentUserContext context) {
        return hasAny(context, "platform.workspace.create", "tenant.workspace.create", "organization.workspace.create");
    }

    public static boolean canUpdateWorkspace(CurrentUserContext context, Workspace workspace) {
        return has(context, "platform.workspace.update")
                || has(context, "tenant.workspace.update") && context.activeTenantId().equals(workspace.tenantId())
                || has(context, "organization.workspace.update")
                && context.activeTenantId().equals(workspace.tenantId())
                && context.activeOrganizationId().equals(workspace.organizationId())
                || has(context, "workspace.update")
                && context.activeTenantId().equals(workspace.tenantId())
                && context.activeOrganizationId().equals(workspace.organizationId())
                && context.activeWorkspaceId().equals(workspace.workspaceId());
    }

    public static boolean canDeleteWorkspace(CurrentUserContext context) {
        return has(context, "platform.workspace.delete");
    }

    public static boolean canRestoreWorkspace(CurrentUserContext context) {
        return has(context, "platform.workspace.restore");
    }

    public static boolean canViewUsers(CurrentUserContext context) {
        return hasAny(context, "platform.user.create", "platform.user.update", "tenant.user.create", "tenant.user.update", "organization.user.update", "organization.membership.assign", "workspace.membership.assign");
    }

    public static boolean canCreateUser(CurrentUserContext context) {
        return hasAny(context, "platform.user.create", "tenant.user.create", "organization.membership.assign");
    }

    public static boolean canUpdateUser(CurrentUserContext context, UserAccount user) {
        return has(context, "platform.user.update")
                || hasAny(context, "tenant.user.update", "organization.user.update") && !user.systemUser();
    }

    public static boolean canDeleteUser(CurrentUserContext context, UserAccount user) {
        return has(context, "platform.user.delete") && !user.systemUser();
    }

    public static boolean canRestoreUser(CurrentUserContext context) {
        return has(context, "platform.user.restore");
    }

    public static boolean canAssignMembership(CurrentUserContext context) {
        return hasAny(context, "platform.membership.assign", "tenant.membership.assign", "organization.membership.assign", "workspace.membership.assign");
    }

    public static boolean canViewRoles(CurrentUserContext context) {
        return hasAny(context, "platform.role.assign", "tenant.role.assign", "organization.membership.assign", "workspace.membership.assign");
    }

    public static boolean canReadSecurityAudit(CurrentUserContext context) {
        return hasAny(context, "platform.audit.read", "tenant.audit.read", "workspace.audit.read");
    }

    public static boolean canViewAgents(CurrentUserContext context) {
        return hasAny(context,
                "platform.agent.read", "platform.agent.create", "platform.agent.update", "platform.agent.delete",
                "tenant.agent.read", "tenant.agent.create", "tenant.agent.update", "tenant.agent.delete",
                "workspace.agent.read", "workspace.agent.create", "workspace.agent.update", "workspace.agent.delete");
    }

    public static boolean canCreateAgent(CurrentUserContext context) {
        return hasAny(context, "platform.agent.create", "tenant.agent.create", "workspace.agent.create");
    }

    public static boolean canUpdateAgent(CurrentUserContext context, AgentProfile agent) {
        return has(context, "platform.agent.update")
                || has(context, "tenant.agent.update") && context.activeTenantId().equals(agent.tenantId())
                || has(context, "workspace.agent.update") && context.activeTenantId().equals(agent.tenantId());
    }

    public static boolean canDeleteAgent(CurrentUserContext context, AgentProfile agent) {
        return has(context, "platform.agent.delete") || has(context, "tenant.agent.delete") && context.activeTenantId().equals(agent.tenantId());
    }

    public static boolean canRestoreAgent(CurrentUserContext context, AgentProfile agent) {
        return has(context, "platform.agent.restore")
                || has(context, "tenant.agent.restore") && context.activeTenantId().equals(agent.tenantId())
                || has(context, "workspace.agent.restore") && context.activeTenantId().equals(agent.tenantId());
    }

    public static boolean canViewWorkflows(CurrentUserContext context) {
        return hasAny(context,
                "platform.workflow.read", "platform.workflow.create", "platform.workflow.update", "platform.workflow.delete",
                "tenant.workflow.read", "tenant.workflow.create", "tenant.workflow.update", "tenant.workflow.delete",
                "workspace.workflow.read", "workspace.workflow.create", "workspace.workflow.update", "workspace.workflow.delete");
    }

    public static boolean canCreateWorkflow(CurrentUserContext context) {
        return hasAny(context, "platform.workflow.create", "tenant.workflow.create", "workspace.workflow.create");
    }

    public static boolean canUpdateWorkflow(CurrentUserContext context, WorkflowDefinition workflow) {
        return has(context, "platform.workflow.update")
                || has(context, "tenant.workflow.update") && context.activeTenantId().equals(workflow.tenantId())
                || has(context, "workspace.workflow.update")
                && context.activeTenantId().equals(workflow.tenantId())
                && context.activeOrganizationId().equals(workflow.organizationId())
                && context.activeWorkspaceId().equals(workflow.workspaceId());
    }

    public static boolean canDeleteWorkflow(CurrentUserContext context, WorkflowDefinition workflow) {
        return has(context, "platform.workflow.delete")
                || has(context, "tenant.workflow.delete") && context.activeTenantId().equals(workflow.tenantId())
                || has(context, "workspace.workflow.delete")
                && context.activeTenantId().equals(workflow.tenantId())
                && context.activeOrganizationId().equals(workflow.organizationId())
                && context.activeWorkspaceId().equals(workflow.workspaceId());
    }

    public static boolean canViewModels(CurrentUserContext context) {
        return hasAny(context,
                "platform.config.manage",  // PLATFORM_SUPER_ADMIN has this
                "tenant.model.manage",     // TENANT_CONFIG_ADMIN has this
                "workspace.model.test", "workspace.model-route.read", "workspace.model-route.manage");  // MODEL_OPERATOR has these
    }

    public static boolean canConfigureModels(CurrentUserContext context) {
        return hasAny(context,
                "platform.config.manage",  // PLATFORM_SUPER_ADMIN has this
                "tenant.model.manage",     // TENANT_CONFIG_ADMIN has this
                "workspace.model-route.manage");  // MODEL_OPERATOR has this
    }

    public static boolean canTestModels(CurrentUserContext context) {
        return hasAny(context,
                "platform.config.manage",  // PLATFORM_SUPER_ADMIN has this
                "workspace.model.test", "workspace.model-route.manage");  // MODEL_OPERATOR has these
    }

    public static boolean canViewModelSecrets(CurrentUserContext context) {
        return hasAny(context,
                "platform.config.manage",  // PLATFORM_SUPER_ADMIN has this
                "tenant.model.manage");    // TENANT_CONFIG_ADMIN has this
    }

    public static boolean canViewAnnotationNodes(CurrentUserContext context) {
        return hasAny(context, "platform.config.manage", "platform.config.read", "tenant.config.manage", "tenant.config.read");
    }

    public static boolean canManageAnnotationNodes(CurrentUserContext context) {
        return hasAny(context, "platform.config.manage", "tenant.config.manage");
    }

    public static boolean canTestAnnotationNodes(CurrentUserContext context) {
        return hasAny(context, "platform.config.manage", "tenant.config.manage", "workspace.model.test");
    }

    public static boolean canViewDatasets(CurrentUserContext context) {
        return hasAny(context,
                "platform.config.manage",
                "tenant.config.manage",
                "workspace.dataset.read", "workspace.dataset.create");
    }

    public static boolean canViewTrainingJobs(CurrentUserContext context) {
        return hasAny(context,
                "platform.config.manage",
                "tenant.config.manage",
                "workspace.training.read", "workspace.training.create");
    }

    public static boolean canViewModelRegistry(CurrentUserContext context) {
        return hasAny(context,
                "platform.config.manage",
                "tenant.config.manage",
                "workspace.model.test", "workspace.model-route.manage");
    }

    public static boolean canViewActiveLearning(CurrentUserContext context) {
        return hasAny(context,
                "platform.config.manage",
                "tenant.config.manage",
                "workspace.training.read", "workspace.training.create");
    }
}
