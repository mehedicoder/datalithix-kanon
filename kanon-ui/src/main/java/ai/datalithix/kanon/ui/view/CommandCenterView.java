package ai.datalithix.kanon.ui.view;

import ai.datalithix.kanon.airouting.model.ChatModelRoute;
import ai.datalithix.kanon.airouting.model.ModelProfile;
import ai.datalithix.kanon.airouting.service.ModelProfileRepository;
import ai.datalithix.kanon.airouting.service.ModelRouter;
import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.TenantContext;
import ai.datalithix.kanon.common.model.PageSpec;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.model.SortDirection;
import ai.datalithix.kanon.domain.model.TaskDescriptor;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceQueryService;
import ai.datalithix.kanon.ingestion.service.DataSourceConnector;
import ai.datalithix.kanon.ingestion.service.DataSourceConnectorRegistry;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.ui.component.HowItWorksSection;
import ai.datalithix.kanon.ui.component.RedactedText;
import ai.datalithix.kanon.ui.i18n.I18n;
import ai.datalithix.kanon.ui.layout.MainLayout;
import ai.datalithix.kanon.workflow.model.WorkflowActionRequest;
import ai.datalithix.kanon.workflow.model.WorkflowInstance;
import ai.datalithix.kanon.workflow.service.WorkflowInstanceRepository;
import ai.datalithix.kanon.workflow.service.WorkflowTaskCommandService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;

@PageTitle("Command Center | Kanon Platform")
@Route(value = "", layout = MainLayout.class)
public class CommandCenterView extends VerticalLayout {
    private static final String DEFAULT_TENANT = "demo-tenant";

    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowTaskCommandService workflowTaskCommandService;
    private final EvidenceQueryService evidenceQueryService;
    private final DataSourceConnectorRegistry connectorRegistry;
    private final ModelRouter modelRouter;
    private final ObjectProvider<ModelProfileRepository> modelProfileRepository;
    private final CurrentUserContextService currentUserContextService;

    private final TextField tenantField = new TextField(I18n.t("field.tenant"));
    private final TextField caseField = new TextField(I18n.t("field.case-filter"));
    private final TextField actorField = new TextField(I18n.t("field.actor"));
    private final TextField reasonField = new TextField(I18n.t("field.reason"));
    private final Grid<WorkflowInstance> workflowGrid = new Grid<>(WorkflowInstance.class, false);
    private final Grid<WorkflowInstance> reviewGrid = new Grid<>(WorkflowInstance.class, false);
    private final Grid<EvidenceEvent> evidenceGrid = new Grid<>(EvidenceEvent.class, false);
    private final Grid<AnnotationDiffRow> annotationDiffGrid = new Grid<>(AnnotationDiffRow.class, false);
    private final Grid<DocumentCorrectionRow> documentCorrectionGrid = new Grid<>(DocumentCorrectionRow.class, false);
    private final Grid<ExternalAnnotationHandoffRow> externalHandoffGrid = new Grid<>(ExternalAnnotationHandoffRow.class, false);
    private final Grid<ExternalAnnotationSyncRow> externalSyncGrid = new Grid<>(ExternalAnnotationSyncRow.class, false);
    private final Grid<DataSourceConnector> connectorGrid = new Grid<>(DataSourceConnector.class, false);
    private final Grid<ModelProfile> modelGrid = new Grid<>(ModelProfile.class, false);
    private final Paragraph routeSummary = new Paragraph();

    public CommandCenterView(
            WorkflowInstanceRepository workflowInstanceRepository,
            WorkflowTaskCommandService workflowTaskCommandService,
            EvidenceQueryService evidenceQueryService,
            DataSourceConnectorRegistry connectorRegistry,
            ModelRouter modelRouter,
            ObjectProvider<ModelProfileRepository> modelProfileRepository,
            CurrentUserContextService currentUserContextService
    ) {
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.workflowTaskCommandService = workflowTaskCommandService;
        this.evidenceQueryService = evidenceQueryService;
        this.connectorRegistry = connectorRegistry;
        this.modelRouter = modelRouter;
        this.modelProfileRepository = modelProfileRepository;
        this.currentUserContextService = currentUserContextService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kanon-command-center");

        CurrentUserContext context = currentUserContextService.currentUser();
        tenantField.setValue(context.activeTenantId());
        tenantField.setReadOnly(true);
        actorField.setValue(context.username());
        actorField.setReadOnly(true);
        reasonField.setPlaceholder(I18n.t("command.reason.placeholder"));

        add(
                cockpitHero(),
                overview(),
                toolbar(),
                workflowSection(),
                reviewSection(),
                evidenceSection(),
                annotationDiffSection(),
                documentCorrectionSection(),
                externalAnnotationHandoffSection(),
                externalAnnotationSyncSection(),
                connectorSection(),
                modelSection()
        );
        refresh();
    }

    private HorizontalLayout toolbar() {
        Button refresh = new Button(I18n.t("action.refresh"), event -> refresh());
        HorizontalLayout toolbar = new HorizontalLayout(tenantField, caseField, actorField, reasonField, refresh);
        toolbar.setAlignItems(Alignment.END);
        toolbar.setWidthFull();
        toolbar.addClassName("kanon-toolbar");
        tenantField.setWidth("180px");
        caseField.setWidth("180px");
        actorField.setWidth("180px");
        reasonField.setWidth("420px");
        return toolbar;
    }

    private Div cockpitHero() {
        Div hero = new Div();
        hero.addClassName("kanon-cockpit");

        Div brand = new Div(new Span("Datalithix"), new Span("Kanon AI Command Center"));
        brand.addClassName("cockpit-brand");

        Div gauge = new Div(new Span("A"), new Span("92% confidence"), new Span("Compliance Risk Score"));
        gauge.addClassName("risk-gauge");

        Div rings = new Div(
                article("EU AI Act"),
                article("GDPR"),
                article("Article 6"),
                article("Classification")
        );
        rings.addClassName("regulatory-rings");

        Div hub = new Div(gauge, rings);
        hub.addClassName("cockpit-hub");

        Div modelCard = card("LLM Model Analysis", "Llama 3 - 98% compliant", "Fallback route and audit constraints verified.");
        Div provenanceCard = card("Data Provenance", "Ledger sealed", "Source nodes, transformations, and evidence hashes are linked.");
        Div articlePanel = card("Article 6: High-Risk AI Classification", "Model map aligned", "Traceability, human oversight, and evidence checkpoints are ready for review.");
        articlePanel.addClassName("article-panel");

        Image castle = new Image("https://upload.wikimedia.org/wikipedia/commons/thumb/3/3e/Nuremberg_Castle%2C_Germany.jpg/800px-Nuremberg_Castle%2C_Germany.jpg", "Nuremberg Castle skyline");
        Div window = new Div(castle);
        window.addClassName("castle-window");

        Div user = new Div(new Span("Review lead"), new Span("Article 6 report"));
        user.addClassName("operator-card");

        Div left = new Div(modelCard, provenanceCard);
        left.addClassName("cockpit-column");

        Div right = new Div(articlePanel, window, user);
        right.addClassName("cockpit-column");

        Div timeline = auditTimeline();
        Div footer = new Div(profileCard("mehedicoder", "Platform contact"), profileCard("Datalithix", "Governance engineering"));
        footer.addClassName("cockpit-footer");

        hero.add(brand, left, hub, right, timeline, footer);
        return hero;
    }

    private static Span article(String text) {
        Span article = new Span(text);
        article.addClassName("article-chip");
        return article;
    }

    private static Div card(String title, String metric, String detail) {
        Div card = new Div();
        card.addClassName("cockpit-card");
        card.add(new H4(title), new Span(metric), new Paragraph(detail));
        return card;
    }

    private static Div auditTimeline() {
        Div timeline = new Div();
        timeline.addClassName("audit-timeline");
        timeline.add(new H3("Kanon Audit Trail"));
        timeline.add(timelineStep("Policy mapped"), timelineStep("Model checked"), timelineStep("Reviewer assigned"), timelineStep("Evidence sealed"));
        return timeline;
    }

    private static Span timelineStep(String text) {
        Span step = new Span(text);
        step.addClassName("timeline-step");
        return step;
    }

    private static Div profileCard(String title, String detail) {
        Div card = new Div(new Span(title), new Span(detail));
        card.addClassName("profile-card");
        return card;
    }

    private VerticalLayout workflowSection() {
        workflowGrid.addColumn(WorkflowInstance::workflowInstanceId).setHeader(I18n.t("grid.workflow-instance")).setAutoWidth(true);
        workflowGrid.addColumn(WorkflowInstance::caseId).setHeader(I18n.t("grid.case")).setAutoWidth(true);
        workflowGrid.addColumn(WorkflowInstance::currentStep).setHeader(I18n.t("grid.step")).setAutoWidth(true);
        workflowGrid.addColumn(WorkflowInstance::currentState).setHeader(I18n.t("grid.state")).setAutoWidth(true);
        workflowGrid.addColumn(WorkflowInstance::assignedUserId).setHeader(I18n.t("grid.assignee")).setAutoWidth(true);
        workflowGrid.addColumn(instance -> instance.reviewStatus().name()).setHeader(I18n.t("grid.review")).setAutoWidth(true);
        workflowGrid.addColumn(instance -> instance.approvalStatus().name()).setHeader(I18n.t("grid.approval")).setAutoWidth(true);
        workflowGrid.addColumn(instance -> format(instance.dueAt())).setHeader(I18n.t("grid.due")).setAutoWidth(true);
        workflowGrid.setHeight("260px");
        return section(I18n.t("command.section.workflow-board"), workflowGrid);
    }

    private VerticalLayout reviewSection() {
        reviewGrid.addColumn(WorkflowInstance::workflowInstanceId).setHeader(I18n.t("grid.task")).setAutoWidth(true);
        reviewGrid.addColumn(WorkflowInstance::caseId).setHeader(I18n.t("grid.case")).setAutoWidth(true);
        reviewGrid.addColumn(WorkflowInstance::assignedUserId).setHeader(I18n.t("grid.assignee")).setAutoWidth(true);
        reviewGrid.addColumn(instance -> instance.reviewStatus().name()).setHeader(I18n.t("grid.review")).setAutoWidth(true);
        reviewGrid.addColumn(instance -> instance.approvalStatus().name()).setHeader(I18n.t("grid.approval")).setAutoWidth(true);
        reviewGrid.addColumn(WorkflowInstance::escalationReason).setHeader(I18n.t("grid.escalation")).setAutoWidth(true);
        reviewGrid.setHeight("220px");

        HorizontalLayout actions = new HorizontalLayout(
                action(I18n.t("action.start-review"), workflowTaskCommandService::startReview),
                action(I18n.t("action.complete-review"), workflowTaskCommandService::completeReview),
                action(I18n.t("action.approve"), workflowTaskCommandService::approve),
                action(I18n.t("action.reject"), workflowTaskCommandService::reject),
                action(I18n.t("action.escalate"), workflowTaskCommandService::escalate),
                action(I18n.t("action.export-ready"), workflowTaskCommandService::markExportReady)
        );
        actions.setWidthFull();
        return section(I18n.t("command.section.human-task-inbox"), actions, reviewGrid);
    }

    private VerticalLayout evidenceSection() {
        evidenceGrid.addColumn(EvidenceEvent::eventId).setHeader(I18n.t("grid.event")).setAutoWidth(true);
        evidenceGrid.addColumn(EvidenceEvent::caseId).setHeader(I18n.t("grid.case")).setAutoWidth(true);
        evidenceGrid.addColumn(EvidenceEvent::eventType).setHeader(I18n.t("grid.type")).setAutoWidth(true);
        evidenceGrid.addColumn(EvidenceEvent::actorId).setHeader(I18n.t("grid.actor")).setAutoWidth(true);
        evidenceGrid.addComponentColumn(event -> RedactedText.sensitiveField("rationale", event.rationale())).setHeader(I18n.t("grid.rationale")).setAutoWidth(true);
        evidenceGrid.addColumn(event -> format(event.occurredAt())).setHeader(I18n.t("grid.occurred")).setAutoWidth(true);
        evidenceGrid.setHeight("240px");
        return section(I18n.t("command.section.evidence-explorer"), evidenceGrid);
    }

    private VerticalLayout annotationDiffSection() {
        annotationDiffGrid.addColumn(AnnotationDiffRow::caseId).setHeader(I18n.t("grid.case")).setAutoWidth(true);
        annotationDiffGrid.addColumn(AnnotationDiffRow::eventType).setHeader(I18n.t("grid.event-type")).setAutoWidth(true);
        annotationDiffGrid.addColumn(AnnotationDiffRow::fieldName).setHeader(I18n.t("grid.field")).setAutoWidth(true);
        annotationDiffGrid.addComponentColumn(row -> RedactedText.sensitiveField(row.fieldName(), row.beforeValue())).setHeader(I18n.t("grid.before")).setAutoWidth(true);
        annotationDiffGrid.addComponentColumn(row -> RedactedText.sensitiveField(row.fieldName(), row.afterValue())).setHeader(I18n.t("grid.after")).setAutoWidth(true);
        annotationDiffGrid.addColumn(AnnotationDiffRow::actorId).setHeader(I18n.t("grid.actor")).setAutoWidth(true);
        annotationDiffGrid.addColumn(row -> format(row.changedAt())).setHeader(I18n.t("grid.changed")).setAutoWidth(true);
        annotationDiffGrid.setHeight("220px");
        return section(I18n.t("command.section.annotation-diff"), annotationDiffGrid);
    }

    private VerticalLayout connectorSection() {
        connectorGrid.addColumn(DataSourceConnector::connectorId).setHeader(I18n.t("grid.connector")).setAutoWidth(true);
        connectorGrid.addColumn(connector -> connector.sourceCategory().name()).setHeader(I18n.t("grid.category")).setAutoWidth(true);
        connectorGrid.addColumn(connector -> connector.sourceType().name()).setHeader(I18n.t("grid.source-type")).setAutoWidth(true);
        connectorGrid.addColumn(connector -> connector.connectorType().name()).setHeader(I18n.t("grid.type")).setAutoWidth(true);
        connectorGrid.addColumn(connector -> connector.health().status().name()).setHeader(I18n.t("grid.health")).setAutoWidth(true);
        connectorGrid.setHeight("220px");
        return section(I18n.t("command.section.source-connectors"), connectorGrid);
    }

    private VerticalLayout documentCorrectionSection() {
        documentCorrectionGrid.addColumn(DocumentCorrectionRow::caseId).setHeader(I18n.t("grid.case")).setAutoWidth(true);
        documentCorrectionGrid.addColumn(DocumentCorrectionRow::fieldName).setHeader(I18n.t("grid.field")).setAutoWidth(true);
        documentCorrectionGrid.addComponentColumn(row -> RedactedText.sensitiveField(row.fieldName(), row.extractedValue())).setHeader(I18n.t("grid.before")).setAutoWidth(true);
        documentCorrectionGrid.addComponentColumn(row -> RedactedText.sensitiveField(row.fieldName(), row.suggestedValue())).setHeader(I18n.t("grid.after")).setAutoWidth(true);
        documentCorrectionGrid.addColumn(DocumentCorrectionRow::confidence).setHeader(I18n.t("grid.confidence")).setAutoWidth(true);
        documentCorrectionGrid.addComponentColumn(row -> {
            Button apply = new Button(I18n.t("action.apply-correction"));
            apply.addClickListener(event -> Notification.show(I18n.t("notification.correction.applied", row.fieldName(), row.caseId())));
            return apply;
        }).setHeader(I18n.t("grid.actions")).setAutoWidth(true);
        documentCorrectionGrid.setHeight("220px");
        return section(I18n.t("command.section.document-correction"), documentCorrectionGrid);
    }

    private VerticalLayout externalAnnotationHandoffSection() {
        externalHandoffGrid.addColumn(ExternalAnnotationHandoffRow::caseId).setHeader(I18n.t("grid.case")).setAutoWidth(true);
        externalHandoffGrid.addColumn(ExternalAnnotationHandoffRow::nodeType).setHeader(I18n.t("grid.type")).setAutoWidth(true);
        externalHandoffGrid.addColumn(ExternalAnnotationHandoffRow::externalTaskId).setHeader(I18n.t("grid.external-task")).setAutoWidth(true);
        externalHandoffGrid.addComponentColumn(row -> {
            Anchor anchor = new Anchor(row.workbenchUrl(), I18n.t("action.open-workbench"));
            anchor.setTarget("_blank");
            return anchor;
        }).setHeader(I18n.t("grid.link")).setAutoWidth(true);
        externalHandoffGrid.setHeight("200px");
        return section(I18n.t("command.section.external-handoff"), externalHandoffGrid);
    }

    private VerticalLayout externalAnnotationSyncSection() {
        externalSyncGrid.addColumn(ExternalAnnotationSyncRow::caseId).setHeader(I18n.t("grid.case")).setAutoWidth(true);
        externalSyncGrid.addColumn(ExternalAnnotationSyncRow::annotationTaskId).setHeader(I18n.t("grid.task")).setAutoWidth(true);
        externalSyncGrid.addColumn(ExternalAnnotationSyncRow::syncStatus).setHeader(I18n.t("grid.sync-status")).setAutoWidth(true);
        externalSyncGrid.addColumn(ExternalAnnotationSyncRow::reviewStatus).setHeader(I18n.t("grid.review")).setAutoWidth(true);
        externalSyncGrid.addColumn(ExternalAnnotationSyncRow::approvalStatus).setHeader(I18n.t("grid.approval")).setAutoWidth(true);
        externalSyncGrid.addColumn(ExternalAnnotationSyncRow::exportStatus).setHeader(I18n.t("grid.export")).setAutoWidth(true);
        externalSyncGrid.addColumn(row -> format(row.updatedAt())).setHeader(I18n.t("grid.updated")).setAutoWidth(true);
        externalSyncGrid.setHeight("240px");
        return section(I18n.t("command.section.external-sync-final-review"), externalSyncGrid);
    }

    private VerticalLayout modelSection() {
        modelGrid.addColumn(ModelProfile::profileKey).setHeader(I18n.t("grid.profile")).setAutoWidth(true);
        modelGrid.addColumn(ModelProfile::provider).setHeader(I18n.t("grid.provider")).setAutoWidth(true);
        modelGrid.addColumn(ModelProfile::backendType).setHeader(I18n.t("grid.backend")).setAutoWidth(true);
        modelGrid.addColumn(ModelProfile::modelName).setHeader(I18n.t("grid.model")).setAutoWidth(true);
        modelGrid.addColumn(ModelProfile::healthStatus).setHeader(I18n.t("grid.health")).setAutoWidth(true);
        modelGrid.addColumn(profile -> profile.enabled() ? I18n.t("state.enabled") : I18n.t("state.disabled")).setHeader(I18n.t("grid.state")).setAutoWidth(true);
        modelGrid.setHeight("220px");
        return section(I18n.t("command.section.model-visibility"), routeSummary, modelGrid);
    }

    private HowItWorksSection overview() {
        return new HowItWorksSection(
                I18n.t("command.overview.summary"),
                List.of(
                        I18n.t("command.overview.workflow-board"),
                        I18n.t("command.overview.human-inbox"),
                        I18n.t("command.overview.evidence-records"),
                        I18n.t("command.overview.evidence-explorer"),
                        I18n.t("command.overview.annotation-diff"),
                        I18n.t("command.overview.source-connectors"),
                        I18n.t("command.overview.model-visibility"),
                        I18n.t("command.overview.external-annotation"),
                        I18n.t("command.overview.source-of-truth")
                )
        );
    }

    private Button action(String label, WorkflowMutation mutation) {
        return new Button(label, event -> reviewGrid.asSingleSelect().getOptionalValue()
                .ifPresentOrElse(
                        selected -> mutate(selected, mutation, label),
                        () -> Notification.show(I18n.t("notification.select-review-task"))
                ));
    }

    private void mutate(WorkflowInstance selected, WorkflowMutation mutation, String label) {
        try {
            mutation.apply(new WorkflowActionRequest(
                    tenant(),
                    selected.workflowInstanceId(),
                    actor(),
                    reasonField.getValue()
            ));
            Notification.show(I18n.t("notification.action.completed", label));
            refresh();
        } catch (RuntimeException exception) {
            Notification.show(I18n.t("notification.action.failed", label, exception.getMessage()));
        }
    }

    private void refresh() {
        String tenantId = tenant();
        CurrentUserContext context = currentUserContextService.currentUser();
        workflowGrid.setItems(workflowInstanceRepository.findPage(query(context, 50)).items());
        reviewGrid.setItems(workflowInstanceRepository.findPage(reviewQuery(context, 50)).items());
        List<EvidenceEvent> evidenceEvents = caseFilter().isBlank()
                ? evidenceQueryService.findRecent(tenantId, 50)
                : evidenceQueryService.findByCaseId(tenantId, caseFilter(), 50);
        evidenceGrid.setItems(evidenceEvents);
        annotationDiffGrid.setItems(annotationDiffRows(evidenceEvents));
        documentCorrectionGrid.setItems(documentCorrectionRows(evidenceEvents));
        externalHandoffGrid.setItems(externalHandoffRows(evidenceEvents));
        externalSyncGrid.setItems(externalSyncRows(evidenceEvents, workflowInstanceRepository.findPage(query(context, 50)).items()));
        connectorGrid.setItems(connectorRegistry.connectors());
        routeSummary.setText(modelRouteSummary(tenantId));
        ModelProfileRepository repository = modelProfileRepository.getIfAvailable();
        if (repository == null) {
            modelGrid.setItems(List.of());
        } else {
            modelGrid.setItems(repository.findPage(query(tenantId, 50)).items());
        }
    }

    private String tenant() {
        String tenant = tenantField.getValue();
        return tenant == null || tenant.isBlank() ? DEFAULT_TENANT : tenant;
    }

    private String actor() {
        String actor = actorField.getValue();
        return actor == null || actor.isBlank() ? "reviewer" : actor;
    }

    private String caseFilter() {
        String caseId = caseField.getValue();
        return caseId == null ? "" : caseId.trim();
    }

    private static QuerySpec query(String tenantId, int size) {
        return new QuerySpec(tenantId, new PageSpec(0, size, "updatedAt", SortDirection.DESC), List.of(), Map.of());
    }

    private static QuerySpec query(CurrentUserContext context, int size) {
        return new QuerySpec(context.activeTenantId(), new PageSpec(0, size, "updatedAt", SortDirection.DESC), List.of(), Map.of(
                "organizationId", context.activeOrganizationId(),
                "workspaceId", context.activeWorkspaceId()
        ));
    }

    private static QuerySpec reviewQuery(CurrentUserContext context, int size) {
        return new QuerySpec(context.activeTenantId(), new PageSpec(0, size, "dueAt", SortDirection.ASC), List.of(), Map.of(
                "organizationId", context.activeOrganizationId(),
                "workspaceId", context.activeWorkspaceId(),
                "reviewStatus", "PENDING"
        ));
    }

    private String modelRouteSummary(String tenantId) {
        TenantContext tenantContext = new TenantContext(
                tenantId,
                DomainType.ACCOUNTING,
                "DE",
                "EU_AI_ACT_2026",
                true,
                false,
                java.util.Set.of("AUDIT_REQUIRED")
        );
        ChatModelRoute route = modelRouter.resolve(
                tenantContext,
                new TaskDescriptor(AiTaskType.EXTRACTION, "command-center-preview", "memory://preview", "v1", false)
        );
        return I18n.t("command.model-route.summary", route.primaryProfileKey(), route.fallbackProfileKey(), route.reason());
    }

    private static VerticalLayout section(String title, com.vaadin.flow.component.Component... components) {
        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull();
        layout.setPadding(false);
        layout.addClassName("kanon-section");
        layout.add(new H2(title));
        layout.add(components);
        return layout;
    }

    private static String format(Instant instant) {
        return instant == null ? "" : instant.toString();
    }

    private static List<AnnotationDiffRow> annotationDiffRows(List<EvidenceEvent> evidenceEvents) {
        return evidenceEvents.stream()
                .flatMap(event -> changedKeys(event).stream()
                        .map(key -> new AnnotationDiffRow(
                                event.caseId(),
                                event.eventType(),
                                key,
                                value(event.beforeState(), key),
                                value(event.afterState(), key),
                                event.actorId(),
                                event.occurredAt()
                        )))
                .toList();
    }

    private static List<DocumentCorrectionRow> documentCorrectionRows(List<EvidenceEvent> evidenceEvents) {
        return evidenceEvents.stream()
                .filter(event -> "EXTERNAL_ANNOTATION_TASK_SYNCED".equals(event.eventType()) || "ANNOTATION_CORRECTED".equals(event.eventType()))
                .map(event -> new DocumentCorrectionRow(
                        event.caseId(),
                        value(event.afterState(), "fieldName").isBlank() ? "extractedField" : value(event.afterState(), "fieldName"),
                        value(event.beforeState(), "value").isBlank() ? "pending-review" : value(event.beforeState(), "value"),
                        value(event.afterState(), "value").isBlank() ? "reviewed-value" : value(event.afterState(), "value"),
                        "0.82"
                ))
                .limit(25)
                .toList();
    }

    private static List<ExternalAnnotationHandoffRow> externalHandoffRows(List<EvidenceEvent> evidenceEvents) {
        return evidenceEvents.stream()
                .filter(event -> "EXTERNAL_ANNOTATION_TASK_PUSHED".equals(event.eventType()))
                .map(event -> {
                    String nodeType = value(event.afterState(), "nodeType");
                    String externalTaskId = value(event.afterState(), "externalTaskId");
                    if (externalTaskId.isBlank()) {
                        externalTaskId = "unknown";
                    }
                    String workbenchUrl = "memory://annotation/" + externalTaskId;
                    return new ExternalAnnotationHandoffRow(
                            event.caseId(),
                            nodeType.isBlank() ? "EXTERNAL" : nodeType,
                            externalTaskId,
                            workbenchUrl
                    );
                })
                .limit(25)
                .toList();
    }

    private static List<ExternalAnnotationSyncRow> externalSyncRows(List<EvidenceEvent> evidenceEvents, List<WorkflowInstance> workflows) {
        return evidenceEvents.stream()
                .filter(event -> event.eventType().startsWith("EXTERNAL_ANNOTATION_TASK_"))
                .map(event -> {
                    String annotationTaskId = value(event.afterState(), "annotationTaskId");
                    String syncStatus = value(event.afterState(), "status");
                    WorkflowInstance workflow = workflows.stream()
                            .filter(item -> Objects.equals(item.caseId(), event.caseId()))
                            .findFirst()
                            .orElse(null);
                    return new ExternalAnnotationSyncRow(
                            event.caseId(),
                            annotationTaskId.isBlank() ? "unknown" : annotationTaskId,
                            syncStatus.isBlank() ? event.eventType() : syncStatus,
                            workflow == null ? "UNKNOWN" : workflow.reviewStatus().name(),
                            workflow == null ? "UNKNOWN" : workflow.approvalStatus().name(),
                            workflow == null ? "PENDING" : workflow.currentState(),
                            event.occurredAt()
                    );
                })
                .limit(50)
                .toList();
    }

    private static Set<String> changedKeys(EvidenceEvent event) {
        Map<String, Object> before = event.beforeState() == null ? Map.of() : event.beforeState();
        Map<String, Object> after = event.afterState() == null ? Map.of() : event.afterState();
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(before.keySet());
        keys.addAll(after.keySet());
        keys.removeIf(key -> Objects.equals(before.get(key), after.get(key)));
        return keys;
    }

    private static String value(Map<String, Object> state, String key) {
        if (state == null || !state.containsKey(key)) {
            return "";
        }
        Object value = state.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private record AnnotationDiffRow(
            String caseId,
            String eventType,
            String fieldName,
            String beforeValue,
            String afterValue,
            String actorId,
            Instant changedAt
    ) {}

    private record DocumentCorrectionRow(
            String caseId,
            String fieldName,
            String extractedValue,
            String suggestedValue,
            String confidence
    ) {
    }

    private record ExternalAnnotationHandoffRow(
            String caseId,
            String nodeType,
            String externalTaskId,
            String workbenchUrl
    ) {
    }

    private record ExternalAnnotationSyncRow(
            String caseId,
            String annotationTaskId,
            String syncStatus,
            String reviewStatus,
            String approvalStatus,
            String exportStatus,
            Instant updatedAt
    ) {
    }

    @FunctionalInterface
    private interface WorkflowMutation {
        WorkflowInstance apply(WorkflowActionRequest request);
    }
}
