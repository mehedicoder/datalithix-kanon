package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.model.GovernanceStatus;
import ai.datalithix.kanon.tenant.model.Organization;
import ai.datalithix.kanon.tenant.model.Tenant;
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

@PageTitle("Organizations | Kanon Platform")
@Route(value = "admin/organizations", layout = MainLayout.class)
public class OrganizationAdminView extends VerticalLayout implements AdminSecuredView {
    private final GovernanceAdministrationService service;
    private final CurrentUserContextService currentUserContextService;
    private final ComboBox<Tenant> tenantFilter = new ComboBox<>(I18n.t("field.tenant"));
    private final Grid<Organization> grid = new Grid<>(Organization.class, false);
    private final Checkbox showArchived = new Checkbox(I18n.t("action.show-archived"));

    public OrganizationAdminView(GovernanceAdministrationService service, CurrentUserContextService currentUserContextService) {
        this.service = service;
        this.currentUserContextService = currentUserContextService;
        setSizeFull();
        setPadding(true);
        add(new HowItWorksSection(
                I18n.t("admin.organization.overview.summary"),
                List.of(
                        I18n.t("admin.organization.overview.super-admin"),
                        I18n.t("admin.organization.overview.tenant-admin"),
                        I18n.t("admin.organization.overview.organization-admin")
                )
        ));
        tenantFilter.setItemLabelGenerator(Tenant::name);
        tenantFilter.setItems(service.tenants());
        tenantFilter.addValueChangeListener(event -> refresh());
        showArchived.addValueChangeListener(event -> refresh());
        service.tenants().stream().findFirst().ifPresent(tenantFilter::setValue);
        HorizontalLayout toolbar = new HorizontalLayout(tenantFilter);
        if (AdminAccess.canCreateOrganization(context())) {
            toolbar.add(new Button(I18n.t("admin.organization.create"), event -> openCreateDialog()));
        }
        toolbar.add(showArchived);
        toolbar.setAlignItems(Alignment.END);
        toolbar.setWidthFull();
        add(toolbar);
        configureGrid();
        add(grid);
        refresh();
    }

    private void configureGrid() {
        grid.addColumn(Organization::organizationId).setHeader(I18n.t("grid.organization")).setAutoWidth(true);
        grid.addColumn(Organization::tenantId).setHeader(I18n.t("grid.tenant")).setAutoWidth(true);
        grid.addColumn(Organization::organizationKey).setHeader(I18n.t("grid.key")).setAutoWidth(true);
        grid.addColumn(Organization::name).setHeader(I18n.t("grid.name")).setAutoWidth(true);
        grid.addColumn(Organization::status).setHeader(I18n.t("grid.status")).setAutoWidth(true);
        grid.addComponentColumn(organization -> {
            CurrentUserContext context = context();
            if (organization.status() == GovernanceStatus.ARCHIVED) {
                if (!AdminAccess.canRestoreOrganization(context)) {
                    return new HorizontalLayout(new Span(I18n.t("state.read-only")));
                }
                return new HorizontalLayout(new Button(I18n.t("action.restore"), event -> openRestoreDialog(organization)));
            }
            HorizontalLayout actions = new HorizontalLayout();
            if (AdminAccess.canUpdateOrganization(context, organization)) {
                actions.add(new Button(I18n.t("action.edit"), event -> openUpdateDialog(organization)));
            }
            if (AdminAccess.canDeleteOrganization(context)) {
                actions.add(new Button(I18n.t("action.delete"), event -> openDeleteDialog(organization)));
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
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.organization.dialog.create"), "720px");
        ComboBox<Tenant> tenant = new ComboBox<>(I18n.t("field.tenant"));
        tenant.setRequiredIndicatorVisible(true);
        tenant.setHelperText(I18n.t("admin.organization.tenant.helper"));
        tenant.setItemLabelGenerator(Tenant::name);
        tenant.setItems(service.tenants());
        tenant.setValue(tenantFilter.getValue());
        TextField key = AdminDialogSupport.requiredText(I18n.t("field.organization-key"), I18n.t("admin.organization.key.helper"));
        TextField name = AdminDialogSupport.requiredText(I18n.t("field.name"), I18n.t("admin.organization.name.helper"));
        AdminDialogSupport.footer(dialog, I18n.t("action.create"), () -> {
            if (tenant.getValue() == null) {
                tenant.setInvalid(true);
                tenant.setErrorMessage(I18n.t("validation.required", I18n.t("field.tenant")));
                return;
            }
            tenant.setInvalid(false);
            if (!AdminDialogSupport.requireFilled(List.of(key, name))) {
                return;
            }
            try {
                service.createOrganization(tenant.getValue().tenantId(), key.getValue(), name.getValue());
                dialog.close();
                refresh();
                Notification.show(I18n.t("admin.organization.created"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.sectionTitle(I18n.t("section.scope")),
                AdminDialogSupport.help(I18n.t("admin.organization.create.help")),
                AdminDialogSupport.form(tenant, key, name)
        ));
        dialog.open();
    }

    private void openUpdateDialog(Organization organization) {
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.organization.dialog.update"), "720px");
        TextField key = AdminDialogSupport.requiredText(I18n.t("field.organization-key"), I18n.t("admin.organization.key.helper"));
        key.setValue(organization.organizationKey());
        TextField name = AdminDialogSupport.requiredText(I18n.t("field.name"), I18n.t("admin.organization.name.helper"));
        name.setValue(organization.name());
        AdminDialogSupport.footer(dialog, I18n.t("action.update"), () -> {
            if (!AdminDialogSupport.requireFilled(List.of(key, name))) {
                return;
            }
            AdminDialogSupport.confirmUpdate(
                    I18n.t("admin.organization.confirm.update.title"),
                    I18n.t("admin.organization.confirm.update.message", organization.name()),
                    () -> {
                        try {
                            service.updateOrganization(organization.tenantId(), organization.organizationId(), key.getValue(), name.getValue());
                            dialog.close();
                            refresh();
                            Notification.show(I18n.t("admin.organization.updated"));
                        } catch (RuntimeException exception) {
                            Notification.show(exception.getMessage());
                        }
                    }
            );
        });
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.sectionTitle(I18n.t("section.identity")),
                AdminDialogSupport.help(I18n.t("admin.organization.update.help")),
                AdminDialogSupport.form(key, name)
        ));
        dialog.open();
    }

    private void openDeleteDialog(Organization organization) {
        AdminDialogSupport.confirmDeletion(I18n.t("admin.organization.dialog.delete"), organization.name(), () -> {
            try {
                service.archiveOrganization(organization.tenantId(), organization.organizationId());
                refresh();
                Notification.show(I18n.t("admin.organization.deleted"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
    }

    private void openRestoreDialog(Organization organization) {
        AdminDialogSupport.confirmUpdate(
                I18n.t("admin.organization.confirm.restore.title"),
                I18n.t("admin.organization.confirm.restore.message", organization.name()),
                () -> {
                    try {
                        service.restoreOrganization(organization.tenantId(), organization.organizationId());
                        refresh();
                        Notification.show(I18n.t("admin.organization.restored"));
                    } catch (RuntimeException exception) {
                        Notification.show(exception.getMessage());
                    }
                }
        );
    }

    private void refresh() {
        Tenant tenant = tenantFilter.getValue();
        List<Organization> organizations = tenant == null ? List.of() : service.organizations(tenant.tenantId());
        if (!showArchived.getValue()) {
            organizations = organizations.stream().filter(organization -> organization.status() != GovernanceStatus.ARCHIVED).toList();
        }
        grid.setItems(organizations);
    }
}
