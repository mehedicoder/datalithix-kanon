package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.agentruntime.model.AgentExecutionMode;
import ai.datalithix.kanon.agentruntime.model.AgentProfile;
import ai.datalithix.kanon.agentruntime.model.AgentStatus;
import ai.datalithix.kanon.agentruntime.model.AgentType;
import ai.datalithix.kanon.agentruntime.service.AgentAdministrationService;
import ai.datalithix.kanon.common.model.PageSpec;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.model.SortDirection;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.ui.component.AdminDialogSupport;
import ai.datalithix.kanon.ui.component.HowItWorksSection;
import ai.datalithix.kanon.ui.i18n.I18n;
import ai.datalithix.kanon.ui.layout.MainLayout;
import ai.datalithix.kanon.ui.security.AdminAccess;
import ai.datalithix.kanon.ui.security.AdminSecuredView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;
import java.util.Set;

@PageTitle("Agents | Kanon Platform")
@Route(value = "admin/agents", layout = MainLayout.class)
public class AgentAdminView extends VerticalLayout implements AdminSecuredView {
    private final AgentAdministrationService service;
    private final CurrentUserContextService currentUserContextService;
    private final Grid<AgentProfile> grid = new Grid<>(AgentProfile.class, false);
    private final TextField searchField = new TextField();
    private final ComboBox<AgentType> typeFilter = new ComboBox<>();
    private final Checkbox showArchived = new Checkbox();

    public AgentAdminView(AgentAdministrationService service, CurrentUserContextService currentUserContextService) {
        this.service = service;
        this.currentUserContextService = currentUserContextService;
        setSizeFull();
        setPadding(true);
        add(new HowItWorksSection(
                I18n.t("admin.agent.overview.summary"),
                List.of(
                        I18n.t("admin.agent.overview.list"),
                        I18n.t("admin.agent.overview.execution"),
                        I18n.t("admin.agent.overview.routing")
                )
        ));

        HorizontalLayout toolbar = createToolbar();
        add(toolbar);
        configureGrid();
        add(grid);
        refresh();
    }

    private HorizontalLayout createToolbar() {
        searchField.setPlaceholder(I18n.t("action.search"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.addValueChangeListener(e -> refresh());
        
        typeFilter.setPlaceholder(I18n.t("field.type"));
        typeFilter.setItems(AgentType.values());
        typeFilter.setClearButtonVisible(true);
        typeFilter.addValueChangeListener(e -> refresh());

        showArchived.setLabel(I18n.t("action.show-archived"));
        showArchived.addValueChangeListener(e -> refresh());

        HorizontalLayout filters = new HorizontalLayout(searchField, typeFilter, showArchived);
        filters.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        HorizontalLayout actions = new HorizontalLayout();
        if (AdminAccess.canCreateAgent(context())) {
            Button createBtn = new Button(I18n.t("action.create"), VaadinIcon.PLUS.create(), e -> openCreateDialog());
            createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            actions.add(createBtn);
        }

        HorizontalLayout layout = new HorizontalLayout(filters, actions);
        layout.setWidthFull();
        layout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        return layout;
    }

    private void configureGrid() {
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        
        grid.addComponentColumn(agent -> {
            Icon statusIcon = VaadinIcon.CIRCLE.create();
            statusIcon.setColor(agent.enabled() ? "var(--lumo-success-color)" : "var(--lumo-disabled-text-color)");
            statusIcon.setTooltipText(agent.enabled() ? I18n.t("state.enabled") : I18n.t("state.disabled"));
            return statusIcon;
        }).setHeader("").setWidth("60px").setFlexGrow(0);
        
        grid.addColumn(AgentProfile::name).setHeader(I18n.t("grid.name")).setAutoWidth(true);
        grid.addColumn(AgentProfile::agentType).setHeader(I18n.t("grid.type")).setAutoWidth(true);
        grid.addColumn(AgentProfile::executionMode).setHeader(I18n.t("grid.mode")).setAutoWidth(true);
        grid.addColumn(agent -> formatInstant(agent.lastRunAt())).setHeader(I18n.t("grid.last-run")).setAutoWidth(true);
        
        grid.addComponentColumn(agent -> {
            HorizontalLayout actions = new HorizontalLayout();
            
            if (agent.status() == AgentStatus.RETIRED) {
                if (AdminAccess.canRestoreAgent(context(), agent)) {
                    Button restore = new Button(VaadinIcon.ROTATE_LEFT.create(), e -> restoreAgent(agent));
                    restore.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                    restore.setTooltipText(I18n.t("action.restore"));
                    actions.add(restore);
                }
                if (AdminAccess.canDeleteAgent(context(), agent)) {
                    Button delete = new Button(VaadinIcon.TRASH.create(), e -> permanentlyDeleteAgent(agent));
                    delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                    delete.setTooltipText(I18n.t("action.delete"));
                    actions.add(delete);
                }
            } else if (AdminAccess.canUpdateAgent(context(), agent)) {
                Button edit = new Button(VaadinIcon.EDIT.create(), e -> openEditDialog(agent));
                edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                edit.setTooltipText(I18n.t("action.edit"));
                actions.add(edit);

                Button toggle = new Button(VaadinIcon.POWER_OFF.create(), e -> toggleAgentStatus(agent));
                toggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                toggle.setTooltipText(agent.enabled() ? I18n.t("action.disable") : I18n.t("action.enable"));
                actions.add(toggle);
            }

            if (agent.status() != AgentStatus.RETIRED && AdminAccess.canDeleteAgent(context(), agent)) {
                Button delete = new Button(VaadinIcon.TRASH.create(), e -> deleteAgent(agent));
                delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                delete.setTooltipText(I18n.t("action.archive"));
                actions.add(delete);
            }
            
            if (actions.getComponentCount() == 0) {
                actions.add(new Span(I18n.t("state.read-only")));
            }

            return actions;
        }).setHeader(I18n.t("grid.actions")).setAutoWidth(true);
    }

    private void openCreateDialog() {
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.agent.dialog.create"), "720px");
        TextField name = AdminDialogSupport.requiredText(I18n.t("field.name"), I18n.t("admin.agent.name.helper"));
        ComboBox<AgentType> type = new ComboBox<>(I18n.t("field.type"));
        type.setRequiredIndicatorVisible(true);
        type.setItems(AgentType.values());
        ComboBox<AgentExecutionMode> mode = new ComboBox<>(I18n.t("field.mode"));
        mode.setRequiredIndicatorVisible(true);
        mode.setItems(AgentExecutionMode.values());

        AdminDialogSupport.footer(dialog, I18n.t("action.create"), () -> {
            if (!AdminDialogSupport.requireFilled(List.of(name))) {
                return;
            }
            if (type.getValue() == null || mode.getValue() == null) {
                Notification.show(I18n.t("admin.agent.validation.type-mode-required"));
                return;
            }
            try {
                service.createAgent(
                        context().activeTenantId(),
                        name.getValue(),
                        type.getValue(),
                        "",
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        null,
                        null,
                        mode.getValue(),
                        60,
                        null,
                        3,
                        10,
                        0,
                        null,
                        null,
                        null,
                        context().username()
                );
                dialog.close();
                refresh();
                Notification.show(I18n.t("admin.agent.created"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.sectionTitle(I18n.t("section.identity")),
                AdminDialogSupport.help(I18n.t("admin.agent.create.help")),
                AdminDialogSupport.form(name, type, mode)
        ));
        dialog.open();
    }

    private void openEditDialog(AgentProfile agent) {
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.agent.dialog.update"), "720px");
        TextField name = AdminDialogSupport.requiredText(I18n.t("field.name"), I18n.t("admin.agent.name.helper"));
        name.setValue(agent.name());
        ComboBox<AgentType> type = new ComboBox<>(I18n.t("field.type"));
        type.setItems(AgentType.values());
        type.setValue(agent.agentType());
        type.setReadOnly(true);
        ComboBox<AgentExecutionMode> mode = new ComboBox<>(I18n.t("field.mode"));
        mode.setItems(AgentExecutionMode.values());
        mode.setValue(agent.executionMode());

        AdminDialogSupport.footer(dialog, I18n.t("action.update"), () -> {
            if (!AdminDialogSupport.requireFilled(List.of(name))) {
                return;
            }
            if (mode.getValue() == null) {
                Notification.show(I18n.t("admin.agent.validation.mode-required"));
                return;
            }
            try {
                service.updateAgent(
                        agent.tenantId(),
                        agent.agentId(),
                        name.getValue(),
                        agent.description(),
                        agent.supportedDomains(),
                        agent.supportedTaskTypes(),
                        agent.supportedAssetTypes(),
                        agent.supportedSourceTypes(),
                        agent.supportedAnnotationTypes(),
                        agent.inputSchemaRef(),
                        agent.outputSchemaRef(),
                        mode.getValue(),
                        agent.timeoutSeconds(),
                        agent.retryPolicy(),
                        agent.maxAttempts(),
                        agent.concurrencyLimit(),
                        agent.priority(),
                        agent.queueName(),
                        agent.modelRoutePolicy(),
                        agent.fallbackModelProfile(),
                        context().username()
                );
                dialog.close();
                refresh();
                Notification.show(I18n.t("admin.agent.updated"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.sectionTitle(I18n.t("section.identity")),
                AdminDialogSupport.help(I18n.t("admin.agent.update.help")),
                AdminDialogSupport.form(name, type, mode)
        ));
        dialog.open();
    }

    private void toggleAgentStatus(AgentProfile agent) {
        try {
            if (agent.enabled()) {
                service.disableAgent(agent.tenantId(), agent.agentId(), context().username());
                Notification.show(I18n.t("admin.agent.disabled"));
            } else {
                service.enableAgent(agent.tenantId(), agent.agentId(), context().username());
                Notification.show(I18n.t("admin.agent.enabled"));
            }
            refresh();
        } catch (RuntimeException exception) {
            Notification.show(exception.getMessage());
        }
    }

    private void deleteAgent(AgentProfile agent) {
        AdminDialogSupport.confirmDeletion(I18n.t("admin.agent.dialog.archive"), agent.name(), () -> {
            try {
                service.archiveAgent(agent.tenantId(), agent.agentId(), context().username());
                refresh();
                Notification.show(I18n.t("admin.agent.archived"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
    }

    private void restoreAgent(AgentProfile agent) {
        AdminDialogSupport.confirmUpdate(
                I18n.t("admin.agent.confirm.restore.title"),
                I18n.t("admin.agent.confirm.restore.message", agent.name()),
                () -> {
                    try {
                        service.restoreAgent(agent.tenantId(), agent.agentId(), context().username());
                        refresh();
                        Notification.show(I18n.t("admin.agent.restored"));
                    } catch (RuntimeException exception) {
                        Notification.show(exception.getMessage());
                    }
                }
        );
    }

    private void permanentlyDeleteAgent(AgentProfile agent) {
        AdminDialogSupport.confirmDeletion(I18n.t("admin.agent.dialog.delete"), agent.name(), () -> {
            try {
                service.deleteAgent(agent.tenantId(), agent.agentId());
                refresh();
                Notification.show(I18n.t("admin.agent.deleted"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
    }

    private void refresh() {
        QuerySpec query = new QuerySpec(
            context().activeTenantId(),
            new PageSpec(0, 50, "name", SortDirection.ASC),
            List.of(),
            Map.of()
        );
        List<AgentProfile> agents = service.findPage(query).items();

        String searchText = searchField.getValue() == null ? "" : searchField.getValue().toLowerCase();
        AgentType selectedType = typeFilter.getValue();

        List<AgentProfile> filtered = agents.stream()
                .filter(a -> Boolean.TRUE.equals(showArchived.getValue()) || a.status() != AgentStatus.RETIRED)
                .filter(a -> searchText.isEmpty() || a.name().toLowerCase().contains(searchText))
                .filter(a -> selectedType == null || a.agentType() == selectedType)
                .toList();

        grid.setItems(filtered);
    }

    @Override
    public CurrentUserContext currentUserContext() {
        return currentUserContextService.currentUser();
    }

    private CurrentUserContext context() {
        return currentUserContext();
    }

    private String formatInstant(Instant instant) {
        if (instant == null) {
            return I18n.t("state.never");
        }
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(I18n.currentLocale())
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }
}
