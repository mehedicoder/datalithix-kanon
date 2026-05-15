package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.model.Membership;
import ai.datalithix.kanon.tenant.model.MembershipScope;
import ai.datalithix.kanon.tenant.model.Organization;
import ai.datalithix.kanon.tenant.model.Role;
import ai.datalithix.kanon.tenant.model.Tenant;
import ai.datalithix.kanon.tenant.model.UserAccount;
import ai.datalithix.kanon.tenant.model.Workspace;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.tenant.service.GovernanceAdministrationService;
import ai.datalithix.kanon.ui.component.AdminDialogSupport;
import ai.datalithix.kanon.ui.component.HowItWorksSection;
import ai.datalithix.kanon.ui.i18n.I18n;
import ai.datalithix.kanon.ui.layout.MainLayout;
import ai.datalithix.kanon.ui.security.AdminAccess;
import ai.datalithix.kanon.ui.security.AdminSecuredView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;

@PageTitle("Memberships | Kanon Platform")
@Route(value = "admin/memberships", layout = MainLayout.class)
public class MembershipAdminView extends VerticalLayout implements AdminSecuredView {
    private final GovernanceAdministrationService service;
    private final CurrentUserContextService currentUserContextService;
    private final Grid<Membership> grid = new Grid<>(Membership.class, false);

    public MembershipAdminView(GovernanceAdministrationService service, CurrentUserContextService currentUserContextService) {
        this.service = service;
        this.currentUserContextService = currentUserContextService;
        setSizeFull();
        setPadding(true);
        add(new HowItWorksSection(
                I18n.t("admin.membership.overview.summary"),
                List.of(
                        I18n.t("admin.membership.overview.access"),
                        I18n.t("admin.membership.overview.workspace-roles"),
                        I18n.t("admin.membership.overview.scope")
                )
        ));
        if (AdminAccess.canAssignMembership(context())) {
            add(new Button(I18n.t("admin.membership.assign"), event -> openAssignDialog()));
        }
        configureGrid();
        add(grid);
        refresh();
    }

    private void configureGrid() {
        grid.addColumn(Membership::membershipId).setHeader(I18n.t("grid.membership")).setAutoWidth(true);
        grid.addColumn(Membership::userId).setHeader(I18n.t("grid.user")).setAutoWidth(true);
        grid.addColumn(Membership::scope).setHeader(I18n.t("grid.scope")).setAutoWidth(true);
        grid.addColumn(Membership::tenantId).setHeader(I18n.t("grid.tenant")).setAutoWidth(true);
        grid.addColumn(Membership::organizationId).setHeader(I18n.t("grid.organization")).setAutoWidth(true);
        grid.addColumn(Membership::workspaceId).setHeader(I18n.t("grid.workspace")).setAutoWidth(true);
        grid.addColumn(membership -> String.join(", ", membership.roleKeys())).setHeader(I18n.t("grid.roles")).setAutoWidth(true);
        grid.setSizeFull();
    }

    private void openAssignDialog() {
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.membership.dialog.assign"), "920px");
        ComboBox<UserAccount> user = new ComboBox<>(I18n.t("field.user"));
        user.setRequiredIndicatorVisible(true);
        user.setHelperText(I18n.t("admin.membership.user.helper"));
        user.setItemLabelGenerator(UserAccount::username);
        user.setItems(service.users());
        ComboBox<MembershipScope> scope = new ComboBox<>(I18n.t("field.scope"));
        scope.setRequiredIndicatorVisible(true);
        scope.setHelperText(I18n.t("admin.membership.scope.helper"));
        scope.setItems(MembershipScope.values());
        scope.setValue(MembershipScope.WORKSPACE);
        ComboBox<Tenant> tenant = new ComboBox<>(I18n.t("field.tenant"));
        tenant.setRequiredIndicatorVisible(true);
        tenant.setItemLabelGenerator(Tenant::name);
        tenant.setItems(service.tenants());
        ComboBox<Organization> organization = new ComboBox<>(I18n.t("field.organization"));
        organization.setRequiredIndicatorVisible(true);
        organization.setItemLabelGenerator(Organization::name);
        ComboBox<Workspace> workspace = new ComboBox<>(I18n.t("field.workspace"));
        workspace.setRequiredIndicatorVisible(true);
        workspace.setItemLabelGenerator(Workspace::name);
        tenant.addValueChangeListener(event -> {
            List<Organization> organizations = event.getValue() == null ? List.of() : service.organizations(event.getValue().tenantId());
            organization.setItems(organizations);
            organizations.stream().findFirst().ifPresent(organization::setValue);
        });
        organization.addValueChangeListener(event -> {
            Tenant selectedTenant = tenant.getValue();
            List<Workspace> workspaces = selectedTenant == null || event.getValue() == null
                    ? List.of()
                    : service.workspaces(selectedTenant.tenantId(), event.getValue().organizationId());
            workspace.setItems(workspaces);
            workspaces.stream().findFirst().ifPresent(workspace::setValue);
        });
        service.tenants().stream().findFirst().ifPresent(tenant::setValue);

        CheckboxGroup<String> roles = new CheckboxGroup<>(I18n.t("field.roles"));
        roles.setHelperText(I18n.t("admin.membership.roles.helper"));
        roles.setItems(service.roles().stream().map(Role::roleKey).toList());
        scope.addValueChangeListener(event -> updateScopeFields(event.getValue(), tenant, organization, workspace));
        updateScopeFields(scope.getValue(), tenant, organization, workspace);

        AdminDialogSupport.footer(dialog, I18n.t("action.assign"), () -> {
            if (user.getValue() == null || scope.getValue() == null || roles.getSelectedItems().isEmpty()) {
                Notification.show(I18n.t("admin.membership.validation.user-scope-role"));
                return;
            }
            if (scope.getValue() != MembershipScope.PLATFORM && tenant.getValue() == null) {
                Notification.show(I18n.t("admin.membership.validation.tenant"));
                return;
            }
            if ((scope.getValue() == MembershipScope.ORGANIZATION || scope.getValue() == MembershipScope.WORKSPACE) && organization.getValue() == null) {
                Notification.show(I18n.t("admin.membership.validation.organization"));
                return;
            }
            if (scope.getValue() == MembershipScope.WORKSPACE && workspace.getValue() == null) {
                Notification.show(I18n.t("admin.membership.validation.workspace"));
                return;
            }
            try {
                service.assignMembership(
                        user.getValue().userId(),
                        scope.getValue(),
                        tenant.getValue() == null ? null : tenant.getValue().tenantId(),
                        organization.getValue() == null ? null : organization.getValue().organizationId(),
                        workspace.getValue() == null ? null : workspace.getValue().workspaceId(),
                        roles.getSelectedItems()
                );
                dialog.close();
                refresh();
                Notification.show(I18n.t("admin.membership.assigned"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.sectionTitle(I18n.t("section.authority")),
                AdminDialogSupport.help(I18n.t("admin.membership.assign.help")),
                AdminDialogSupport.form(user, scope, tenant, organization, workspace, roles)
        ));
        dialog.open();
    }

    private static void updateScopeFields(
            MembershipScope scope,
            ComboBox<Tenant> tenant,
            ComboBox<Organization> organization,
            ComboBox<Workspace> workspace
    ) {
        boolean tenantRequired = scope != MembershipScope.PLATFORM;
        boolean organizationRequired = scope == MembershipScope.ORGANIZATION || scope == MembershipScope.WORKSPACE;
        boolean workspaceRequired = scope == MembershipScope.WORKSPACE;
        tenant.setEnabled(tenantRequired);
        organization.setEnabled(organizationRequired);
        workspace.setEnabled(workspaceRequired);
        if (!tenantRequired) {
            tenant.clear();
        }
        if (!organizationRequired) {
            organization.clear();
        }
        if (!workspaceRequired) {
            workspace.clear();
        }
    }

    private void refresh() {
        grid.setItems(service.memberships());
    }

    @Override
    public CurrentUserContext currentUserContext() {
        return currentUserContextService.currentUser();
    }

    private CurrentUserContext context() {
        return currentUserContext();
    }
}
