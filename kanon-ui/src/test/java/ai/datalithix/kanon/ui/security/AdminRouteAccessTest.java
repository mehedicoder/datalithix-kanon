package ai.datalithix.kanon.ui.security;

import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.ui.view.admin.ActiveLearningAdminView;
import ai.datalithix.kanon.ui.view.admin.AgentAdminView;
import ai.datalithix.kanon.ui.view.admin.DatasetAdminView;
import ai.datalithix.kanon.ui.view.admin.ExternalAnnotationNodeAdminView;
import ai.datalithix.kanon.ui.view.admin.MembershipAdminView;
import ai.datalithix.kanon.ui.view.admin.ModelAdminView;
import ai.datalithix.kanon.ui.view.admin.ModelRegistryAdminView;
import ai.datalithix.kanon.ui.view.admin.OrganizationAdminView;
import ai.datalithix.kanon.ui.view.admin.RoleAdminView;
import ai.datalithix.kanon.ui.view.admin.SecurityAuditView;
import ai.datalithix.kanon.ui.view.admin.TenantAdminView;
import ai.datalithix.kanon.ui.view.admin.TrainingJobAdminView;
import ai.datalithix.kanon.ui.view.admin.UserAdminView;
import ai.datalithix.kanon.ui.view.admin.WorkflowAdminView;
import ai.datalithix.kanon.ui.view.admin.WorkspaceAdminView;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminRouteAccessTest {
    @Test
    void deniesAllRoutesWithoutPermissions() {
        CurrentUserContext context = context(Set.of());
        assertFalse(AdminRouteAccess.canEnter(TenantAdminView.class, context));
        assertFalse(AdminRouteAccess.canEnter(OrganizationAdminView.class, context));
        assertFalse(AdminRouteAccess.canEnter(WorkspaceAdminView.class, context));
        assertFalse(AdminRouteAccess.canEnter(UserAdminView.class, context));
        assertFalse(AdminRouteAccess.canEnter(MembershipAdminView.class, context));
        assertFalse(AdminRouteAccess.canEnter(RoleAdminView.class, context));
        assertFalse(AdminRouteAccess.canEnter(SecurityAuditView.class, context));
        assertFalse(AdminRouteAccess.canEnter(AgentAdminView.class, context));
        assertFalse(AdminRouteAccess.canEnter(WorkflowAdminView.class, context));
        assertFalse(AdminRouteAccess.canEnter(ModelAdminView.class, context));
        assertFalse(AdminRouteAccess.canEnter(DatasetAdminView.class, context));
        assertFalse(AdminRouteAccess.canEnter(TrainingJobAdminView.class, context));
        assertFalse(AdminRouteAccess.canEnter(ModelRegistryAdminView.class, context));
        assertFalse(AdminRouteAccess.canEnter(ActiveLearningAdminView.class, context));
        assertFalse(AdminRouteAccess.canEnter(ExternalAnnotationNodeAdminView.class, context));
    }

    @Test
    void allowsOnlyRoutesMatchingGrantedPermissions() {
        CurrentUserContext tenantAdmin = context(Set.of("tenant.update", "tenant.membership.assign", "tenant.audit.read", "platform.config.manage"));
        assertTrue(AdminRouteAccess.canEnter(TenantAdminView.class, tenantAdmin));
        assertTrue(AdminRouteAccess.canEnter(MembershipAdminView.class, tenantAdmin));
        assertTrue(AdminRouteAccess.canEnter(SecurityAuditView.class, tenantAdmin));
        assertTrue(AdminRouteAccess.canEnter(DatasetAdminView.class, tenantAdmin));
        assertTrue(AdminRouteAccess.canEnter(TrainingJobAdminView.class, tenantAdmin));
        assertTrue(AdminRouteAccess.canEnter(ModelRegistryAdminView.class, tenantAdmin));
        assertTrue(AdminRouteAccess.canEnter(ActiveLearningAdminView.class, tenantAdmin));
        assertTrue(AdminRouteAccess.canEnter(ExternalAnnotationNodeAdminView.class, tenantAdmin));
    }

    @Test
    void deniesUnmatchedRoutes() {
        CurrentUserContext onlyTenant = context(Set.of("tenant.update"));
        assertTrue(AdminRouteAccess.canEnter(TenantAdminView.class, onlyTenant));
        assertFalse(AdminRouteAccess.canEnter(AgentAdminView.class, onlyTenant));
        assertFalse(AdminRouteAccess.canEnter(WorkflowAdminView.class, onlyTenant));
    }

    @Test
    void allowsSuperAdminToEnterAgentAdministration() {
        CurrentUserContext superAdmin = context(Set.of("platform.agent.read", "platform.agent.create", "platform.agent.update", "platform.agent.delete"));
        assertTrue(AdminRouteAccess.canEnter(AgentAdminView.class, superAdmin));
    }

    @Test
    void treatsCreatePermissionAsSufficientToEnterAgentAdministration() {
        CurrentUserContext workspaceManager = context(Set.of("workspace.agent.create"));
        assertTrue(AdminRouteAccess.canEnter(AgentAdminView.class, workspaceManager));
    }

    @Test
    void allowsSuperAdminToEnterWorkflowAdministration() {
        CurrentUserContext superAdmin = context(Set.of("platform.workflow.read", "platform.workflow.create", "platform.workflow.update", "platform.workflow.delete"));
        assertTrue(AdminRouteAccess.canEnter(WorkflowAdminView.class, superAdmin));
    }

    @Test
    void treatsCreatePermissionAsSufficientToEnterWorkflowAdministration() {
        CurrentUserContext workspaceManager = context(Set.of("workspace.workflow.create"));
        assertTrue(AdminRouteAccess.canEnter(WorkflowAdminView.class, workspaceManager));
    }

    @Test
    void allowsAnnotationNodesWithPlatformConfigManage() {
        CurrentUserContext admin = context(Set.of("platform.config.manage"));
        assertTrue(AdminRouteAccess.canEnter(ExternalAnnotationNodeAdminView.class, admin));
    }

    @Test
    void allowsAnnotationNodesWithTenantConfigRead() {
        CurrentUserContext admin = context(Set.of("tenant.config.read"));
        assertTrue(AdminRouteAccess.canEnter(ExternalAnnotationNodeAdminView.class, admin));
    }

    @Test
    void deniesUnknownRouteType() {
        CurrentUserContext context = context(Set.of("platform.config.manage"));
        assertFalse(AdminRouteAccess.canEnter(String.class, context));
    }

    private static CurrentUserContext context(Set<String> permissions) {
        return new CurrentUserContext(
                "user-1",
                "operator",
                "tenant-1",
                "org-1",
                "workspace-1",
                Set.of(),
                permissions,
                List.of()
        );
    }
}
