package ai.datalithix.kanon.workflow.model;

import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.compliance.DataClassification;
import ai.datalithix.kanon.common.security.AccessControlContext;
import ai.datalithix.kanon.common.security.AccessPurpose;
import ai.datalithix.kanon.common.security.Permission;
import ai.datalithix.kanon.common.security.SecurityRole;
import ai.datalithix.kanon.policy.model.PolicyDecision;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowPlanningProblemTest {
    @Test
    void filtersInitiallyApplicableActionsUsingFactsPermissionsAndPolicy() {
        WorkflowPlanningContext context = new WorkflowPlanningContext(
                "tenant-a",
                "case-1",
                WorkflowType.ANNOTATION_EXTRACTION,
                DomainType.ACCOUNTING,
                AiTaskType.EXTRACTION,
                AssetType.DOCUMENT,
                SourceCategory.DOCUMENT,
                SourceType.FILE_UPLOAD,
                "trace-1",
                "workflow-1",
                new PolicyDecision(true, List.of("policy-accounting"), "allowed"),
                accessControlContext(),
                Map.of("regulatoryAct", "local")
        );
        WorkflowGoal goal = new WorkflowGoal(
                "goal-1",
                "Extract fields",
                WorkflowType.ANNOTATION_EXTRACTION,
                Set.of("fields.extracted"),
                Set.of("review.rejected"),
                Set.of("policy-accounting"),
                10
        );
        WorkflowActionDefinition applicable = new WorkflowActionDefinition(
                "extract-fields",
                "Extract fields",
                null,
                WorkflowType.ANNOTATION_EXTRACTION,
                Set.of(DomainType.ACCOUNTING),
                Set.of(AiTaskType.EXTRACTION),
                Set.of(AssetType.DOCUMENT),
                Set.of(SourceCategory.DOCUMENT),
                Set.of(SourceType.FILE_UPLOAD),
                Set.of("payload.stored"),
                Set.of("fields.extracted"),
                Set.of("policy-accounting"),
                Set.of(Permission.WORKFLOW_EXECUTE),
                1,
                true,
                "EXTRACTION_ANNOTATION_AGENT",
                "accounting-default"
        );
        WorkflowActionDefinition blocked = new WorkflowActionDefinition(
                "approve-review",
                "Approve review",
                null,
                WorkflowType.ANNOTATION_EXTRACTION,
                Set.of(DomainType.ACCOUNTING),
                Set.of(AiTaskType.EXTRACTION),
                Set.of(AssetType.DOCUMENT),
                Set.of(SourceCategory.DOCUMENT),
                Set.of(SourceType.FILE_UPLOAD),
                Set.of("human.reviewed"),
                Set.of("review.approved"),
                Set.of("policy-accounting"),
                Set.of(Permission.REVIEW_APPROVE),
                2,
                false,
                "REVIEW_ORCHESTRATION_AGENT",
                null
        );

        WorkflowPlanningProblem problem = new WorkflowPlanningProblem(
                "problem-1",
                context,
                goal,
                Set.of("payload.stored"),
                List.of(applicable, blocked)
        );

        assertEquals(List.of(applicable), problem.initiallyApplicableActions());
        assertTrue(goal.isSatisfiedBy(Set.of("payload.stored", "fields.extracted")));
    }

    private static AccessControlContext accessControlContext() {
        return new AccessControlContext(
                "tenant-a",
                "user-1",
                Set.of(SecurityRole.DOMAIN_MANAGER),
                Set.of(Permission.WORKFLOW_EXECUTE),
                Set.of(DomainType.ACCOUNTING),
                Set.of("case-1"),
                Set.of(DataClassification.INTERNAL),
                AccessPurpose.REVIEW
        );
    }
}
