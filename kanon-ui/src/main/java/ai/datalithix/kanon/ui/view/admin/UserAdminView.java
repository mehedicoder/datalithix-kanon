package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.model.GovernanceStatus;
import ai.datalithix.kanon.tenant.model.UserAccount;
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
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;

@PageTitle("Users | Kanon Platform")
@Route(value = "admin/users", layout = MainLayout.class)
public class UserAdminView extends VerticalLayout implements AdminSecuredView {
    private final GovernanceAdministrationService service;
    private final CurrentUserContextService currentUserContextService;
    private final Grid<UserAccount> grid = new Grid<>(UserAccount.class, false);
    private final Checkbox showArchived = new Checkbox(I18n.t("action.show-archived"));

    public UserAdminView(GovernanceAdministrationService service, CurrentUserContextService currentUserContextService) {
        this.service = service;
        this.currentUserContextService = currentUserContextService;
        setSizeFull();
        setPadding(true);
        add(new HowItWorksSection(
                I18n.t("admin.user.overview.summary"),
                List.of(
                        I18n.t("admin.user.overview.identity"),
                        I18n.t("admin.user.overview.membership"),
                        I18n.t("admin.user.overview.multi-scope")
                )
        ));
        showArchived.addValueChangeListener(event -> refresh());
        HorizontalLayout toolbar = new HorizontalLayout();
        if (AdminAccess.canCreateUser(context())) {
            toolbar.add(new Button(I18n.t("admin.user.create"), event -> openCreateDialog()));
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
        grid.addColumn(UserAccount::userId).setHeader(I18n.t("grid.user")).setAutoWidth(true);
        grid.addColumn(UserAccount::username).setHeader(I18n.t("grid.username")).setAutoWidth(true);
        grid.addColumn(UserAccount::displayName).setHeader(I18n.t("grid.display-name")).setAutoWidth(true);
        grid.addColumn(UserAccount::email).setHeader(I18n.t("grid.email")).setAutoWidth(true);
        grid.addColumn(UserAccount::status).setHeader(I18n.t("grid.status")).setAutoWidth(true);
        grid.addColumn(user -> user.systemUser() ? I18n.t("user.type.system") : I18n.t("user.type.human")).setHeader(I18n.t("grid.type")).setAutoWidth(true);
        grid.addComponentColumn(user -> {
            CurrentUserContext context = context();
            if (user.status() == GovernanceStatus.ARCHIVED) {
                if (!AdminAccess.canRestoreUser(context)) {
                    return new HorizontalLayout(new Span(I18n.t("state.read-only")));
                }
                return new HorizontalLayout(new Button(I18n.t("action.restore"), event -> openRestoreDialog(user)));
            }
            HorizontalLayout actions = new HorizontalLayout();
            if (AdminAccess.canUpdateUser(context, user)) {
                actions.add(new Button(I18n.t("action.edit"), event -> openUpdateDialog(user)));
            }
            if (AdminAccess.canDeleteUser(context, user)) {
                actions.add(new Button(I18n.t("action.delete"), event -> openDeleteDialog(user)));
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
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.user.dialog.create"), "720px");
        TextField username = AdminDialogSupport.requiredText(I18n.t("field.username"), I18n.t("admin.user.username.helper"));
        TextField email = AdminDialogSupport.requiredText(I18n.t("field.email"), I18n.t("admin.user.email.helper"));
        TextField displayName = AdminDialogSupport.requiredText(I18n.t("field.display-name"), I18n.t("admin.user.display-name.helper"));
        PasswordField password = new PasswordField(I18n.t("field.initial-password"));
        password.setRequiredIndicatorVisible(true);
        password.setHelperText(I18n.t("admin.user.password.helper"));
        password.setValue("change-me");
        AdminDialogSupport.footer(dialog, I18n.t("action.create"), () -> {
            if (!AdminDialogSupport.requireFilled(List.of(username, email, displayName))) {
                return;
            }
            if (password.getValue() == null || password.getValue().isBlank()) {
                password.setInvalid(true);
                password.setErrorMessage(I18n.t("validation.required", I18n.t("field.initial-password")));
                return;
            }
            password.setInvalid(false);
            try {
                service.createUser(username.getValue(), email.getValue(), displayName.getValue(), password.getValue());
                dialog.close();
                refresh();
                Notification.show(I18n.t("admin.user.created"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.sectionTitle(I18n.t("section.identity")),
                AdminDialogSupport.help(I18n.t("admin.user.create.help")),
                AdminDialogSupport.form(username, email, displayName, password)
        ));
        dialog.open();
    }

    private void openUpdateDialog(UserAccount user) {
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.user.dialog.update"), "720px");
        TextField username = AdminDialogSupport.requiredText(I18n.t("field.username"), I18n.t("admin.user.username.helper"));
        username.setValue(user.username());
        TextField email = AdminDialogSupport.requiredText(I18n.t("field.email"), I18n.t("admin.user.email.helper"));
        email.setValue(user.email());
        TextField displayName = AdminDialogSupport.requiredText(I18n.t("field.display-name"), I18n.t("admin.user.display-name.helper"));
        displayName.setValue(user.displayName());
        AdminDialogSupport.footer(dialog, I18n.t("action.update"), () -> {
            if (!AdminDialogSupport.requireFilled(List.of(username, email, displayName))) {
                return;
            }
            AdminDialogSupport.confirmUpdate(
                    I18n.t("admin.user.confirm.update.title"),
                    I18n.t("admin.user.confirm.update.message", user.username()),
                    () -> {
                        try {
                            service.updateUser(user.userId(), username.getValue(), email.getValue(), displayName.getValue());
                            dialog.close();
                            refresh();
                            Notification.show(I18n.t("admin.user.updated"));
                        } catch (RuntimeException exception) {
                            Notification.show(exception.getMessage());
                        }
                    }
            );
        });
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.sectionTitle(I18n.t("section.identity")),
                AdminDialogSupport.help(I18n.t("admin.user.update.help")),
                AdminDialogSupport.form(username, email, displayName)
        ));
        dialog.open();
    }

    private void openDeleteDialog(UserAccount user) {
        AdminDialogSupport.confirmDeletion(I18n.t("admin.user.dialog.delete"), user.username(), () -> {
            try {
                service.archiveUser(user.userId());
                refresh();
                Notification.show(I18n.t("admin.user.deleted"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
    }

    private void openRestoreDialog(UserAccount user) {
        AdminDialogSupport.confirmUpdate(
                I18n.t("admin.user.confirm.restore.title"),
                I18n.t("admin.user.confirm.restore.message", user.username()),
                () -> {
                    try {
                        service.restoreUser(user.userId());
                        refresh();
                        Notification.show(I18n.t("admin.user.restored"));
                    } catch (RuntimeException exception) {
                        Notification.show(exception.getMessage());
                    }
                }
        );
    }

    private void refresh() {
        List<UserAccount> users = service.users();
        if (!showArchived.getValue()) {
            users = users.stream().filter(user -> user.status() != GovernanceStatus.ARCHIVED).toList();
        }
        grid.setItems(users);
    }
}
