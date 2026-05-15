package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.model.GovernanceStatus;
import ai.datalithix.kanon.tenant.model.Organization;
import ai.datalithix.kanon.tenant.model.Tenant;
import ai.datalithix.kanon.tenant.model.Workspace;
import ai.datalithix.kanon.tenant.model.WorkspaceType;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.tenant.service.GovernanceAdministrationService;
import ai.datalithix.kanon.ui.component.AdminDialogSupport;
import ai.datalithix.kanon.ui.component.HowItWorksSection;
import ai.datalithix.kanon.ui.i18n.I18n;
import ai.datalithix.kanon.ui.layout.MainLayout;
import ai.datalithix.kanon.ui.security.AdminAccess;
import ai.datalithix.kanon.ui.security.AdminSecuredView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;

@PageTitle("Workspaces | Kanon Platform")
@Route(value = "admin/workspaces", layout = MainLayout.class)
public class WorkspaceAdminView extends VerticalLayout implements AdminSecuredView {
    private final GovernanceAdministrationService service;
    private final CurrentUserContextService currentUserContextService;
    private final ComboBox<Tenant> tenantFilter = new ComboBox<>(I18n.t("field.tenant"));
    private final ComboBox<Organization> organizationFilter = new ComboBox<>(I18n.t("field.organization"));
    private final Grid<Workspace> grid = new Grid<>(Workspace.class, false);
    private final Checkbox showArchived = new Checkbox(I18n.t("action.show-archived"));

    public WorkspaceAdminView(GovernanceAdministrationService service, CurrentUserContextService currentUserContextService) {
        this.service = service;
        this.currentUserContextService = currentUserContextService;
        setSizeFull();
        setPadding(true);
        add(new HowItWorksSection(
                I18n.t("admin.workspace.overview.summary"),
                List.of(
                        I18n.t("admin.workspace.overview.belongs"),
                        I18n.t("admin.workspace.overview.membership"),
                        I18n.t("admin.workspace.overview.domains")
                )
        ));
        tenantFilter.setItemLabelGenerator(Tenant::name);
        tenantFilter.setItems(service.tenants());
        tenantFilter.addValueChangeListener(event -> loadOrganizations());
        organizationFilter.setItemLabelGenerator(Organization::name);
        organizationFilter.addValueChangeListener(event -> refresh());
        showArchived.addValueChangeListener(event -> refresh());
        service.tenants().stream().findFirst().ifPresent(tenantFilter::setValue);
        HorizontalLayout toolbar = new HorizontalLayout(tenantFilter, organizationFilter);
        if (AdminAccess.canCreateWorkspace(context())) {
            toolbar.add(new Button(I18n.t("admin.workspace.create"), event -> openCreateDialog()));
        }
        toolbar.add(showArchived);
        toolbar.setAlignItems(Alignment.END);
        toolbar.setWidthFull();
        add(toolbar);
        configureGrid();
        add(grid);
        loadOrganizations();
    }

    private void configureGrid() {
        grid.addColumn(Workspace::workspaceId).setHeader(I18n.t("grid.workspace")).setAutoWidth(true);
        grid.addColumn(Workspace::organizationId).setHeader(I18n.t("grid.organization")).setAutoWidth(true);
        grid.addColumn(Workspace::name).setHeader(I18n.t("grid.name")).setAutoWidth(true);
        grid.addColumn(Workspace::workspaceType).setHeader(I18n.t("grid.type")).setAutoWidth(true);
        grid.addColumn(Workspace::domainType).setHeader(I18n.t("grid.domain")).setAutoWidth(true);
        grid.addColumn(Workspace::status).setHeader(I18n.t("grid.status")).setAutoWidth(true);
        grid.addComponentColumn(workspace -> {
            CurrentUserContext context = context();
            if (workspace.status() == GovernanceStatus.ARCHIVED) {
                if (!AdminAccess.canRestoreWorkspace(context)) {
                    return new HorizontalLayout(new Span(I18n.t("state.read-only")));
                }
                return new HorizontalLayout(new Button(I18n.t("action.restore"), event -> openRestoreDialog(workspace)));
            }
            HorizontalLayout actions = new HorizontalLayout();
            if (AdminAccess.canUpdateWorkspace(context, workspace)) {
                actions.add(new Button(I18n.t("action.edit"), event -> openUpdateDialog(workspace)));
            }
            if (AdminAccess.canDeleteWorkspace(context)) {
                actions.add(new Button(I18n.t("action.delete"), event -> openDeleteDialog(workspace)));
            }
            if (actions.getComponentCount() == 0) {
                actions.add(new Span(I18n.t("state.read-only")));
            }
            return actions;
        }).setHeader(I18n.t("grid.actions")).setAutoWidth(true);
        grid.setSizeFull();
    }

    @Override
    public CurrentUserContext currentUserContext() {
        return currentUserContextService.currentUser();
    }

    private CurrentUserContext context() {
        return currentUserContext();
    }

    private void openCreateDialog() {
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.workspace.dialog.create"), "820px");
        ComboBox<Tenant> tenant = new ComboBox<>(I18n.t("field.tenant"));
        tenant.setRequiredIndicatorVisible(true);
        tenant.setHelperText(I18n.t("admin.workspace.tenant.helper"));
        tenant.setItemLabelGenerator(Tenant::name);
        tenant.setItems(service.tenants());
        tenant.setValue(tenantFilter.getValue());
        ComboBox<Organization> organization = new ComboBox<>(I18n.t("field.organization"));
        organization.setRequiredIndicatorVisible(true);
        organization.setHelperText(I18n.t("admin.workspace.organization.helper"));
        organization.setItemLabelGenerator(Organization::name);
        tenant.addValueChangeListener(event -> organization.setItems(event.getValue() == null ? List.of() : service.organizations(event.getValue().tenantId())));
        if (tenant.getValue() != null) {
            organization.setItems(service.organizations(tenant.getValue().tenantId()));
            organization.setValue(organizationFilter.getValue());
        }
        TextField key = AdminDialogSupport.requiredText(I18n.t("field.workspace-key"), I18n.t("admin.workspace.key.helper"));
        TextField name = AdminDialogSupport.requiredText(I18n.t("field.name"), I18n.t("admin.workspace.name.helper"));
        ComboBox<WorkspaceType> type = new ComboBox<>(I18n.t("field.type"));
        type.setRequiredIndicatorVisible(true);
        type.setHelperText(I18n.t("admin.workspace.type.helper"));
        type.setItems(WorkspaceType.values());
        type.setValue(WorkspaceType.BUSINESS);
        ComboBox<DomainType> domain = new ComboBox<>(I18n.t("field.domain"));
        domain.setHelperText(I18n.t("admin.workspace.domain.helper"));
        domain.setItems(DomainType.values());
        AdminDialogSupport.footer(dialog, I18n.t("action.create"), () -> {
            if (tenant.getValue() == null || organization.getValue() == null || type.getValue() == null) {
                Notification.show(I18n.t("admin.workspace.validation.scope-required"));
                return;
            }
            if (!AdminDialogSupport.requireFilled(List.of(key, name))) {
                return;
            }
            try {
                service.createWorkspace(tenant.getValue().tenantId(), organization.getValue().organizationId(), key.getValue(), name.getValue(), type.getValue(), domain.getValue());
                dialog.close();
                refresh();
                Notification.show(I18n.t("admin.workspace.created"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.sectionTitle(I18n.t("section.scope")),
                AdminDialogSupport.help(I18n.t("admin.workspace.create.help")),
                AdminDialogSupport.form(tenant, organization, key, name, type, domain)
        ));
        dialog.open();
    }

    private void openUpdateDialog(Workspace workspace) {
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.workspace.dialog.update"), "820px");
        TextField key = AdminDialogSupport.requiredText(I18n.t("field.workspace-key"), I18n.t("admin.workspace.key.helper"));
        key.setValue(workspace.workspaceKey());
        TextField name = AdminDialogSupport.requiredText(I18n.t("field.name"), I18n.t("admin.workspace.name.helper"));
        name.setValue(workspace.name());
        ComboBox<WorkspaceType> type = new ComboBox<>(I18n.t("field.type"));
        type.setRequiredIndicatorVisible(true);
        type.setItems(WorkspaceType.values());
        type.setValue(workspace.workspaceType());
        ComboBox<DomainType> domain = new ComboBox<>(I18n.t("field.domain"));
        domain.setItems(DomainType.values());
        domain.setValue(workspace.domainType());
        AdminDialogSupport.footer(dialog, I18n.t("action.update"), () -> {
            if (type.getValue() == null) {
                Notification.show(I18n.t("validation.required", I18n.t("field.type")));
                return;
            }
            if (!AdminDialogSupport.requireFilled(List.of(key, name))) {
                return;
            }
            AdminDialogSupport.confirmUpdate(
                    I18n.t("admin.workspace.confirm.update.title"),
                    I18n.t("admin.workspace.confirm.update.message", workspace.name()),
                    () -> {
                        try {
                            service.updateWorkspace(
                                    workspace.tenantId(),
                                    workspace.organizationId(),
                                    workspace.workspaceId(),
                                    key.getValue(),
                                    name.getValue(),
                                    type.getValue(),
                                    domain.getValue()
                            );
                            dialog.close();
                            refresh();
                            Notification.show(I18n.t("admin.workspace.updated"));
                        } catch (RuntimeException exception) {
                            Notification.show(exception.getMessage());
                        }
                    }
            );
        });
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.sectionTitle(I18n.t("section.operational-boundary")),
                AdminDialogSupport.help(I18n.t("admin.workspace.update.help")),
                AdminDialogSupport.form(key, name, type, domain)
        ));
        dialog.open();
    }

    private void openDeleteDialog(Workspace workspace) {
        AdminDialogSupport.confirmDeletion(I18n.t("admin.workspace.dialog.delete"), workspace.name(), () -> {
            try {
                service.archiveWorkspace(workspace.tenantId(), workspace.organizationId(), workspace.workspaceId());
                refresh();
                Notification.show(I18n.t("admin.workspace.deleted"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
    }

    private void openRestoreDialog(Workspace workspace) {
        AdminDialogSupport.confirmUpdate(
                I18n.t("admin.workspace.confirm.restore.title"),
                I18n.t("admin.workspace.confirm.restore.message", workspace.name()),
                () -> {
                    try {
                        service.restoreWorkspace(workspace.tenantId(), workspace.organizationId(), workspace.workspaceId());
                        refresh();
                        Notification.show(I18n.t("admin.workspace.restored"));
                    } catch (RuntimeException exception) {
                        Notification.show(exception.getMessage());
                    }
                }
        );
    }

    private void loadOrganizations() {
        Tenant tenant = tenantFilter.getValue();
        List<Organization> organizations = tenant == null ? List.of() : service.organizations(tenant.tenantId());
        organizationFilter.setItems(organizations);
        organizations.stream().findFirst().ifPresentOrElse(organizationFilter::setValue, () -> grid.setItems(List.of()));
        refresh();
    }

    private void refresh() {
        Tenant tenant = tenantFilter.getValue();
        Organization organization = organizationFilter.getValue();
        List<Workspace> workspaces = tenant == null || organization == null ? List.of() : service.workspaces(tenant.tenantId(), organization.organizationId());
        if (!showArchived.getValue()) {
            workspaces = workspaces.stream().filter(workspace -> workspace.status() != GovernanceStatus.ARCHIVED).toList();
        }
        grid.setItems(workspaces);
    }
}
