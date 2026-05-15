package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.dataset.service.DatasetRepository;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.training.model.ComputeBackend;
import ai.datalithix.kanon.training.model.ComputeBackendType;
import ai.datalithix.kanon.training.model.HyperParameterConfig;
import ai.datalithix.kanon.training.model.TrainingJob;
import ai.datalithix.kanon.training.model.TrainingJobStatus;
import ai.datalithix.kanon.training.service.TrainingJobRepository;
import ai.datalithix.kanon.training.service.TrainingOrchestrationService;
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

@PageTitle("Training Jobs | Kanon Platform")
@Route(value = "admin/training", layout = MainLayout.class)
public class TrainingJobAdminView extends VerticalLayout implements AdminSecuredView {
    private final ObjectProvider<TrainingOrchestrationService> orchestrationProvider;
    private final ObjectProvider<TrainingJobRepository> jobRepositoryProvider;
    private final ObjectProvider<DatasetRepository> datasetRepositoryProvider;
    private final CurrentUserContextService currentUserContextService;
    private final Grid<TrainingJob> grid = new Grid<>(TrainingJob.class, false);

    public TrainingJobAdminView(
            ObjectProvider<TrainingOrchestrationService> orchestrationProvider,
            ObjectProvider<TrainingJobRepository> jobRepositoryProvider,
            ObjectProvider<DatasetRepository> datasetRepositoryProvider,
            CurrentUserContextService currentUserContextService
    ) {
        this.orchestrationProvider = orchestrationProvider;
        this.jobRepositoryProvider = jobRepositoryProvider;
        this.datasetRepositoryProvider = datasetRepositoryProvider;
        this.currentUserContextService = currentUserContextService;
        setSizeFull();
        setPadding(true);

        add(new HowItWorksSection(
                "Training jobs orchestrate model training on configurable compute backends.",
                List.of(
                        "Select a curated dataset version and configure hyperparameters.",
                        "Submit to a local GPU, Kubernetes, or cloud ML compute backend.",
                        "Monitor job progress, metrics, and checkpoints in real time."
                )
        ));

        add(createToolbar());
        configureGrid();
        add(grid);
        refresh();
    }

    private TrainingOrchestrationService orchestration() {
        TrainingOrchestrationService svc = orchestrationProvider.getIfAvailable();
        if (svc == null) throw new IllegalStateException("TrainingOrchestrationService not available");
        return svc;
    }

    private TrainingJobRepository jobRepository() {
        TrainingJobRepository repo = jobRepositoryProvider.getIfAvailable();
        if (repo == null) throw new IllegalStateException("TrainingJobRepository not available");
        return repo;
    }

    private HorizontalLayout createToolbar() {
        Button submitButton = new Button("Submit Training Job", VaadinIcon.PLUS.create(), event -> openSubmitDialog());
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout toolbar = new HorizontalLayout(submitButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.END);
        return toolbar;
    }

    private void configureGrid() {
        grid.addColumn(TrainingJob::modelName).setHeader("Model").setAutoWidth(true);
        grid.addColumn(job -> job.status().name()).setHeader("Status").setAutoWidth(true);
        grid.addColumn(job -> job.datasetVersionId()).setHeader("Dataset").setAutoWidth(true);
        grid.addColumn(job -> job.computeBackendId()).setHeader("Backend").setAutoWidth(true);
        grid.addColumn(job -> formatInstant(job.requestedAt())).setHeader("Requested").setAutoWidth(true);
        grid.addComponentColumn(this::createActions).setHeader("Actions").setAutoWidth(true);
    }

    private HorizontalLayout createActions(TrainingJob job) {
        HorizontalLayout actions = new HorizontalLayout();
        if (job.status() == TrainingJobStatus.QUEUED || job.status() == TrainingJobStatus.RUNNING) {
            Button cancelBtn = new Button(VaadinIcon.STOP.create(), event -> {
                orchestration().cancelJob(context().activeTenantId(), job.trainingJobId());
                refresh();
                Notification.show("Job cancelled");
            });
            cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            cancelBtn.setTooltipText("Cancel");
            actions.add(cancelBtn);
        }
        return actions;
    }

    private void openSubmitDialog() {
        Dialog dialog = AdminDialogSupport.dialog("Submit Training Job", "700px");

        TextField modelName = AdminDialogSupport.requiredText("Model Name", "e.g. bert-finetuned-v1");
        ComboBox<String> datasetVersion = new ComboBox<>("Dataset Version");
        datasetVersion.setWidthFull();
        var datasetRepo = datasetRepositoryProvider.getIfAvailable();
        if (datasetRepo != null) {
            var versionIds = datasetRepo.findByTenant(context().activeTenantId()).stream()
                    .flatMap(def -> datasetRepo.findVersionsByDefinitionId(context().activeTenantId(), def.datasetDefinitionId()).stream())
                    .map(v -> v.datasetVersionId())
                    .toList();
            datasetVersion.setItems(versionIds);
            datasetVersion.setItemLabelGenerator(v -> {
                var dv = datasetRepo.findVersionById(context().activeTenantId(), v);
                return dv.map(d -> d.datasetVersionId().substring(0, 8) + "... (v" + d.versionNumber() + ")").orElse(v);
            });
        }

        ComboBox<String> computeBackend = new ComboBox<>("Compute Backend");
        computeBackend.setWidthFull();
        var backends = jobRepository().findEnabledBackends(context().activeTenantId());
        computeBackend.setItems(backends.stream().map(ComputeBackend::name).toList());

        TextField framework = new TextField("Framework");
        framework.setValue("pytorch");
        framework.setWidthFull();

        TextField modelArchitecture = new TextField("Architecture");
        modelArchitecture.setValue("bert-base");
        modelArchitecture.setWidthFull();

        IntegerField epochs = new IntegerField("Epochs");
        epochs.setValue(3);
        IntegerField batchSize = new IntegerField("Batch Size");
        batchSize.setValue(16);
        NumberField learningRate = new NumberField("Learning Rate");
        learningRate.setValue(0.001);

        AdminDialogSupport.footer(dialog, "Submit", () -> {
            if (!AdminDialogSupport.requireFilled(List.of(modelName))) return;
            try {
                HyperParameterConfig hyperParams = new HyperParameterConfig(
                        framework.getValue(), modelArchitecture.getValue(),
                        epochs.getValue(), batchSize.getValue(), learningRate.getValue(), Map.of());
                orchestration().submitJob(
                        context().activeTenantId(),
                        datasetVersion.getValue(),
                        null,
                        computeBackend.getValue(),
                        modelName.getValue(),
                        hyperParams,
                        context().username()
                );
                dialog.close();
                refresh();
                Notification.show("Training job submitted");
            } catch (Exception e) {
                Notification.show("Submission failed: " + e.getMessage());
            }
        });

        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.form(modelName, datasetVersion, computeBackend,
                        framework, modelArchitecture, epochs, batchSize, learningRate)));
        dialog.open();
    }

    private void refresh() {
        List<TrainingJob> jobs = jobRepository().findByTenant(context().activeTenantId());
        grid.setItems(jobs);
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
