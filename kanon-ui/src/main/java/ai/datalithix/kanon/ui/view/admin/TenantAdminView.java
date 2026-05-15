package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.model.GovernanceStatus;
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

@PageTitle("Tenants | Kanon Platform")
@Route(value = "admin/tenants", layout = MainLayout.class)
public class TenantAdminView extends VerticalLayout implements AdminSecuredView {
    private final GovernanceAdministrationService service;
    private final CurrentUserContextService currentUserContextService;
    private final Grid<Tenant> grid = new Grid<>(Tenant.class, false);
    private final Checkbox showArchived = new Checkbox(I18n.t("action.show-archived"));

    public TenantAdminView(GovernanceAdministrationService service, CurrentUserContextService currentUserContextService) {
        this.service = service;
        this.currentUserContextService = currentUserContextService;
        setSizeFull();
        setPadding(true);
        add(new HowItWorksSection(
                I18n.t("admin.tenant.overview.summary"),
                List.of(
                        I18n.t("admin.tenant.overview.super-admin"),
                        I18n.t("admin.tenant.overview.tenant-admin"),
                        I18n.t("admin.tenant.overview.read-only")
                )
        ));
        showArchived.addValueChangeListener(event -> refresh());
        HorizontalLayout toolbar = createToolbar();
        toolbar.setAlignItems(Alignment.END);
        toolbar.setWidthFull();
        add(toolbar);
        configureGrid();
        add(grid);
        refresh();
    }

    HorizontalLayout createToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        if (AdminAccess.canCreateTenant(context())) {
            toolbar.add(new Button(I18n.t("admin.tenant.create"), event -> openCreateDialog()));
        }
        toolbar.add(showArchived);
        return toolbar;
    }

    private void configureGrid() {
        grid.addColumn(Tenant::tenantId).setHeader(I18n.t("grid.tenant")).setAutoWidth(true);
        grid.addColumn(Tenant::tenantKey).setHeader(I18n.t("grid.key")).setAutoWidth(true);
        grid.addColumn(Tenant::name).setHeader(I18n.t("grid.name")).setAutoWidth(true);
        grid.addColumn(Tenant::status).setHeader(I18n.t("grid.status")).setAutoWidth(true);
        grid.addColumn(Tenant::dataResidency).setHeader(I18n.t("grid.residency")).setAutoWidth(true);
        grid.addColumn(tenant -> tenant.audit().updatedAt()).setHeader(I18n.t("grid.updated")).setAutoWidth(true);
        grid.addComponentColumn(tenant -> {
            CurrentUserContext context = context();
            if (tenant.status() == GovernanceStatus.ARCHIVED) {
                if (!AdminAccess.canRestoreTenant(context)) {
                    return new HorizontalLayout(new Span(I18n.t("state.read-only")));
                }
                return new com.vaadin.flow.component.orderedlayout.HorizontalLayout(
                        new Button(I18n.t("action.restore"), event -> openRestoreDialog(tenant))
                );
            }
            HorizontalLayout actions = new HorizontalLayout();
            if (AdminAccess.canUpdateTenant(context, tenant)) {
                actions.add(new Button(I18n.t("action.edit"), event -> openUpdateDialog(tenant)));
            }
            if (AdminAccess.canDeleteTenant(context)) {
                actions.add(new Button(I18n.t("action.delete"), event -> openDeleteDialog(tenant)));
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
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.tenant.dialog.create"), "720px");
        TextField key = AdminDialogSupport.requiredText(I18n.t("field.tenant-key"), I18n.t("admin.tenant.key.helper"));
        TextField name = AdminDialogSupport.requiredText(I18n.t("field.name"), I18n.t("admin.tenant.name.helper"));
        TextField residency = new TextField(I18n.t("field.data-residency"));
        residency.setHelperText(I18n.t("admin.tenant.residency.helper"));
        residency.setValue("DEFAULT");
        TextField locale = new TextField(I18n.t("field.default-locale"));
        locale.setHelperText(I18n.t("admin.tenant.locale.helper"));
        locale.setValue("en");
        AdminDialogSupport.footer(dialog, I18n.t("action.create"), () -> {
            if (!AdminDialogSupport.requireFilled(List.of(key, name))) {
                return;
            }
            try {
                service.createTenant(key.getValue(), name.getValue(), residency.getValue(), locale.getValue());
                dialog.close();
                refresh();
                Notification.show(I18n.t("admin.tenant.created"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.sectionTitle(I18n.t("section.identity")),
                AdminDialogSupport.help(I18n.t("admin.tenant.create.help")),
                AdminDialogSupport.form(key, name, residency, locale)
        ));
        dialog.open();
    }

    private void openUpdateDialog(Tenant tenant) {
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.tenant.dialog.update"), "720px");
        TextField key = AdminDialogSupport.requiredText(I18n.t("field.tenant-key"), I18n.t("admin.tenant.key.helper"));
        key.setValue(tenant.tenantKey());
        TextField name = AdminDialogSupport.requiredText(I18n.t("field.name"), I18n.t("admin.tenant.name.helper"));
        name.setValue(tenant.name());
        TextField residency = new TextField(I18n.t("field.data-residency"));
        residency.setValue(tenant.dataResidency() == null ? "" : tenant.dataResidency());
        TextField locale = new TextField(I18n.t("field.default-locale"));
        locale.setValue(tenant.defaultLocale() == null ? "" : tenant.defaultLocale());
        AdminDialogSupport.footer(dialog, I18n.t("action.update"), () -> {
            if (!AdminDialogSupport.requireFilled(List.of(key, name))) {
                return;
            }
            AdminDialogSupport.confirmUpdate(
                    I18n.t("admin.tenant.confirm.update.title"),
                    I18n.t("admin.tenant.confirm.update.message", tenant.name()),
                    () -> {
                        try {
                            service.updateTenant(tenant.tenantId(), key.getValue(), name.getValue(), residency.getValue(), locale.getValue());
                            dialog.close();
                            refresh();
                            Notification.show(I18n.t("admin.tenant.updated"));
                        } catch (RuntimeException exception) {
                            Notification.show(exception.getMessage());
                        }
                    }
            );
        });
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.sectionTitle(I18n.t("section.identity")),
                AdminDialogSupport.help(I18n.t("admin.tenant.update.help")),
                AdminDialogSupport.form(key, name, residency, locale)
        ));
        dialog.open();
    }

    private void openDeleteDialog(Tenant tenant) {
        AdminDialogSupport.confirmDeletion(I18n.t("admin.tenant.dialog.delete"), tenant.name(), () -> {
            try {
                service.archiveTenant(tenant.tenantId());
                refresh();
                Notification.show(I18n.t("admin.tenant.deleted"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
    }

    private void openRestoreDialog(Tenant tenant) {
        AdminDialogSupport.confirmUpdate(
                I18n.t("admin.tenant.confirm.restore.title"),
                I18n.t("admin.tenant.confirm.restore.message", tenant.name()),
                () -> {
                    try {
                        service.restoreTenant(tenant.tenantId());
                        refresh();
                        Notification.show(I18n.t("admin.tenant.restored"));
                    } catch (RuntimeException exception) {
                        Notification.show(exception.getMessage());
                    }
                }
        );
    }

    private void refresh() {
        List<Tenant> tenants = service.tenants();
        if (!showArchived.getValue()) {
            tenants = tenants.stream().filter(tenant -> tenant.status() != GovernanceStatus.ARCHIVED).toList();
        }
        grid.setItems(tenants);
    }
}
