package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.dataset.model.CurationRule;
import ai.datalithix.kanon.dataset.model.DatasetDefinition;
import ai.datalithix.kanon.dataset.model.DatasetVersion;
import ai.datalithix.kanon.dataset.model.ExportFormat;
import ai.datalithix.kanon.dataset.model.SplitStrategy;
import ai.datalithix.kanon.dataset.service.DatasetCurationService;
import ai.datalithix.kanon.dataset.service.DatasetRepository;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.ui.component.AdminDialogSupport;
import ai.datalithix.kanon.ui.component.HowItWorksSection;
import ai.datalithix.kanon.ui.i18n.I18n;
import ai.datalithix.kanon.ui.layout.MainLayout;
import ai.datalithix.kanon.ui.security.AdminSecuredView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;

@PageTitle("Datasets | Kanon Platform")
@Route(value = "admin/datasets", layout = MainLayout.class)
public class DatasetAdminView extends VerticalLayout implements AdminSecuredView {
    private final ObjectProvider<DatasetRepository> repositoryProvider;
    private final ObjectProvider<DatasetCurationService> curationServiceProvider;
    private final CurrentUserContextService currentUserContextService;
    private final Grid<DatasetDefinition> grid = new Grid<>(DatasetDefinition.class, false);

    public DatasetAdminView(
            ObjectProvider<DatasetRepository> repositoryProvider,
            ObjectProvider<DatasetCurationService> curationServiceProvider,
            CurrentUserContextService currentUserContextService
    ) {
        this.repositoryProvider = repositoryProvider;
        this.curationServiceProvider = curationServiceProvider;
        this.currentUserContextService = currentUserContextService;
        setSizeFull();
        setPadding(true);

        add(new HowItWorksSection(
                "Datasets aggregate annotated records into versioned training datasets.",
                List.of(
                        "Define dataset definitions with source annotations and split ratios.",
                        "Curate to produce versioned snapshots with train/val/test splits.",
                        "Export to JSONL, Parquet, Hugging Face, or TFRecord formats."
                )
        ));

        add(createToolbar());
        configureGrid();
        add(grid);
        refresh();
    }

    private DatasetRepository repository() {
        DatasetRepository repo = repositoryProvider.getIfAvailable();
        if (repo == null) throw new IllegalStateException("DatasetRepository not available");
        return repo;
    }

    private DatasetCurationService curationService() {
        DatasetCurationService svc = curationServiceProvider.getIfAvailable();
        if (svc == null) throw new IllegalStateException("DatasetCurationService not available");
        return svc;
    }

    private HorizontalLayout createToolbar() {
        Button createButton = new Button("Create Dataset", VaadinIcon.PLUS.create(), event -> openCreateDialog());
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout toolbar = new HorizontalLayout(createButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.END);
        return toolbar;
    }

    private void configureGrid() {
        grid.addColumn(DatasetDefinition::name).setHeader("Name").setAutoWidth(true);
        grid.addColumn(DatasetDefinition::domainType).setHeader("Domain").setAutoWidth(true);
        grid.addColumn(d -> String.valueOf(d.latestVersionNumber())).setHeader("Versions").setAutoWidth(true);
        grid.addColumn(d -> d.enabled() ? "Active" : "Disabled").setHeader("Status").setAutoWidth(true);
        grid.addComponentColumn(this::createActions).setHeader("Actions").setAutoWidth(true);
    }

    private HorizontalLayout createActions(DatasetDefinition def) {
        HorizontalLayout actions = new HorizontalLayout();
        Button curate = new Button(VaadinIcon.PLAY.create(), event -> curateDataset(def));
        curate.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        curate.setTooltipText("Curate");
        actions.add(curate);

        Button versions = new Button(VaadinIcon.LIST.create(), event -> showVersions(def));
        versions.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        versions.setTooltipText("Versions");
        actions.add(versions);

        return actions;
    }

    private void openCreateDialog() {
        Dialog dialog = AdminDialogSupport.dialog("Create Dataset", "600px");
        TextField name = AdminDialogSupport.requiredText("Name", "Dataset name");
        TextField description = new TextField("Description");
        description.setWidthFull();
        ComboBox<String> domainType = new ComboBox<>("Domain");
        domainType.setItems("ACCOUNTING", "HR", "AGRICULTURE", "MEDICAL", "LOGISTICS", "LEGAL", "CUSTOM");
        domainType.setValue("ACCOUNTING");
        NumberField trainRatio = new NumberField("Train %");
        trainRatio.setValue(0.8);
        NumberField valRatio = new NumberField("Validation %");
        valRatio.setValue(0.1);
        NumberField testRatio = new NumberField("Test %");
        testRatio.setValue(0.1);

        AdminDialogSupport.footer(dialog, "Create", () -> {
            if (!AdminDialogSupport.requireFilled(List.of(name))) return;
            String defId = "ds-" + UUID.randomUUID();
            AuditMetadata audit = new AuditMetadata(Instant.now(), context().username(),
                    Instant.now(), context().username(), 1);
            DatasetDefinition def = new DatasetDefinition(defId, context().activeTenantId(),
                    name.getValue(), description.getValue(), domainType.getValue(),
                    Set.of(), null, SplitStrategy.RANDOM, trainRatio.getValue(), valRatio.getValue(),
                    testRatio.getValue(), List.of(ExportFormat.JSONL), "EU", true, 0, audit);
            repository().save(def);
            dialog.close();
            refresh();
            Notification.show("Dataset created");
        });
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.form(name, description, domainType, trainRatio, valRatio, testRatio)));
        dialog.open();
    }

    private void curateDataset(DatasetDefinition def) {
        try {
            DatasetVersion version = curationService().curate(def, context().username());
            refresh();
            Notification.show("Dataset v" + version.versionNumber() + " curated");
        } catch (Exception e) {
            Notification.show("Curation failed: " + e.getMessage());
        }
    }

    private void showVersions(DatasetDefinition def) {
        Dialog dialog = AdminDialogSupport.dialog("Versions: " + def.name(), "800px");
        List<DatasetVersion> versions = repository().findVersionsByDefinitionId(
                context().activeTenantId(), def.datasetDefinitionId());
        VerticalLayout content = new VerticalLayout();
        for (DatasetVersion v : versions) {
            Span versionInfo = new Span("v" + v.versionNumber() + " - " + v.totalRecordCount()
                    + " records - " + v.exportStatus() + " - " + v.curatedAt());
            content.add(versionInfo);
            Button exportBtn = new Button("Export JSONL", event -> {
                curationService().export(v, List.of(ExportFormat.JSONL));
                Notification.show("Export started");
            });
            exportBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            content.add(exportBtn);
        }
        AdminDialogSupport.footer(dialog, "Close", dialog::close);
        dialog.add(AdminDialogSupport.body(content));
        dialog.open();
    }

    private void refresh() {
        List<DatasetDefinition> defs = repository().findByTenant(context().activeTenantId());
        grid.setItems(defs);
    }

    @Override
    public CurrentUserContext currentUserContext() {
        return currentUserContextService.currentUser();
    }

    private CurrentUserContext context() {
        return currentUserContext();
    }
}
