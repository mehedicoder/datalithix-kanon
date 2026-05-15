package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.airouting.model.ChatModelRoute;
import ai.datalithix.kanon.airouting.model.ModelExecutionPolicy;
import ai.datalithix.kanon.airouting.model.ModelInvocationRequest;
import ai.datalithix.kanon.airouting.model.ModelInvocationResult;
import ai.datalithix.kanon.airouting.model.ModelInvocationStatus;
import ai.datalithix.kanon.airouting.model.ModelProfile;
import ai.datalithix.kanon.airouting.service.ModelInvocationService;
import ai.datalithix.kanon.airouting.service.ModelProfileRepository;
import ai.datalithix.kanon.airouting.service.ModelRouter;
import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.TenantContext;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.common.model.PageSpec;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.model.SortDirection;
import ai.datalithix.kanon.common.runtime.ExecutionControls;
import ai.datalithix.kanon.common.runtime.RetryPolicy;
import ai.datalithix.kanon.common.security.AccessControlContext;
import ai.datalithix.kanon.common.security.AccessPurpose;
import ai.datalithix.kanon.domain.model.TaskDescriptor;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.ui.component.AdminDialogSupport;
import ai.datalithix.kanon.ui.component.HowItWorksSection;
import ai.datalithix.kanon.ui.component.RedactedText;
import ai.datalithix.kanon.ui.i18n.I18n;
import ai.datalithix.kanon.ui.layout.MainLayout;
import ai.datalithix.kanon.ui.security.AdminAccess;
import ai.datalithix.kanon.ui.security.AdminSecuredView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@PageTitle("Models | Kanon Platform")
@Route(value = "admin/models", layout = MainLayout.class)
public class ModelAdminView extends VerticalLayout implements AdminSecuredView {
    private final ObjectProvider<ModelProfileRepository> repositoryProvider;
    private final ObjectProvider<ModelInvocationService> invocationServiceProvider;
    private final ObjectProvider<ModelRouter> modelRouterProvider;
    private final CurrentUserContextService currentUserContextService;
    private final Grid<ModelProfile> grid = new Grid<>(ModelProfile.class, false);
    private final TextField searchField = new TextField();
    private final ComboBox<String> backendTypeFilter = new ComboBox<>();
    private final ComboBox<String> providerFilter = new ComboBox<>();
    private final Checkbox showDisabledCheckbox = new Checkbox();

    public ModelAdminView(
            ObjectProvider<ModelProfileRepository> repositoryProvider,
            ObjectProvider<ModelInvocationService> invocationServiceProvider,
            ObjectProvider<ModelRouter> modelRouterProvider,
            CurrentUserContextService currentUserContextService
    ) {
        this.repositoryProvider = repositoryProvider;
        this.invocationServiceProvider = invocationServiceProvider;
        this.modelRouterProvider = modelRouterProvider;
        this.currentUserContextService = currentUserContextService;
        setSizeFull();
        setPadding(true);
        
        add(new HowItWorksSection(
                I18n.t("admin.model.overview.summary"),
                List.of(
                        I18n.t("admin.model.overview.backend-types"),
                        I18n.t("admin.model.overview.routing"),
                        I18n.t("admin.model.overview.health")
                )
        ));

        add(createToolbar());
        configureGrid();
        add(grid);
        refresh();
    }
    
    private ModelProfileRepository repository() {
        ModelProfileRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new IllegalStateException("ModelProfileRepository is not available");
        }
        return repository;
    }

    private HorizontalLayout createToolbar() {
        searchField.setPlaceholder(I18n.t("action.search"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.addValueChangeListener(event -> refresh());

        backendTypeFilter.setPlaceholder(I18n.t("field.backend-type"));
        backendTypeFilter.setItems("LOCAL_SERVER", "API");
        backendTypeFilter.setClearButtonVisible(true);
        backendTypeFilter.addValueChangeListener(event -> refresh());

        providerFilter.setPlaceholder(I18n.t("field.provider"));
        providerFilter.setItems("OpenAI", "Anthropic", "Ollama", "Azure", "Custom");
        providerFilter.setClearButtonVisible(true);
        providerFilter.addValueChangeListener(event -> refresh());

        showDisabledCheckbox.setLabel(I18n.t("action.show-disabled"));
        showDisabledCheckbox.addValueChangeListener(event -> refresh());

        HorizontalLayout filters = new HorizontalLayout(searchField, backendTypeFilter, providerFilter, showDisabledCheckbox);
        filters.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        HorizontalLayout actions = new HorizontalLayout();
        if (AdminAccess.canConfigureModels(context())) {
            Button createButton = new Button(I18n.t("admin.model.create"), VaadinIcon.PLUS.create(), event -> openCreateDialog());
            createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            actions.add(createButton);
        }

        HorizontalLayout layout = new HorizontalLayout(filters, actions);
        layout.setWidthFull();
        layout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        return layout;
    }

    private void configureGrid() {
        grid.addComponentColumn(model -> {
            Icon statusIcon = VaadinIcon.CIRCLE.create();
            String healthStatus = model.healthStatus();
            if (model.enabled() && "HEALTHY".equals(healthStatus)) {
                statusIcon.setColor("var(--lumo-success-color)");
                statusIcon.setTooltipText(I18n.t("state.healthy"));
            } else if (model.enabled() && "DEGRADED".equals(healthStatus)) {
                statusIcon.setColor("var(--lumo-warning-color)");
                statusIcon.setTooltipText(I18n.t("state.degraded"));
            } else if (model.enabled()) {
                statusIcon.setColor("var(--lumo-error-color)");
                statusIcon.setTooltipText(I18n.t("state.unhealthy"));
            } else {
                statusIcon.setColor("var(--lumo-disabled-text-color)");
                statusIcon.setTooltipText(I18n.t("state.disabled"));
            }
            return statusIcon;
        }).setHeader("").setWidth("60px").setFlexGrow(0);

        grid.addComponentColumn(model -> {
            Icon typeIcon = model.local() ? VaadinIcon.SERVER.create() : VaadinIcon.CLOUD.create();
            typeIcon.setTooltipText(model.local() ? I18n.t("model.type.local") : I18n.t("model.type.api"));
            return typeIcon;
        }).setHeader("").setWidth("60px").setFlexGrow(0);

        grid.addColumn(ModelProfile::modelName).setHeader(I18n.t("grid.model")).setAutoWidth(true);
        grid.addColumn(ModelProfile::provider).setHeader(I18n.t("grid.provider")).setAutoWidth(true);
        grid.addColumn(ModelProfile::backendType).setHeader(I18n.t("grid.backend")).setAutoWidth(true);
        grid.addColumn(ModelProfile::costClass).setHeader(I18n.t("grid.cost-class")).setAutoWidth(true);
        grid.addColumn(ModelProfile::latencyClass).setHeader(I18n.t("grid.latency-class")).setAutoWidth(true);
        grid.addColumn(model -> formatInstant(model.audit().updatedAt())).setHeader(I18n.t("grid.updated")).setAutoWidth(true);

        grid.addComponentColumn(this::createModelActions).setHeader(I18n.t("grid.actions")).setAutoWidth(true);
    }

    HorizontalLayout createModelActions(ModelProfile model) {
        HorizontalLayout actions = new HorizontalLayout();

        if (AdminAccess.canConfigureModels(context())) {
            Button edit = new Button(VaadinIcon.EDIT.create(), event -> openEditDialog(model));
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            edit.setTooltipText(I18n.t("action.edit"));
            actions.add(edit);

            Button toggle = new Button(VaadinIcon.POWER_OFF.create(), event -> toggleModelStatus(model));
            toggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            toggle.setTooltipText(model.enabled() ? I18n.t("action.disable") : I18n.t("action.enable"));
            actions.add(toggle);
        }

        if (AdminAccess.canTestModels(context())) {
            Button test = new Button(VaadinIcon.CONNECT.create(), event -> testConnection(model));
            test.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            test.setTooltipText(I18n.t("admin.model.action.test-connection"));
            actions.add(test);

            Button dryRun = new Button(VaadinIcon.SPLIT.create(), event -> dryRunRouting(model));
            dryRun.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            dryRun.setTooltipText(I18n.t("admin.model.action.dry-run"));
            actions.add(dryRun);
        }

        if (AdminAccess.canConfigureModels(context())) {
            Button delete = new Button(VaadinIcon.TRASH.create(), event -> deleteModel(model));
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            delete.setTooltipText(I18n.t("action.delete"));
            actions.add(delete);
        }

        if (actions.getComponentCount() == 0) {
            actions.add(new Span(I18n.t("state.read-only")));
        }
        return actions;
    }

    private void openCreateDialog() {
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.model.dialog.create"), "920px");
        
        TextField profileKey = AdminDialogSupport.requiredText(I18n.t("field.profile-key"), I18n.t("admin.model.profile-key.helper"));
        TextField modelName = AdminDialogSupport.requiredText(I18n.t("field.model-name"), I18n.t("admin.model.model-name.helper"));
        ComboBox<String> backendType = new ComboBox<>(I18n.t("field.backend-type"));
        backendType.setRequiredIndicatorVisible(true);
        backendType.setItems("LOCAL_SERVER", "API");
        backendType.setValue("LOCAL_SERVER");
        
        ComboBox<String> provider = new ComboBox<>(I18n.t("field.provider"));
        provider.setRequiredIndicatorVisible(true);
        provider.setItems("OpenAI", "Anthropic", "Ollama", "Azure", "Custom");
        provider.setValue("OpenAI");
        
        TextField modelId = AdminDialogSupport.requiredText(I18n.t("field.model-id"), I18n.t("admin.model.model-id.helper"));
        TextField baseUrl = new TextField(I18n.t("field.base-url"));
        baseUrl.setPlaceholder("http://localhost:11434");
        baseUrl.setWidthFull();
        
        TextField secretRef = new TextField(I18n.t("field.secret-ref"));
        secretRef.setPlaceholder(I18n.t("admin.model.secret-ref.placeholder"));
        secretRef.setHelperText(I18n.t("admin.model.secret-ref.helper"));
        secretRef.setWidthFull();
        
        MultiSelectComboBox<AiTaskType> taskCapabilities = new MultiSelectComboBox<>(I18n.t("field.task-capabilities"));
        taskCapabilities.setItems(AiTaskType.values());
        taskCapabilities.setWidthFull();
        
        ComboBox<String> costClass = new ComboBox<>(I18n.t("field.cost-class"));
        costClass.setItems("FREE", "LOW", "MEDIUM", "HIGH", "PREMIUM");
        costClass.setValue("MEDIUM");
        
        ComboBox<String> latencyClass = new ComboBox<>(I18n.t("field.latency-class"));
        latencyClass.setItems("REALTIME", "FAST", "NORMAL", "SLOW", "BATCH");
        latencyClass.setValue("NORMAL");
        
        IntegerField priority = new IntegerField(I18n.t("field.priority"));
        priority.setValue(100);
        priority.setHelperText(I18n.t("admin.model.priority.helper"));
        
        Checkbox supportsTools = new Checkbox(I18n.t("field.supports-tools"));
        Checkbox supportsStructuredOutput = new Checkbox(I18n.t("field.supports-structured-output"));
        
        TextField fallbackProfileKey = new TextField(I18n.t("field.fallback-profile"));
        fallbackProfileKey.setPlaceholder(I18n.t("admin.model.fallback.placeholder"));
        fallbackProfileKey.setWidthFull();

        AdminDialogSupport.footer(dialog, I18n.t("action.create"), () -> {
            if (!AdminDialogSupport.requireFilled(List.of(profileKey, modelName, modelId))) {
                return;
            }
            if (backendType.getValue() == null || provider.getValue() == null) {
                Notification.show(I18n.t("admin.model.validation.backend-provider-required"));
                return;
            }
            try {
                ModelProfile model = new ModelProfile(
                        profileKey.getValue(),
                        provider.getValue(),
                        backendType.getValue(),
                        modelId.getValue(),
                        modelName.getValue(),
                        emptyToNull(baseUrl.getValue()),
                        "LOCAL_SERVER".equals(backendType.getValue()),
                        supportsTools.getValue(),
                        supportsStructuredOutput.getValue(),
                        taskCapabilities.getSelectedItems(),
                        costClass.getValue(),
                        latencyClass.getValue(),
                        "LOCAL_SERVER".equals(backendType.getValue()) ? "LOCAL" : "CLOUD",
                        Set.of(),
                        true,
                        "UNKNOWN",
                        emptyToNull(secretRef.getValue()),
                        priority.getValue(),
                        createDefaultExecutionPolicy(emptyToNull(fallbackProfileKey.getValue())),
                        auditNow(0)
                );
                repository().save(model);
                dialog.close();
                refresh();
                Notification.show(I18n.t("admin.model.created"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
        
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.sectionTitle(I18n.t("section.identity")),
                AdminDialogSupport.help(I18n.t("admin.model.create.help")),
                AdminDialogSupport.form(profileKey, modelName, backendType, provider, modelId),
                AdminDialogSupport.sectionTitle(I18n.t("section.connection")),
                AdminDialogSupport.form(baseUrl, secretRef),
                AdminDialogSupport.sectionTitle(I18n.t("section.capabilities")),
                AdminDialogSupport.form(taskCapabilities, supportsTools, supportsStructuredOutput),
                AdminDialogSupport.sectionTitle(I18n.t("section.routing")),
                AdminDialogSupport.form(costClass, latencyClass, priority, fallbackProfileKey)
        ));
        dialog.open();
    }

    private void openEditDialog(ModelProfile model) {
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.model.dialog.update"), "920px");
        
        TextField profileKey = AdminDialogSupport.requiredText(I18n.t("field.profile-key"), I18n.t("admin.model.profile-key.helper"));
        profileKey.setValue(model.profileKey());
        profileKey.setReadOnly(true);
        
        TextField modelName = AdminDialogSupport.requiredText(I18n.t("field.model-name"), I18n.t("admin.model.model-name.helper"));
        modelName.setValue(model.modelName());
        
        ComboBox<String> backendType = new ComboBox<>(I18n.t("field.backend-type"));
        backendType.setItems("LOCAL_SERVER", "API");
        backendType.setValue(model.backendType());
        backendType.setReadOnly(true);
        
        ComboBox<String> provider = new ComboBox<>(I18n.t("field.provider"));
        provider.setItems("OpenAI", "Anthropic", "Ollama", "Azure", "Custom");
        provider.setValue(model.provider());
        
        TextField modelId = AdminDialogSupport.requiredText(I18n.t("field.model-id"), I18n.t("admin.model.model-id.helper"));
        modelId.setValue(model.modelId());
        
        TextField baseUrl = new TextField(I18n.t("field.base-url"));
        baseUrl.setValue(nullToEmpty(model.baseUrl()));
        baseUrl.setWidthFull();
        
        // Secret reference field with redaction support
        VerticalLayout secretSection = new VerticalLayout();
        secretSection.setPadding(false);
        secretSection.setSpacing(false);
        
        if (model.secretRef() != null && !AdminAccess.canViewModelSecrets(context())) {
            secretSection.add(RedactedText.secret(I18n.t("field.secret-ref"), model.secretRef()));
        } else {
            TextField secretRef = new TextField(I18n.t("field.secret-ref"));
            secretRef.setValue(nullToEmpty(model.secretRef()));
            secretRef.setPlaceholder(I18n.t("admin.model.secret-ref.placeholder"));
            secretRef.setHelperText(I18n.t("admin.model.secret-ref.helper"));
            secretRef.setWidthFull();
            secretSection.add(secretRef);
        }
        
        MultiSelectComboBox<AiTaskType> taskCapabilities = new MultiSelectComboBox<>(I18n.t("field.task-capabilities"));
        taskCapabilities.setItems(AiTaskType.values());
        taskCapabilities.select(model.taskCapabilities());
        taskCapabilities.setWidthFull();
        
        ComboBox<String> costClass = new ComboBox<>(I18n.t("field.cost-class"));
        costClass.setItems("FREE", "LOW", "MEDIUM", "HIGH", "PREMIUM");
        costClass.setValue(model.costClass());
        
        ComboBox<String> latencyClass = new ComboBox<>(I18n.t("field.latency-class"));
        latencyClass.setItems("REALTIME", "FAST", "NORMAL", "SLOW", "BATCH");
        latencyClass.setValue(model.latencyClass());
        
        IntegerField priority = new IntegerField(I18n.t("field.priority"));
        priority.setValue(model.priority());
        
        Checkbox supportsTools = new Checkbox(I18n.t("field.supports-tools"));
        supportsTools.setValue(model.supportsTools());
        
        Checkbox supportsStructuredOutput = new Checkbox(I18n.t("field.supports-structured-output"));
        supportsStructuredOutput.setValue(model.supportsStructuredOutput());
        
        TextField fallbackProfileKey = new TextField(I18n.t("field.fallback-profile"));
        fallbackProfileKey.setValue(nullToEmpty(model.executionPolicy().fallbackProfileKey()));
        fallbackProfileKey.setWidthFull();

        AdminDialogSupport.footer(dialog, I18n.t("action.update"), () -> {
            if (!AdminDialogSupport.requireFilled(List.of(modelName, modelId))) {
                return;
            }
            if (provider.getValue() == null) {
                Notification.show(I18n.t("admin.model.validation.provider-required"));
                return;
            }
            try {
                // Get secret ref value - either from the editable field or keep existing
                String newSecretRef = model.secretRef();
                if (AdminAccess.canViewModelSecrets(context())) {
                    TextField secretRefField = (TextField) secretSection.getComponentAt(0);
                    newSecretRef = emptyToNull(secretRefField.getValue());
                }
                
                ModelProfile updated = new ModelProfile(
                        model.profileKey(),
                        provider.getValue(),
                        model.backendType(),
                        modelId.getValue(),
                        modelName.getValue(),
                        emptyToNull(baseUrl.getValue()),
                        model.local(),
                        supportsTools.getValue(),
                        supportsStructuredOutput.getValue(),
                        taskCapabilities.getSelectedItems(),
                        costClass.getValue(),
                        latencyClass.getValue(),
                        model.locality(),
                        model.complianceTags(),
                        model.enabled(),
                        model.healthStatus(),
                        newSecretRef,
                        priority.getValue(),
                        createDefaultExecutionPolicy(emptyToNull(fallbackProfileKey.getValue())),
                        updatedAudit(model.audit())
                );
                repository().save(updated);
                dialog.close();
                refresh();
                Notification.show(I18n.t("admin.model.updated"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
        
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.sectionTitle(I18n.t("section.identity")),
                AdminDialogSupport.help(I18n.t("admin.model.update.help")),
                AdminDialogSupport.form(profileKey, modelName, backendType, provider, modelId),
                AdminDialogSupport.sectionTitle(I18n.t("section.connection")),
                baseUrl,
                secretSection,
                AdminDialogSupport.sectionTitle(I18n.t("section.capabilities")),
                AdminDialogSupport.form(taskCapabilities, supportsTools, supportsStructuredOutput),
                AdminDialogSupport.sectionTitle(I18n.t("section.routing")),
                AdminDialogSupport.form(costClass, latencyClass, priority, fallbackProfileKey)
        ));
        dialog.open();
    }

    private void toggleModelStatus(ModelProfile model) {
        try {
            ModelProfile updated = new ModelProfile(
                    model.profileKey(),
                    model.provider(),
                    model.backendType(),
                    model.modelId(),
                    model.modelName(),
                    model.baseUrl(),
                    model.local(),
                    model.supportsTools(),
                    model.supportsStructuredOutput(),
                    model.taskCapabilities(),
                    model.costClass(),
                    model.latencyClass(),
                    model.locality(),
                    model.complianceTags(),
                    !model.enabled(),
                    model.healthStatus(),
                    model.secretRef(),
                    model.priority(),
                    model.executionPolicy(),
                    updatedAudit(model.audit())
            );
            repository().save(updated);
            refresh();
            Notification.show(updated.enabled() ? I18n.t("admin.model.enabled") : I18n.t("admin.model.disabled"));
        } catch (RuntimeException exception) {
            Notification.show(exception.getMessage());
        }
    }

    private void testConnection(ModelProfile model) {
        ModelInvocationService invocationService = invocationServiceProvider.getIfAvailable();
        if (invocationService == null) {
            Notification.show(I18n.t("admin.model.test-connection.service-unavailable"), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.model.dialog.test-connection"), "720px");
        
        Span statusSpan = new Span(I18n.t("admin.model.test-connection.running", model.modelName()));
        statusSpan.getStyle().set("font-weight", "bold");
        
        Span resultSpan = new Span();
        resultSpan.getStyle().set("white-space", "pre-wrap");
        resultSpan.getStyle().set("font-family", "monospace");
        resultSpan.getStyle().set("padding", "var(--lumo-space-m)");
        resultSpan.getStyle().set("background", "var(--lumo-contrast-5pct)");
        resultSpan.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        resultSpan.setVisible(false);
        
        Button closeButton = new Button(I18n.t("action.close"), event -> dialog.close());
        closeButton.setEnabled(false);
        
        HorizontalLayout footer = new HorizontalLayout(closeButton);
        footer.setJustifyContentMode(JustifyContentMode.END);
        footer.setWidthFull();
        
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.help(I18n.t("admin.model.test-connection.help")),
                statusSpan,
                resultSpan,
                footer
        ));
        dialog.open();
        
        CurrentUserContext currentContext = context();
        ModelInvocationRequest request = testConnectionRequest(model, currentContext);

        // Run test asynchronously without pinning platform threads.
        dialog.getUI().ifPresent(ui -> {
            Thread.ofVirtual().name("model-test-connection-" + model.profileKey()).start(() -> {
                try {
                    ModelInvocationResult result = invocationService.invoke(request);
                    
                    ui.access(() -> {
                        if (result.status() == ModelInvocationStatus.COMPLETED) {
                            statusSpan.setText(I18n.t("admin.model.test-connection.success", model.modelName()));
                            statusSpan.getStyle().set("color", "var(--lumo-success-color)");
                            
                            StringBuilder resultText = new StringBuilder();
                            resultText.append("Status: ").append(result.status()).append("\n");
                            resultText.append("Latency: ").append(result.latency().toMillis()).append("ms\n");
                            resultText.append("Response: ").append(result.outputRef() != null ? result.outputRef() : "N/A").append("\n");
                            if (!result.metadata().isEmpty()) {
                                resultText.append("\nMetadata:\n");
                                result.metadata().forEach((key, value) ->
                                    resultText.append("  ").append(key).append(": ").append(value).append("\n")
                                );
                            }
                            resultSpan.setText(resultText.toString());
                            resultSpan.setVisible(true);
                        } else {
                            statusSpan.setText(I18n.t("admin.model.test-connection.failed", model.modelName(), result.failureReason()));
                            statusSpan.getStyle().set("color", "var(--lumo-error-color)");
                            
                            String errorText = "Status: " + result.status() + "\n" +
                                             "Reason: " + (result.failureReason() != null ? result.failureReason() : "Unknown") + "\n" +
                                             "Latency: " + result.latency().toMillis() + "ms";
                            resultSpan.setText(errorText);
                            resultSpan.setVisible(true);
                        }
                        closeButton.setEnabled(true);
                    });
                } catch (Exception e) {
                    ui.access(() -> {
                        statusSpan.setText(I18n.t("admin.model.test-connection.error"));
                        statusSpan.getStyle().set("color", "var(--lumo-error-color)");
                        resultSpan.setText("Error: " + e.getMessage());
                        resultSpan.setVisible(true);
                        closeButton.setEnabled(true);
                    });
                }
            });
        });
    }

    ModelInvocationRequest testConnectionRequest(ModelProfile model, CurrentUserContext currentContext) {
        AccessControlContext accessContext = new AccessControlContext(
                currentContext.activeTenantId(),
                currentContext.userId(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                AccessPurpose.MODEL_INVOCATION
        );

        return new ModelInvocationRequest(
                "test-" + UUID.randomUUID(),
                currentContext.activeTenantId(),
                "test-case",
                model.profileKey(),
                AiTaskType.REASONING,
                null,
                null,
                Map.of("prompt", "Hello! Please respond with 'Connection successful' to confirm you are working."),
                "test-connection",
                Instant.now(),
                accessContext
        );
    }

    private void dryRunRouting(ModelProfile model) {
        ModelRouter router = modelRouterProvider.getIfAvailable();
        if (router == null) {
            Notification.show(I18n.t("admin.model.dry-run.service-unavailable"), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.model.dialog.dry-run"), "820px");
        
        ComboBox<AiTaskType> taskType = new ComboBox<>(I18n.t("field.task-type"));
        taskType.setItems(AiTaskType.values());
        taskType.setValue(AiTaskType.REASONING);
        taskType.setWidthFull();
        
        TextField tenantId = new TextField(I18n.t("field.tenant"));
        tenantId.setValue(context().activeTenantId());
        tenantId.setReadOnly(true);
        tenantId.setWidthFull();
        
        Checkbox preferLocal = new Checkbox(I18n.t("field.prefer-local-models"));
        preferLocal.setValue(false);
        
        Checkbox highRisk = new Checkbox(I18n.t("field.high-risk-task"));
        highRisk.setValue(false);
        
        Span result = new Span();
        result.getStyle().set("white-space", "pre-wrap");
        result.getStyle().set("font-family", "monospace");
        result.getStyle().set("padding", "var(--lumo-space-m)");
        result.getStyle().set("background", "var(--lumo-contrast-5pct)");
        result.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        result.setVisible(false);
        
        Button runButton = new Button(I18n.t("admin.model.action.run-dry-run"), event -> {
            try {
                TenantContext tenantContext = new TenantContext(
                        context().activeTenantId(),
                        null,              // domainType
                        "US",              // countryCode
                        null,              // regulatoryAct
                        true,              // allowCloudModels
                        preferLocal.getValue(), // preferLocalModels
                        Set.of()           // enabledPolicies
                );
                
                TaskDescriptor taskDescriptor = new TaskDescriptor(
                        taskType.getValue(),
                        "dry-run-case",
                        "dry-run-input",
                        "1.0",
                        highRisk.getValue()
                );
                
                ChatModelRoute route = router.resolve(tenantContext, taskDescriptor);

                result.setText(dryRunResult(model, tenantContext, taskDescriptor, route));
                result.setVisible(true);
                
            } catch (Exception e) {
                result.setText("Error during routing: " + e.getMessage());
                result.getStyle().set("color", "var(--lumo-error-color)");
                result.setVisible(true);
            }
        });
        runButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button closeButton = new Button(I18n.t("action.close"), event -> dialog.close());
        
        HorizontalLayout footer = new HorizontalLayout(runButton, closeButton);
        footer.setJustifyContentMode(JustifyContentMode.END);
        footer.setWidthFull();
        
        dialog.add(AdminDialogSupport.body(
                AdminDialogSupport.help(I18n.t("admin.model.dry-run.help")),
                AdminDialogSupport.form(taskType, tenantId, preferLocal, highRisk),
                AdminDialogSupport.sectionTitle(I18n.t("section.routing-result")),
                result,
                footer
        ));
        dialog.open();
    }

    String dryRunResult(ModelProfile model, TenantContext tenantContext, TaskDescriptor taskDescriptor, ChatModelRoute route) {
        ModelProfile primaryModel = route.primaryProfileKey() == null
                ? null
                : repository().findByProfileKey(tenantContext.tenantId(), route.primaryProfileKey()).orElse(null);
        ModelProfile fallbackModel = route.fallbackProfileKey() == null
                ? null
                : repository().findByProfileKey(tenantContext.tenantId(), route.fallbackProfileKey()).orElse(null);

        StringBuilder resultText = new StringBuilder();
        resultText.append("=== Routing Decision ===\n\n");
        resultText.append("Task Type: ").append(taskDescriptor.taskType()).append("\n");
        resultText.append("Tenant: ").append(tenantContext.tenantId()).append("\n");
        resultText.append("Prefer Local: ").append(tenantContext.preferLocalModels()).append("\n");
        resultText.append("High Risk: ").append(taskDescriptor.highRisk()).append("\n\n");

        resultText.append("=== Selected Route ===\n\n");
        resultText.append("Primary Profile: ").append(route.primaryProfileKey()).append("\n");
        appendModelDetails(resultText, primaryModel, true);

        if (route.fallbackProfileKey() != null) {
            resultText.append("\nFallback Profile: ").append(route.fallbackProfileKey()).append("\n");
            appendModelDetails(resultText, fallbackModel, false);
        }

        resultText.append("\nReason: ").append(route.reason()).append("\n");

        boolean isSelectedPrimary = model.profileKey().equals(route.primaryProfileKey());
        boolean isSelectedFallback = route.fallbackProfileKey() != null
                && model.profileKey().equals(route.fallbackProfileKey());

        if (isSelectedPrimary) {
            resultText.append("\nThis model is the PRIMARY choice for this routing scenario");
        } else if (isSelectedFallback) {
            resultText.append("\nThis model is the FALLBACK choice for this routing scenario");
        } else {
            resultText.append("\nThis model is NOT selected for this routing scenario");
        }
        return resultText.toString();
    }

    private static void appendModelDetails(StringBuilder resultText, ModelProfile selectedModel, boolean includePolicyDetails) {
        if (selectedModel == null) {
            return;
        }
        resultText.append("  Model: ").append(selectedModel.modelName()).append("\n");
        resultText.append("  Provider: ").append(selectedModel.provider()).append("\n");
        resultText.append("  Backend: ").append(selectedModel.backendType()).append("\n");
        if (includePolicyDetails) {
            resultText.append("  Cost Class: ").append(selectedModel.costClass()).append("\n");
            resultText.append("  Latency Class: ").append(selectedModel.latencyClass()).append("\n");
            resultText.append("  Local: ").append(selectedModel.local()).append("\n");
            resultText.append("  Enabled: ").append(selectedModel.enabled()).append("\n");
            resultText.append("  Health: ").append(selectedModel.healthStatus()).append("\n");
        }
    }

    private void deleteModel(ModelProfile model) {
        AdminDialogSupport.confirmDeletion(I18n.t("admin.model.dialog.delete"), model.modelName(), () -> {
            try {
                // TODO: Implement actual delete through repository
                // For now, just disable it
                ModelProfile updated = new ModelProfile(
                        model.profileKey(),
                        model.provider(),
                        model.backendType(),
                        model.modelId(),
                        model.modelName(),
                        model.baseUrl(),
                        model.local(),
                        model.supportsTools(),
                        model.supportsStructuredOutput(),
                        model.taskCapabilities(),
                        model.costClass(),
                        model.latencyClass(),
                        model.locality(),
                        model.complianceTags(),
                        false,
                        "DELETED",
                        model.secretRef(),
                        model.priority(),
                        model.executionPolicy(),
                        updatedAudit(model.audit())
                );
                repository().save(updated);
                refresh();
                Notification.show(I18n.t("admin.model.deleted"));
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
    }

    private void refresh() {
        QuerySpec query = new QuerySpec(
                context().activeTenantId(),
                new PageSpec(0, 50, "modelName", SortDirection.ASC),
                List.of(),
                Map.of()
        );
        
        String searchText = searchField.getValue() == null ? "" : searchField.getValue().toLowerCase();
        String selectedBackendType = backendTypeFilter.getValue();
        String selectedProvider = providerFilter.getValue();
        boolean showDisabled = showDisabledCheckbox.getValue();

        List<ModelProfile> models = repository().findPage(query).items().stream()
                .filter(m -> showDisabled || m.enabled())
                .filter(m -> searchText.isEmpty() || 
                        m.modelName().toLowerCase().contains(searchText) ||
                        m.profileKey().toLowerCase().contains(searchText))
                .filter(m -> selectedBackendType == null || m.backendType().equals(selectedBackendType))
                .filter(m -> selectedProvider == null || m.provider().equals(selectedProvider))
                .toList();

        grid.setItems(models);
    }

    @Override
    public CurrentUserContext currentUserContext() {
        return currentUserContextService.currentUser();
    }

    private CurrentUserContext context() {
        return currentUserContext();
    }

    private ModelExecutionPolicy createDefaultExecutionPolicy(String fallbackProfileKey) {
        return new ModelExecutionPolicy(
                new ExecutionControls(java.time.Duration.ofSeconds(60), 3, 10, 60),
                new RetryPolicy(3, java.time.Duration.ofSeconds(1), java.time.Duration.ofSeconds(30)),
                fallbackProfileKey,
                true,
                true,
                true
        );
    }

    private AuditMetadata auditNow(long version) {
        Instant now = Instant.now();
        String userWithTenant = context().username() + "@" + context().activeTenantId();
        return new AuditMetadata(now, userWithTenant, now, userWithTenant, version);
    }

    private AuditMetadata updatedAudit(AuditMetadata existing) {
        String userWithTenant = context().username() + "@" + context().activeTenantId();
        return new AuditMetadata(
                existing.createdAt(),
                existing.createdBy(),
                Instant.now(),
                userWithTenant,
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

// Made with Bob
