package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.modelregistry.model.DeploymentTarget;
import ai.datalithix.kanon.modelregistry.model.EvaluationRun;
import ai.datalithix.kanon.modelregistry.model.ModelEntry;
import ai.datalithix.kanon.modelregistry.model.ModelLifecycleStage;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import ai.datalithix.kanon.modelregistry.service.DeploymentService;
import ai.datalithix.kanon.modelregistry.service.EvaluationService;
import ai.datalithix.kanon.modelregistry.service.ModelRegistryService;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.ui.component.AdminDialogSupport;
import ai.datalithix.kanon.ui.component.HowItWorksSection;
import ai.datalithix.kanon.ui.i18n.I18n;
import ai.datalithix.kanon.ui.layout.MainLayout;
import ai.datalithix.kanon.ui.security.AdminSecuredView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;

@PageTitle("Model Registry | Kanon Platform")
@Route(value = "admin/model-registry", layout = MainLayout.class)
public class ModelRegistryAdminView extends VerticalLayout implements AdminSecuredView {
    private final ObjectProvider<ModelRegistryService> registryProvider;
    private final ObjectProvider<EvaluationService> evaluationProvider;
    private final ObjectProvider<DeploymentService> deploymentProvider;
    private final CurrentUserContextService currentUserContextService;
    private final Grid<ModelEntry> modelGrid = new Grid<>(ModelEntry.class, false);
    private final Grid<DeploymentTarget> deploymentGrid = new Grid<>(DeploymentTarget.class, false);

    public ModelRegistryAdminView(
            ObjectProvider<ModelRegistryService> registryProvider,
            ObjectProvider<EvaluationService> evaluationProvider,
            ObjectProvider<DeploymentService> deploymentProvider,
            CurrentUserContextService currentUserContextService
    ) {
        this.registryProvider = registryProvider;
        this.evaluationProvider = evaluationProvider;
        this.deploymentProvider = deploymentProvider;
        this.currentUserContextService = currentUserContextService;
        setSizeFull();
        setPadding(true);

        add(new HowItWorksSection(
                "Model Registry manages model versions, evaluation, and deployments.",
                List.of(
                        "Register trained models with full lineage to dataset and training run.",
                        "Promote models through lifecycle stages: dev → staging → production.",
                        "Evaluate against test sets, deploy to serving endpoints, and roll back if needed."
                )
        ));

        Tabs tabs = new Tabs(new Tab("Models"), new Tab("Deployments"));
        tabs.addSelectedChangeListener(event -> {
            modelGrid.setVisible(event.getSelectedTab().getLabel().equals("Models"));
            deploymentGrid.setVisible(event.getSelectedTab().getLabel().equals("Deployments"));
        });

        add(tabs);
        configureModelGrid();
        configureDeploymentGrid();
        add(modelGrid, deploymentGrid);
        deploymentGrid.setVisible(false);
        refresh();
    }

    private ModelRegistryService registry() {
        ModelRegistryService svc = registryProvider.getIfAvailable();
        if (svc == null) throw new IllegalStateException("ModelRegistryService not available");
        return svc;
    }

    private EvaluationService evaluation() {
        EvaluationService svc = evaluationProvider.getIfAvailable();
        if (svc == null) throw new IllegalStateException("EvaluationService not available");
        return svc;
    }

    private DeploymentService deployment() {
        DeploymentService svc = deploymentProvider.getIfAvailable();
        if (svc == null) throw new IllegalStateException("DeploymentService not available");
        return svc;
    }

    private void configureModelGrid() {
        modelGrid.addColumn(ModelEntry::modelName).setHeader("Model").setAutoWidth(true);
        modelGrid.addColumn(ModelEntry::framework).setHeader("Framework").setAutoWidth(true);
        modelGrid.addColumn(ModelEntry::taskType).setHeader("Task").setAutoWidth(true);
        modelGrid.addColumn(ModelEntry::domainType).setHeader("Domain").setAutoWidth(true);
        modelGrid.addColumn(ModelEntry::latestLifecycleStage).setHeader("Stage").setAutoWidth(true);
        modelGrid.addColumn(e -> String.valueOf(e.latestVersionNumber())).setHeader("Versions").setAutoWidth(true);
        modelGrid.addComponentColumn(this::createModelActions).setHeader("Actions").setAutoWidth(true);
    }

    private void configureDeploymentGrid() {
        deploymentGrid.addColumn(DeploymentTarget::targetType).setHeader("Type").setAutoWidth(true);
        deploymentGrid.addColumn(DeploymentTarget::endpointUrl).setHeader("Endpoint").setAutoWidth(true);
        deploymentGrid.addColumn(d -> d.healthy() ? "Healthy" : "Unhealthy").setHeader("Health").setAutoWidth(true);
        deploymentGrid.addColumn(d -> d.active() ? "Active" : "Inactive").setHeader("Status").setAutoWidth(true);
        deploymentGrid.addColumn(d -> formatInstant(d.deployedAt())).setHeader("Deployed").setAutoWidth(true);
        deploymentGrid.addComponentColumn(this::createDeploymentActions).setHeader("Actions").setAutoWidth(true);
    }

    private HorizontalLayout createModelActions(ModelEntry entry) {
        HorizontalLayout actions = new HorizontalLayout();

        Button versions = new Button(VaadinIcon.LIST.create(), event -> showVersions(entry));
        versions.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        versions.setTooltipText("Versions");
        actions.add(versions);

        Button evaluate = new Button(VaadinIcon.CHART.create(), event -> openEvaluateDialog(entry));
        evaluate.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        evaluate.setTooltipText("Evaluate");
        actions.add(evaluate);

        return actions;
    }

    private HorizontalLayout createDeploymentActions(DeploymentTarget target) {
        HorizontalLayout actions = new HorizontalLayout();
        if (target.active()) {
            Button rollback = new Button(VaadinIcon.REFRESH.create(), event -> {
                deployment().rollback(context().activeTenantId(), target.deploymentTargetId(), context().username());
                refresh();
                Notification.show("Rolled back");
            });
            rollback.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            rollback.setTooltipText("Rollback");
            actions.add(rollback);
        }
        return actions;
    }

    private void showVersions(ModelEntry entry) {
        Dialog dialog = AdminDialogSupport.dialog("Versions: " + entry.modelName(), "700px");
        List<ModelVersion> versions = registry().listVersions(context().activeTenantId(), entry.modelEntryId());
        VerticalLayout content = new VerticalLayout();
        for (ModelVersion v : versions) {
            Span info = new Span("v" + v.versionNumber() + " - " + v.lifecycleStage()
                    + " - " + v.artifact().artifactUri());
            content.add(info);

            HorizontalLayout actions = new HorizontalLayout();
            if (v.lifecycleStage() == ModelLifecycleStage.DEVELOPMENT) {
                Button promote = new Button("Promote to Staging", event -> {
                    registry().promoteModel(context().activeTenantId(), v.modelVersionId(),
                            ModelLifecycleStage.STAGING, context().username());
                    dialog.close();
                    refresh();
                    Notification.show("Promoted to staging");
                });
                promote.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                actions.add(promote);
            } else if (v.lifecycleStage() == ModelLifecycleStage.STAGING) {
                Button promote = new Button("Promote to Production", event -> {
                    registry().promoteModel(context().activeTenantId(), v.modelVersionId(),
                            ModelLifecycleStage.PRODUCTION, context().username());
                    dialog.close();
                    refresh();
                    Notification.show("Promoted to production");
                });
                promote.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                actions.add(promote);
            }

            if (v.lifecycleStage() != ModelLifecycleStage.ARCHIVED) {
                Button eval = new Button("Evaluate", event -> {
                    evaluation().evaluate(context().activeTenantId(), v.modelVersionId(), null, context().username());
                    Notification.show("Evaluation completed");
                });
                actions.add(eval);
            }
            content.add(actions);
        }
        AdminDialogSupport.footer(dialog, "Close", dialog::close);
        dialog.add(AdminDialogSupport.body(content));
        dialog.open();
    }

    private void openEvaluateDialog(ModelEntry entry) {
        Dialog dialog = AdminDialogSupport.dialog("Evaluate: " + entry.modelName(), "600px");
        List<ModelVersion> versions = registry().listVersions(context().activeTenantId(), entry.modelEntryId());
        VerticalLayout content = new VerticalLayout();
        if (versions.isEmpty()) {
            content.add(new Span("No versions to evaluate"));
        }
        for (ModelVersion v : versions) {
            Span versionLabel = new Span("v" + v.versionNumber() + " (" + v.lifecycleStage() + ")");
            Button runEval = new Button("Run Evaluation", event -> {
                EvaluationRun result = evaluation().evaluate(
                        context().activeTenantId(), v.modelVersionId(), null, context().username());
                Notification.show("Evaluation: " + result.status()
                        + " (" + String.format("%.2f", result.metrics().stream()
                                .mapToDouble(m -> m.value()).average().orElse(0))
                        + " avg)");
                dialog.close();
            });
            runEval.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            HorizontalLayout row = new HorizontalLayout(versionLabel, runEval);
            content.add(row);
        }
        AdminDialogSupport.footer(dialog, "Close", dialog::close);
        dialog.add(AdminDialogSupport.body(content));
        dialog.open();
    }

    private void refresh() {
        List<ModelEntry> models = registry().listModels(context().activeTenantId());
        modelGrid.setItems(models);
        List<DeploymentTarget> deployments = deployment().getActiveDeployments(context().activeTenantId());
        deploymentGrid.setItems(deployments);
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
