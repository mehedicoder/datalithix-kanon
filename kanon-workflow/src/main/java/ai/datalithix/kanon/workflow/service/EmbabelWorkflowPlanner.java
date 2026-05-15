package ai.datalithix.kanon.workflow.service;

import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.TenantContext;
import ai.datalithix.kanon.common.compliance.DataClassification;
import ai.datalithix.kanon.common.runtime.BackpressurePolicy;
import ai.datalithix.kanon.common.runtime.BackpressureStrategy;
import ai.datalithix.kanon.common.runtime.RetryPolicy;
import ai.datalithix.kanon.common.security.AccessControlContext;
import ai.datalithix.kanon.common.security.AccessPurpose;
import ai.datalithix.kanon.common.security.Permission;
import ai.datalithix.kanon.common.security.SecurityRole;
import ai.datalithix.kanon.domain.model.TaskDescriptor;
import ai.datalithix.kanon.policy.model.PolicyDecision;
import ai.datalithix.kanon.workflow.model.WorkflowActionDefinition;
import ai.datalithix.kanon.workflow.model.WorkflowExecutionPolicy;
import ai.datalithix.kanon.workflow.model.WorkflowGoal;
import ai.datalithix.kanon.workflow.model.WorkflowPlan;
import ai.datalithix.kanon.workflow.model.WorkflowPlanningContext;
import ai.datalithix.kanon.workflow.model.WorkflowPlanningProblem;
import ai.datalithix.kanon.workflow.model.WorkflowType;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Primary
@Profile("embabel")
public class EmbabelWorkflowPlanner implements WorkflowPlanner {
    private final ObjectProvider<EmbabelPlanningClient> embabelPlanningClient;
    private final WorkflowActionCatalog actionCatalog;
    private final WorkflowPlanner fallbackPlanner;

    public EmbabelWorkflowPlanner(
            ObjectProvider<EmbabelPlanningClient> embabelPlanningClient,
            ObjectProvider<WorkflowActionCatalog> actionCatalog,
            DefaultWorkflowPlanner fallbackPlanner
    ) {
        this.embabelPlanningClient = embabelPlanningClient;
        WorkflowActionCatalog configuredActionCatalog = actionCatalog.getIfAvailable();
        this.actionCatalog = configuredActionCatalog == null
                ? context -> defaultActions(context.workflowType())
                : configuredActionCatalog;
        this.fallbackPlanner = fallbackPlanner;
    }

    @Override
    public WorkflowPlan plan(TenantContext tenantContext, TaskDescriptor taskDescriptor) {
        WorkflowType workflowType = workflowType(taskDescriptor.taskType());
        WorkflowPlanningContext context = planningContext(tenantContext, taskDescriptor, workflowType);
        WorkflowPlanningProblem problem = new WorkflowPlanningProblem(
                tenantContext.tenantId() + "-" + taskDescriptor.caseId() + "-embabel",
                context,
                goal(taskDescriptor.taskType(), workflowType),
                initialFacts(taskDescriptor),
                actionCatalog.actionsFor(context)
        );
        EmbabelPlanningClient client = embabelPlanningClient.getIfAvailable();
        if (client == null) {
            return fallbackPlanner.plan(tenantContext, taskDescriptor);
        }
        EmbabelPlanningResult result = client.plan(problem);
        if (result == null || !result.usable()) {
            WorkflowPlan fallback = fallbackPlanner.plan(tenantContext, taskDescriptor);
            return new WorkflowPlan(
                    fallback.workflowKey(),
                    fallback.caseId(),
                    fallback.correlationId(),
                    fallback.plannedSteps(),
                    fallback.executionPolicy(),
                    "Embabel planner fallback: " + (result == null ? "no result" : result.failureReason())
            );
        }
        return new WorkflowPlan(
                workflowType.name().toLowerCase() + "-embabel-workflow",
                taskDescriptor.caseId(),
                taskDescriptor.caseId(),
                result.actionIds(),
                executionPolicy(result.actionIds()),
                result.rationale()
        );
    }

    private static WorkflowPlanningContext planningContext(
            TenantContext tenantContext,
            TaskDescriptor taskDescriptor,
            WorkflowType workflowType
    ) {
        return new WorkflowPlanningContext(
                tenantContext.tenantId(),
                taskDescriptor.caseId(),
                workflowType,
                tenantContext.domainType(),
                taskDescriptor.taskType(),
                AssetType.UNKNOWN,
                SourceCategory.SYSTEM,
                SourceType.EXTERNAL_SYSTEM,
                null,
                null,
                new PolicyDecision(true, List.copyOf(tenantContext.enabledPolicies()), "Tenant policy allows planning"),
                new AccessControlContext(
                        tenantContext.tenantId(),
                        "system",
                        Set.of(SecurityRole.INTEGRATION_SERVICE_ACCOUNT),
                        Set.of(Permission.WORKFLOW_EXECUTE, Permission.MODEL_INVOKE),
                        Set.of(tenantContext.domainType()),
                        Set.of(taskDescriptor.caseId()),
                        Set.of(DataClassification.INTERNAL),
                        AccessPurpose.MODEL_INVOCATION
                ),
                Map.of(
                        "countryCode", tenantContext.countryCode(),
                        "regulatoryAct", tenantContext.regulatoryAct(),
                        "schemaVersion", taskDescriptor.schemaVersion()
                )
        );
    }

    private static WorkflowType workflowType(AiTaskType taskType) {
        return switch (taskType) {
            case EXTRACTION, CLASSIFICATION, OCR_POST_PROCESSING -> WorkflowType.ANNOTATION_EXTRACTION;
            case COMPLIANCE_CHECK, SCORING, REASONING -> WorkflowType.HUMAN_REVIEW_APPROVAL;
            case SUMMARIZATION, TRANSLATION -> WorkflowType.DATA_INGESTION;
        };
    }

    private static WorkflowGoal goal(AiTaskType taskType, WorkflowType workflowType) {
        Set<String> desiredFacts = switch (workflowType) {
            case DATA_INGESTION -> Set.of("payload.ingested");
            case ANNOTATION_EXTRACTION -> Set.of("annotations.produced");
            case HUMAN_REVIEW_APPROVAL -> Set.of("human.review.completed");
        };
        return new WorkflowGoal(
                taskType.name().toLowerCase() + "-goal",
                "Complete " + taskType.name().toLowerCase() + " workflow",
                workflowType,
                desiredFacts,
                Set.of("workflow.failed"),
                Set.of(),
                10
        );
    }

    private static Set<String> initialFacts(TaskDescriptor taskDescriptor) {
        if (taskDescriptor.inputRef() == null || taskDescriptor.inputRef().isBlank()) {
            return Set.of();
        }
        return Set.of("input.available");
    }

    private static List<WorkflowActionDefinition> defaultActions(WorkflowType workflowType) {
        return switch (workflowType) {
            case DATA_INGESTION -> List.of(
                    action("ingest-payload", workflowType, Set.of("input.available"), Set.of("payload.ingested"))
            );
            case ANNOTATION_EXTRACTION -> List.of(
                    action("store-payload", workflowType, Set.of("input.available"), Set.of("payload.stored")),
                    action("produce-annotations", workflowType, Set.of("payload.stored"), Set.of("annotations.produced"))
            );
            case HUMAN_REVIEW_APPROVAL -> List.of(
                    action("prepare-review", workflowType, Set.of("input.available"), Set.of("review.prepared")),
                    action("complete-human-review", workflowType, Set.of("review.prepared"), Set.of("human.review.completed"))
            );
        };
    }

    private static WorkflowActionDefinition action(
            String actionId,
            WorkflowType workflowType,
            Set<String> preconditions,
            Set<String> effects
    ) {
        return new WorkflowActionDefinition(
                actionId,
                actionId,
                null,
                workflowType,
                Set.of(DomainType.HR, DomainType.ACCOUNTING, DomainType.AGRICULTURE, DomainType.MEDICAL, DomainType.LOGISTICS, DomainType.LEGAL, DomainType.CUSTOM),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                preconditions,
                effects,
                Set.of(),
                Set.of(Permission.WORKFLOW_EXECUTE),
                1,
                true,
                null,
                null
        );
    }

    private static WorkflowExecutionPolicy executionPolicy(List<String> actionIds) {
        return new WorkflowExecutionPolicy(
                true,
                true,
                new RetryPolicy(3, Duration.ofSeconds(1), Duration.ofSeconds(30)),
                new BackpressurePolicy(1_000, BackpressureStrategy.DEFER_TO_QUEUE),
                actionIds
        );
    }
}
