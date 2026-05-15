package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.activelearning.model.ActiveLearningCycle;
import ai.datalithix.kanon.activelearning.model.ActiveLearningConfig;
import ai.datalithix.kanon.activelearning.model.CycleStatus;
import ai.datalithix.kanon.activelearning.model.SelectionStrategyType;
import ai.datalithix.kanon.activelearning.service.ActiveLearningCycleRepository;
import ai.datalithix.kanon.activelearning.service.ActiveLearningOrchestrator;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.ui.component.AdminDialogSupport;
import ai.datalithix.kanon.ui.component.HowItWorksSection;
import ai.datalithix.kanon.ui.i18n.I18n;
import ai.datalithix.kanon.ui.layout.MainLayout;
import ai.datalithix.kanon.ui.security.AdminSecuredView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;

@PageTitle("Active Learning | Kanon Platform")
@Route(value = "admin/active-learning", layout = MainLayout.class)
public class ActiveLearningAdminView extends VerticalLayout implements AdminSecuredView {
    private final ObjectProvider<ActiveLearningOrchestrator> orchestratorProvider;
    private final ObjectProvider<ActiveLearningCycleRepository> repositoryProvider;
    private final CurrentUserContextService currentUserContextService;
    private final Grid<ActiveLearningCycle> grid = new Grid<>(ActiveLearningCycle.class, false);

    public ActiveLearningAdminView(
            ObjectProvider<ActiveLearningOrchestrator> orchestratorProvider,
            ObjectProvider<ActiveLearningCycleRepository> repositoryProvider,
            CurrentUserContextService currentUserContextService
    ) {
        this.orchestratorProvider = orchestratorProvider;
        this.repositoryProvider = repositoryProvider;
        this.currentUserContextService = currentUserContextService;
        setSizeFull();
        setPadding(true);

        add(new HowItWorksSection(
                "Active learning cycles iteratively select the most informative records for annotation.",
                List.of(
                        "Choose a strategy: uncertainty sampling, diversity sampling, query-by-committee, or policy-defined.",
                        "Configure budget, thresholds, and human review requirements per cycle.",
                        "Each cycle progresses through selection, review, dataset update, retraining, and evaluation."
                )
        ));

        add(createToolbar());
        configureGrid();
        add(grid);
        refresh();
    }

    private ActiveLearningOrchestrator orchestrator() {
        ActiveLearningOrchestrator svc = orchestratorProvider.getIfAvailable();
        if (svc == null) throw new IllegalStateException("ActiveLearningOrchestrator not available");
        return svc;
    }

    private ActiveLearningCycleRepository repository() {
        ActiveLearningCycleRepository repo = repositoryProvider.getIfAvailable();
        if (repo == null) throw new IllegalStateException("ActiveLearningCycleRepository not available");
        return repo;
    }

    private HorizontalLayout createToolbar() {
        Button startButton = new Button("Start Cycle", VaadinIcon.PLUS.create(), event -> openStartDialog());
        startButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout toolbar = new HorizontalLayout(startButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.END);
        return toolbar;
    }

    private void configureGrid() {
        grid.addColumn(ActiveLearningCycle::cycleId).setHeader("Cycle ID").setAutoWidth(true);
        grid.addColumn(c -> c.status().name()).setHeader("Status").setAutoWidth(true);
        grid.addColumn(c -> c.strategyType().name()).setHeader("Strategy").setAutoWidth(true);
        grid.addColumn(c -> String.valueOf(c.selectedRecordCount())).setHeader("Selected").setAutoWidth(true);
        grid.addColumn(c -> c.modelEntryId()).setHeader("Model").setAutoWidth(true);
        grid.addColumn(c -> c.modelVersionId()).setHeader("Version").setAutoWidth(true);
        grid.addColumn(c -> formatInstant(c.startedAt())).setHeader("Started").setAutoWidth(true);
        grid.addComponentColumn(this::createActions).setHeader("Actions").setAutoWidth(true);
    }

    private HorizontalLayout createActions(ActiveLearningCycle cycle) {
        HorizontalLayout actions = new HorizontalLayout();
        if (cycle.status() == CycleStatus.AWAITING_REVIEW) {
            Button approveBtn = new Button(VaadinIcon.CHECK.create(), event -> {
                orchestrator().recordReviewProgress(
                        context().activeTenantId(), cycle.cycleId(),
                        cycle.selectedRecordCount(), context().username());
                refresh();
                Notification.show("Review approved");
            });
            approveBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
            approveBtn.setTooltipText("Approve (pass all)");
            actions.add(approveBtn);

            Button rejectBtn = new Button(VaadinIcon.CLOSE.create(), event -> {
                orchestrator().rejectCycle(context().activeTenantId(), cycle.cycleId(),
                        "Rejected in admin view", context().username());
                refresh();
                Notification.show("Cycle rejected");
            });
            rejectBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            rejectBtn.setTooltipText("Reject");
            actions.add(rejectBtn);
        }
        if (cycle.status() == CycleStatus.RETRAINING) {
            Button completeBtn = new Button(VaadinIcon.FLASK.create(), event -> {
                orchestrator().updateStatus(context().activeTenantId(), cycle.cycleId(),
                        CycleStatus.EVALUATING, null, context().username());
                refresh();
                Notification.show("Moved to evaluation");
            });
            completeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            completeBtn.setTooltipText("Complete Retraining");
            actions.add(completeBtn);
        }
        if (cycle.status() == CycleStatus.EVALUATING) {
            Button passBtn = new Button(VaadinIcon.THUMBS_UP.create(), event -> {
                orchestrator().updateStatus(context().activeTenantId(), cycle.cycleId(),
                        CycleStatus.PROMOTED, null, context().username());
                refresh();
                Notification.show("Cycle promoted");
            });
            passBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
            passBtn.setTooltipText("Promote");
            actions.add(passBtn);
        }
        if (canCancel(cycle.status())) {
            Button cancelBtn = new Button(VaadinIcon.STOP.create(), event -> {
                orchestrator().cancelCycle(context().activeTenantId(), cycle.cycleId(), context().username());
                refresh();
                Notification.show("Cycle cancelled");
            });
            cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            cancelBtn.setTooltipText("Cancel");
            actions.add(cancelBtn);
        }
        return actions;
    }

    private boolean canCancel(CycleStatus status) {
        return status == CycleStatus.AWAITING_REVIEW
                || status == CycleStatus.RETRAINING || status == CycleStatus.EVALUATING;
    }

    private void openStartDialog() {
        Dialog dialog = AdminDialogSupport.dialog("Start Active Learning Cycle", "700px");

        TextField modelEntryId = AdminDialogSupport.requiredText("Model Entry ID", "e.g. me-xxx");
        TextField modelVersionId = AdminDialogSupport.requiredText("Model Version ID", "e.g. mv-xxx");
        TextField sourceDatasetVersionId = AdminDialogSupport.requiredText("Source Dataset Version ID", "e.g. dv-xxx");
        ComboBox<SelectionStrategyType> strategy = new ComboBox<>("Strategy");
        strategy.setItems(SelectionStrategyType.values());
        strategy.setValue(SelectionStrategyType.UNCERTAINTY_SAMPLING);
        strategy.setWidthFull();

        NumberField uncertaintyThreshold = new NumberField("Uncertainty Threshold");
        uncertaintyThreshold.setValue(0.1);
        IntegerField budget = new IntegerField("Budget");
        budget.setValue(100);
        IntegerField maxIterations = new IntegerField("Max Iterations");
        maxIterations.setValue(5);

        AdminDialogSupport.footer(dialog, "Start", () -> {
            if (!AdminDialogSupport.requireFilled(List.of(modelEntryId, modelVersionId, sourceDatasetVersionId))) return;
            try {
                var config = new ActiveLearningConfig(
                        strategy.getValue(), uncertaintyThreshold.getValue(), budget.getValue(),
                        maxIterations.getValue(), true, null, false, Map.of());
                orchestrator().startCycle(
                        context().activeTenantId(),
                        modelEntryId.getValue(),
                        modelVersionId.getValue(),
                        sourceDatasetVersionId.getValue(),
                        config,
                        List.of(),
                        context().username()
                );
                dialog.close();
                refresh();
                Notification.show("Active learning cycle started");
            } catch (Exception e) {
                Notification.show("Failed: " + e.getMessage());
            }
        });

        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.form(modelEntryId, modelVersionId, sourceDatasetVersionId,
                        strategy, uncertaintyThreshold, budget, maxIterations)));
        dialog.open();
    }

    private void refresh() {
        List<ActiveLearningCycle> cycles = repository().findByTenant(context().activeTenantId());
        grid.setItems(cycles);
    }

    @Override
    public CurrentUserContext currentUserContext() {
        return currentUserContextService.currentUser();
    }

    private CurrentUserContext context() {
        return currentUserContext();
    }

    private String formatInstant(Instant instant) {
        if (instant == null) return "-";
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(I18n.currentLocale())
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }
}
