package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.annotation.model.AnnotationNodeVerificationResult;
import ai.datalithix.kanon.annotation.model.AnnotationNodeVerificationStep;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationNode;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationNodeStatus;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationProviderType;
import ai.datalithix.kanon.annotation.service.ExternalAnnotationNodeService;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.ui.component.AdminDialogSupport;
import ai.datalithix.kanon.ui.i18n.I18n;
import ai.datalithix.kanon.ui.layout.MainLayout;
import ai.datalithix.kanon.ui.security.AdminAccess;
import ai.datalithix.kanon.ui.security.AdminSecuredView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import org.springframework.beans.factory.ObjectProvider;

@PageTitle("Annotation Nodes | Kanon Platform")
@Route(value = "admin/annotation-nodes", layout = MainLayout.class)
public class ExternalAnnotationNodeAdminView extends VerticalLayout implements AdminSecuredView {
    private final ObjectProvider<ExternalAnnotationNodeService> nodeServiceProvider;
    private final CurrentUserContextService currentUserContextService;
    private final Grid<ExternalAnnotationNode> grid = new Grid<>(ExternalAnnotationNode.class, false);

    public ExternalAnnotationNodeAdminView(
            ObjectProvider<ExternalAnnotationNodeService> nodeServiceProvider,
            CurrentUserContextService currentUserContextService
    ) {
        this.nodeServiceProvider = nodeServiceProvider;
        this.currentUserContextService = currentUserContextService;
        setSizeFull();
        add(createToolbar());
        configureGrid();
        add(grid);
        refresh();
    }

    private ExternalAnnotationNodeService service() {
        ExternalAnnotationNodeService service = nodeServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("ExternalAnnotationNodeService is not available");
        }
        return service;
    }

    private HorizontalLayout createToolbar() {
        HorizontalLayout actions = new HorizontalLayout();
        if (AdminAccess.canManageAnnotationNodes(context())) {
            Button createButton = new Button(I18n.t("admin.annotation-node.create"), event -> openCreateDialog());
            createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            actions.add(createButton);
        }
        return actions;
    }

    private void configureGrid() {
        grid.addColumn(ExternalAnnotationNode::displayName).setHeader(I18n.t("grid.name")).setAutoWidth(true);
        grid.addColumn(node -> node.providerType().name()).setHeader(I18n.t("field.provider")).setAutoWidth(true);
        grid.addColumn(ExternalAnnotationNode::baseUrl).setHeader(I18n.t("field.base-url")).setAutoWidth(true);
        grid.addColumn(node -> node.status() == null ? "" : node.status().name()).setHeader(I18n.t("grid.status")).setAutoWidth(true);
        grid.addColumn(node -> node.lastKnownVersion() == null ? "" : node.lastKnownVersion()).setHeader(I18n.t("grid.version")).setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader(I18n.t("grid.actions")).setAutoWidth(true);
    }

    private HorizontalLayout actions(ExternalAnnotationNode node) {
        HorizontalLayout actions = new HorizontalLayout();
        if (AdminAccess.canManageAnnotationNodes(context())) {
            Button edit = new Button(I18n.t("action.edit"), event -> openEditDialog(node));
            actions.add(edit);
            Button delete = new Button(I18n.t("action.delete"), event -> deleteNode(node));
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            actions.add(delete);
        }
        if (AdminAccess.canTestAnnotationNodes(context())) {
            Button test = new Button(I18n.t("admin.annotation-node.action.test"), event -> testConnection(node));
            test.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            actions.add(test);
        }
        return actions;
    }

    private void openCreateDialog() {
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.annotation-node.dialog.create"), "880px");
        TextField displayName = AdminDialogSupport.requiredText(I18n.t("field.name"), "");
        ComboBox<ExternalAnnotationProviderType> providerType = new ComboBox<>(I18n.t("field.provider"));
        providerType.setItems(ExternalAnnotationProviderType.values());
        providerType.setValue(ExternalAnnotationProviderType.LABEL_STUDIO);
        TextField baseUrl = AdminDialogSupport.requiredText(I18n.t("field.base-url"), "");
        TextField secretRef = AdminDialogSupport.requiredText(I18n.t("field.secret-ref"), I18n.t("admin.annotation-node.secret-ref.helper"));
        TextField storageBucket = new TextField(I18n.t("field.storage-bucket"));
        AdminDialogSupport.footer(dialog, I18n.t("action.create"), () -> {
            try {
                service().create(
                        context().activeTenantId(),
                        platformScoped(),
                        context().activeTenantId(),
                        displayName.getValue(),
                        providerType.getValue(),
                        baseUrl.getValue(),
                        secretRef.getValue(),
                        storageBucket.getValue(),
                        context().username()
                );
                dialog.close();
                refresh();
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
        dialog.add(AdminDialogSupport.body(displayName, providerType, baseUrl, secretRef, storageBucket));
        dialog.open();
    }

    private void openEditDialog(ExternalAnnotationNode node) {
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.annotation-node.dialog.update"), "880px");
        TextField displayName = AdminDialogSupport.requiredText(I18n.t("field.name"), "");
        displayName.setValue(node.displayName());
        TextField baseUrl = AdminDialogSupport.requiredText(I18n.t("field.base-url"), "");
        baseUrl.setValue(node.baseUrl());
        TextField secretRef = AdminDialogSupport.requiredText(I18n.t("field.secret-ref"), I18n.t("admin.annotation-node.secret-ref.helper"));
        secretRef.setValue(node.secretRef());
        TextField storageBucket = new TextField(I18n.t("field.storage-bucket"));
        storageBucket.setValue(node.storageBucket() == null ? "" : node.storageBucket());
        AdminDialogSupport.footer(dialog, I18n.t("action.update"), () -> {
            try {
                service().update(
                        context().activeTenantId(),
                        platformScoped(),
                        node.nodeId(),
                        displayName.getValue(),
                        baseUrl.getValue(),
                        secretRef.getValue(),
                        storageBucket.getValue(),
                        node.enabled(),
                        context().username()
                );
                dialog.close();
                refresh();
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage());
            }
        });
        dialog.add(AdminDialogSupport.body(displayName, baseUrl, secretRef, storageBucket));
        dialog.open();
    }

    private void testConnection(ExternalAnnotationNode node) {
        Dialog dialog = AdminDialogSupport.dialog(I18n.t("admin.annotation-node.dialog.test"), "760px");
        Span output = new Span(I18n.t("admin.annotation-node.test.running"));
        output.getStyle().set("white-space", "pre-wrap");
        output.getStyle().set("font-family", "monospace");
        dialog.add(output);
        dialog.open();
        dialog.getUI().ifPresent(ui -> Thread.ofVirtual().start(() -> {
            AnnotationNodeVerificationResult result = service().testConnection(
                    context().activeTenantId(),
                    platformScoped(),
                    node.nodeId(),
                    context().username()
            );
            ui.access(() -> {
                output.setText(renderResult(result));
                refresh();
            });
        }));
    }

    String renderResult(AnnotationNodeVerificationResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("Status: ").append(result.resultingStatus()).append("\n");
        builder.append("Latency: ").append(result.totalLatencyMs()).append("ms\n");
        builder.append("Version: ").append(result.detectedVersion() == null ? "unknown" : result.detectedVersion()).append("\n\n");
        for (AnnotationNodeVerificationStep step : result.steps()) {
            builder.append("- ").append(step.stepName()).append(": ").append(step.status()).append(" (").append(step.detail()).append(")\n");
        }
        return builder.toString();
    }

    private void deleteNode(ExternalAnnotationNode node) {
        try {
            service().delete(context().activeTenantId(), platformScoped(), node.nodeId(), context().username());
            refresh();
        } catch (RuntimeException exception) {
            Notification.show(exception.getMessage());
        }
    }

    private void refresh() {
        List<ExternalAnnotationNode> nodes = service().list(context().activeTenantId(), platformScoped());
        grid.setItems(nodes);
    }

    private boolean platformScoped() {
        return context().permissions().contains("platform.config.manage") || context().permissions().contains("platform.config.read");
    }

    @Override
    public CurrentUserContext currentUserContext() {
        return currentUserContextService.currentUser();
    }

    private CurrentUserContext context() {
        return currentUserContext();
    }
}
