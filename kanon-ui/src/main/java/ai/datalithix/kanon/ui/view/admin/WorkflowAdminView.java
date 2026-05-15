package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DataResidency;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.model.AuditMetadata;
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
import ai.datalithix.kanon.workflow.model.PlannerType;
import ai.datalithix.kanon.workflow.model.WorkflowDefinition;
import ai.datalithix.kanon.workflow.model.WorkflowStatus;
import ai.datalithix.kanon.workflow.model.WorkflowType;
import ai.datalithix.kanon.workflow.service.WorkflowDefinitionRepository;
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
import com.vaadin.flow.component.textfield.TextArea;
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
import java.util.UUID;

@PageTitle("Workflows | Kanon Platform")
@Route(value = "admin/workflows", layout = MainLayout.class)
public class WorkflowAdminView extends VerticalLayout implements AdminSecuredView {
    private final WorkflowDefinitionRepository repository;
    private final CurrentUserContextService currentUserContextService;
    private final Grid<WorkflowDefinition> grid = new Grid<>(WorkflowDefinition.class, false);
    private final TextField searchField = new TextField();
    private final ComboBox<WorkflowType> workflowTypeFilter = new ComboBox<>();
    private final ComboBox<PlannerType> plannerTypeFilter = new ComboBox<>();
    private final Checkbox showArchived = new Checkbox();

    public WorkflowAdminView(
            WorkflowDefinitionRepository repository,
            CurrentUserContextService currentUserContextService
    ) {
        this.repository = repository;
        this.currentUserContextService = currentUserContextService;
        setSizeFull();
        setPadding(true);
        add(new HowItWorksSection(
                I18n.t("admin.workflow.overview.summary"),
                List.of(
                        I18n.t("admin.workflow.overview.boundary"),
                        I18n.t("admin.workflow.overview.planning"),
                        I18n.t("admin.workflow.overview.agent-routing")
                )
        ));

        add(createToolbar());
        configureGrid();
        add(grid);
        refresh();
    }

    private HorizontalLayout createToolbar() {
        searchField.setPlaceholder(I18n.t("action.search"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.addValueChangeListener(event -> refresh());

        workflowTypeFilter.setPlaceholder(I18n.t("field.workflow-type"));
        workflowTypeFilter.setItems(WorkflowType.values());
        workflowTypeFilter.setClearButtonVisible(true);
        workflowTypeFilter.addValueChangeListener(event -> refresh());

        plannerTypeFilter.setPlaceholder(I18n.t("field.planner-type"));
        plannerTypeFilter.setItems(PlannerType.values());
        plannerTypeFilter.setClearButtonVisible(true);
        plannerTypeFilter.addValueChangeListener(event -> refresh());

        showArchived.setLabel(I18n.t("action.show-archived"));
        showArchived.addValueChangeListener(event -> refresh());

        HorizontalLayout filters = new HorizontalLayout(searchField, workflowTypeFilter, plannerTypeFilter, showArchived);
        filters.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        HorizontalLayout actions = new HorizontalLayout();
        if (AdminAccess.canCreateWorkflow(context())) {
            Button createButton = new Button(I18n.t("admin.workflow.create"), VaadinIcon.PLUS.create(), event -> openCreateDialog());
            createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            actions.add(createButton);
        }

        HorizontalLayout layout = new HorizontalLayout(filters, actions);
        layout.setWidthFull();
        layout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        return layout;
    }

    private void configureGrid() {
        grid.addComponentColumn(workflow -> {
            Icon statusIcon = VaadinIcon.CIRCLE.create();
            statusIcon.setColor(workflow.enabled() ? "var(--lumo-success-color)" : "var(--lumo-disabled-text-color)");
            statusIcon.setTooltipText(workflow.enabled() ? I18n.t("state.enabled") : I18n.t("state.disabled"));
            return statusIcon;
        }).setHeader("").setWidth("60px").setFlexGrow(0);

        grid.addColumn(WorkflowDefinition::name).setHeader(I18n.t("grid.name")).setAutoWidth(true);
        grid.addColumn(WorkflowDefinition::workflowType).setHeader(I18n.t("grid.workflow-type")).setAutoWidth(true);
        grid.addColumn(WorkflowDefinition::plannerType).setHeader(I18n.t("grid.planner-type")).setAutoWidth(true);
        grid.addColumn(WorkflowDefinition::domainType).setHeader(I18n.t("grid.domain")).setAutoWidth(true);
        grid.addColumn(WorkflowDefinition::taskType).setHeader(I18n.t("grid.task-type")).setAutoWidth(true);
        grid.addColumn(WorkflowDefinition::status).setHeader(I18n.t("grid.status")).setAutoWidth(true);
        grid.addColumn(workflow -> formatInstant(workflow.audit().updatedAt())).setHeader(I18n.t("grid.updated")).setAutoWidth(true);

        grid.addComponentColumn(workflow -> {
            HorizontalLayout actions = new HorizontalLayout();

            if (AdminAccess.canUpdateWorkflow(context(), workflow)) {
                if (workflow.status() == WorkflowStatus.RETIRED) {
                    Button restore = new Button(VaadinIcon.ROTATE_LEFT.create(), event -> restoreWorkflow(workflow));
                    restore.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                    restore.setTooltipText(I18n.t("action.restore"));
                    actions.add(restore);
                } else {
                    Button edit = new Button(VaadinIcon.EDIT.create(), event -> openEditDialog(workflow));
                    edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                    edit.setTooltipText(I18n.t("action.edit"));
                    actions.add(edit);

                    Button toggle = new Button(VaadinIcon.POWER_OFF.create(), event -> toggleWorkflowStatus(workflow));
                    toggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                    toggle.setTooltipText(workflow.enabled() ? I18n.t("action.disable") : I18n.t("action.enable"));
                    actions.add(toggle);
                }
            }

            if (AdminAccess.canDeleteWorkflow(context(), workflow)) {
                if (workflow.status() == WorkflowStatus.RETIRED) {
                    Button delete = new Button(VaadinIcon.TRASH.create(), event -> deleteWorkflow(workflow));
                    delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                    delete.setTooltipText(I18n.t("action.delete"));
                    actions.add(delete);
                } else {
                    Button archive = new Button(VaadinIcon.ARCHIVE.create(), event -> archiveWorkflow(workflow));
                    archive.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                    archive.setTooltipText(I18n.t("action.archive"));
                    actions.add(archive);
                }
            }

            if (actions.getComponentCount() == 0) {
                actions.add(new Span(I18n.t("state.read-only")));
            }
            return actions;
        }).setHeader(I18n.t("grid.actions")).setAutoWidth(true);
    }

    private void openCreateDialog() {
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.workflow.dialog.create"), "820px");
        TextField name = AdminDialogSupport.requiredText(I18n.t("field.name"), I18n.t("admin.workflow.name.helper"));
        ComboBox<WorkflowType> workflowType = new ComboBox<>(I18n.t("field.workflow-type"));
        workflowType.setRequiredIndicatorVisible(true);
        workflowType.setItems(WorkflowType.values());
        ComboBox<PlannerType> plannerType = new ComboBox<>(I18n.t("field.planner-type"));
        plannerType.setRequiredIndicatorVisible(true);
        plannerType.setItems(PlannerType.values());
        plannerType.setValue(PlannerType.STATIC);
        ComboBox<DomainType> domainType = new ComboBox<>(I18n.t("field.domain"));
        domainType.setItems(DomainType.values());
        domainType.setValue(DomainType.CUSTOM);
        ComboBox<AiTaskType> taskType = new ComboBox<>(I18n.t("field.task-type"));
        taskType.setItems(AiTaskType.values());
        taskType.setValue(AiTaskType.REASONING);
        TextField modelRoutePolicy = new TextField(I18n.t("field.model-route-policy"));
        modelRoutePolicy.setPlaceholder(I18n.t("admin.workflow.model-route-policy.placeholder"));
        TextArea goal = new TextArea(I18n.t("field.goal"));
        goal.setPlaceholder(I18n.t("admin.workflow.goal.placeholder"));
        goal.setWidthFull();

        AdminDialogSupport.footer(dialog, I18n.t("action.create"), () -> {
            if (!AdminDialogSupport.requireFilled(List.of(name))) {
                return;
            }
            if (workflowType.getValue() == null || plannerType.getValue() == null) {
                Notification.show(I18n.t("admin.workflow.validation.type-planner-required"));
                return;
            }
            try {
                WorkflowDefinition workflow = new WorkflowDefinition(
                        UUID.randomUUID().toString(),
                        context().activeTenantId(),
                        context().activeOrganizationId(),
                        context().activeWorkspaceId(),
                        name.getValue(),
                        workflowType.getValue(),
                        "",
                        WorkflowStatus.DRAFT,
                        false,
                        domainType.getValue(),
                        taskType.getValue(),
                        AssetType.UNKNOWN,
                        SourceType.MANUAL_ENTRY,
                        null,
                        null,
                        DataResidency.TENANT_DEFINED,
                        goal.getValue(),
                        plannerType.getValue(),
                        null,
                        null,
                        List.of(),
                        List.of(),
                        null,
                        emptyToNull(modelRoutePolicy.getValue()),
                        Set.of(),
                        auditNow(0)
                );
                repository.save(workflow);
                dialog.close();
                refresh();
                Notification.show(I18n.t("admin.workflow.created"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.sectionTitle(I18n.t("section.identity")),
                AdminDialogSupport.help(I18n.t("admin.workflow.create.help")),
                AdminDialogSupport.form(name, workflowType, plannerType, domainType, taskType, modelRoutePolicy),
                goal
        ));
        dialog.open();
    }

    private void openEditDialog(WorkflowDefinition workflow) {
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.workflow.dialog.update"), "820px");
        TextField name = AdminDialogSupport.requiredText(I18n.t("field.name"), I18n.t("admin.workflow.name.helper"));
        name.setValue(workflow.name());
        ComboBox<WorkflowType> workflowType = new ComboBox<>(I18n.t("field.workflow-type"));
        workflowType.setItems(WorkflowType.values());
        workflowType.setValue(workflow.workflowType());
        workflowType.setReadOnly(true);
        ComboBox<PlannerType> plannerType = new ComboBox<>(I18n.t("field.planner-type"));
        plannerType.setItems(PlannerType.values());
        plannerType.setValue(workflow.plannerType());
        ComboBox<DomainType> domainType = new ComboBox<>(I18n.t("field.domain"));
        domainType.setItems(DomainType.values());
        domainType.setValue(workflow.domainType());
        ComboBox<AiTaskType> taskType = new ComboBox<>(I18n.t("field.task-type"));
        taskType.setItems(AiTaskType.values());
        taskType.setValue(workflow.taskType());
        TextField modelRoutePolicy = new TextField(I18n.t("field.model-route-policy"));
        modelRoutePolicy.setValue(nullToEmpty(workflow.modelRoutePolicy()));
        TextArea goal = new TextArea(I18n.t("field.goal"));
        goal.setValue(nullToEmpty(workflow.goal()));
        goal.setWidthFull();

        AdminDialogSupport.footer(dialog, I18n.t("action.update"), () -> {
            if (!AdminDialogSupport.requireFilled(List.of(name))) {
                return;
            }
            if (plannerType.getValue() == null) {
                Notification.show(I18n.t("admin.workflow.validation.planner-required"));
                return;
            }
            try {
                repository.save(copyWorkflow(
                        workflow,
                        name.getValue(),
                        workflow.status(),
                        workflow.enabled(),
                        domainType.getValue(),
                        taskType.getValue(),
                        plannerType.getValue(),
                        goal.getValue(),
                        emptyToNull(modelRoutePolicy.getValue())
                ));
                dialog.close();
                refresh();
                Notification.show(I18n.t("admin.workflow.updated"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.sectionTitle(I18n.t("section.identity")),
                AdminDialogSupport.help(I18n.t("admin.workflow.update.help")),
                AdminDialogSupport.form(name, workflowType, plannerType, domainType, taskType, modelRoutePolicy),
                goal
        ));
        dialog.open();
    }

    private void toggleWorkflowStatus(WorkflowDefinition workflow) {
        WorkflowStatus newStatus = workflow.enabled() ? WorkflowStatus.DISABLED : WorkflowStatus.ACTIVE;
        boolean newEnabled = !workflow.enabled();
        repository.save(copyWorkflow(
                workflow,
                workflow.name(),
                newStatus,
                newEnabled,
                workflow.domainType(),
                workflow.taskType(),
                workflow.plannerType(),
                workflow.goal(),
                workflow.modelRoutePolicy()
        ));
        refresh();
        Notification.show(newEnabled ? I18n.t("admin.workflow.enabled") : I18n.t("admin.workflow.disabled"));
    }

    private void archiveWorkflow(WorkflowDefinition workflow) {
        AdminDialogSupport.confirmDeletion(I18n.t("admin.workflow.dialog.archive"), workflow.name(), () -> {
            repository.save(copyWorkflow(
                    workflow,
                    workflow.name(),
                    WorkflowStatus.RETIRED,
                    false,
                    workflow.domainType(),
                    workflow.taskType(),
                    workflow.plannerType(),
                    workflow.goal(),
                    workflow.modelRoutePolicy()
            ));
            refresh();
            Notification.show(I18n.t("admin.workflow.archived"));
        });
    }

    private void restoreWorkflow(WorkflowDefinition workflow) {
        AdminDialogSupport.confirmUpdate(
                I18n.t("admin.workflow.confirm.restore.title"),
                I18n.t("admin.workflow.confirm.restore.message", workflow.name()),
                () -> {
                    repository.save(copyWorkflow(
                            workflow,
                            workflow.name(),
                            WorkflowStatus.DRAFT,
                            false,
                            workflow.domainType(),
                            workflow.taskType(),
                            workflow.plannerType(),
                            workflow.goal(),
                            workflow.modelRoutePolicy()
                    ));
                    refresh();
                    Notification.show(I18n.t("admin.workflow.restored"));
                }
        );
    }

    private void deleteWorkflow(WorkflowDefinition workflow) {
        AdminDialogSupport.confirmDeletion(I18n.t("admin.workflow.dialog.delete"), workflow.name(), () -> {
            repository.deleteById(workflow.tenantId(), workflow.workflowId());
            refresh();
            Notification.show(I18n.t("admin.workflow.deleted"));
        });
    }

    private void refresh() {
        QuerySpec query = new QuerySpec(
                context().activeTenantId(),
                new PageSpec(0, 50, "name", SortDirection.ASC),
                List.of(),
                Map.of()
        );
        String searchText = searchField.getValue() == null ? "" : searchField.getValue().toLowerCase();
        WorkflowType selectedWorkflowType = workflowTypeFilter.getValue();
        PlannerType selectedPlannerType = plannerTypeFilter.getValue();

        List<WorkflowDefinition> workflows = repository.findPage(query).items().stream()
                .filter(this::isVisibleInCurrentScope)
                .filter(workflow -> Boolean.TRUE.equals(showArchived.getValue()) || workflow.status() != WorkflowStatus.RETIRED)
                .filter(workflow -> searchText.isEmpty() || workflow.name().toLowerCase().contains(searchText))
                .filter(workflow -> selectedWorkflowType == null || workflow.workflowType() == selectedWorkflowType)
                .filter(workflow -> selectedPlannerType == null || workflow.plannerType() == selectedPlannerType)
                .toList();

        grid.setItems(workflows);
    }

    @Override
    public CurrentUserContext currentUserContext() {
        return currentUserContextService.currentUser();
    }

    private CurrentUserContext context() {
        return currentUserContext();
    }

    private boolean isVisibleInCurrentScope(WorkflowDefinition workflow) {
        CurrentUserContext context = context();
        if (!context.activeTenantId().equals(workflow.tenantId())) {
            return false;
        }
        if (AdminAccess.hasAny(context,
                "platform.workflow.read", "platform.workflow.create", "platform.workflow.update", "platform.workflow.delete",
                "tenant.workflow.read", "tenant.workflow.create", "tenant.workflow.update", "tenant.workflow.delete")) {
            return true;
        }
        return AdminAccess.hasAny(context,
                "workspace.workflow.read", "workspace.workflow.create", "workspace.workflow.update", "workspace.workflow.delete")
                && context.activeOrganizationId().equals(workflow.organizationId())
                && context.activeWorkspaceId().equals(workflow.workspaceId());
    }

    private WorkflowDefinition copyWorkflow(
            WorkflowDefinition workflow,
            String name,
            WorkflowStatus status,
            boolean enabled,
            DomainType domainType,
            AiTaskType taskType,
            PlannerType plannerType,
            String goal,
            String modelRoutePolicy
    ) {
        return new WorkflowDefinition(
                workflow.workflowId(),
                workflow.tenantId(),
                workflow.organizationId(),
                workflow.workspaceId(),
                name,
                workflow.workflowType(),
                workflow.description(),
                status,
                enabled,
                domainType,
                taskType,
                workflow.assetType(),
                workflow.sourceType(),
                workflow.policyProfile(),
                workflow.regulatoryAct(),
                workflow.dataResidency(),
                goal,
                plannerType,
                workflow.plannerVersion(),
                workflow.actionSetRef(),
                workflow.preconditions(),
                workflow.constraints(),
                workflow.fallbackWorkflowRef(),
                modelRoutePolicy,
                workflow.allowedModelProfileIds(),
                updatedAudit(workflow.audit())
        );
    }

    private AuditMetadata auditNow(long version) {
        Instant now = Instant.now();
        return new AuditMetadata(now, context().username(), now, context().username(), version);
    }

    private AuditMetadata updatedAudit(AuditMetadata existing) {
        return new AuditMetadata(
                existing.createdAt(),
                existing.createdBy(),
                Instant.now(),
                context().username(),
                existing.version() + 1
        );
    }

    private String formatInstant(Instant instant) {
        if (instant == null) {
            return I18n.t("state.not-available");
        }
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(I18n.currentLocale())
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
