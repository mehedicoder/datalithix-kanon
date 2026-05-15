package ai.datalithix.kanon.ui.security;

import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.ui.view.admin.ActiveLearningAdminView;
import ai.datalithix.kanon.ui.view.admin.AgentAdminView;
import ai.datalithix.kanon.ui.view.admin.ExternalAnnotationNodeAdminView;
import ai.datalithix.kanon.ui.view.admin.DatasetAdminView;
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

public final class AdminRouteAccess {
    private AdminRouteAccess() {
    }

    public static boolean canEnter(Class<?> routeType, CurrentUserContext context) {
        if (TenantAdminView.class.equals(routeType)) {
            return AdminAccess.canViewTenants(context);
        }
        if (OrganizationAdminView.class.equals(routeType)) {
            return AdminAccess.canViewOrganizations(context);
        }
        if (WorkspaceAdminView.class.equals(routeType)) {
            return AdminAccess.canViewWorkspaces(context);
        }
        if (UserAdminView.class.equals(routeType)) {
            return AdminAccess.canViewUsers(context);
        }
        if (MembershipAdminView.class.equals(routeType)) {
            return AdminAccess.canAssignMembership(context);
        }
        if (RoleAdminView.class.equals(routeType)) {
            return AdminAccess.canViewRoles(context);
        }
        if (SecurityAuditView.class.equals(routeType)) {
            return AdminAccess.canReadSecurityAudit(context);
        }
        if (AgentAdminView.class.equals(routeType)) {
            return AdminAccess.canViewAgents(context);
        }
        if (WorkflowAdminView.class.equals(routeType)) {
            return AdminAccess.canViewWorkflows(context);
        }
        if (ModelAdminView.class.equals(routeType)) {
            return AdminAccess.canViewModels(context);
        }
        if (DatasetAdminView.class.equals(routeType)) {
            return AdminAccess.canViewDatasets(context);
        }
        if (TrainingJobAdminView.class.equals(routeType)) {
            return AdminAccess.canViewTrainingJobs(context);
        }
        if (ModelRegistryAdminView.class.equals(routeType)) {
            return AdminAccess.canViewModelRegistry(context);
        }
        if (ActiveLearningAdminView.class.equals(routeType)) {
            return AdminAccess.canViewActiveLearning(context);
        }
        if (ExternalAnnotationNodeAdminView.class.equals(routeType)) {
            return AdminAccess.canViewAnnotationNodes(context);
        }
        return false;
    }
}
