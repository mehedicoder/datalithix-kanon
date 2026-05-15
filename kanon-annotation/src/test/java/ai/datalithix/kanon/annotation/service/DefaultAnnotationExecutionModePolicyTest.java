package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationExecutionMode;
import ai.datalithix.kanon.annotation.model.AnnotationExecutionModePolicyInput;
import ai.datalithix.kanon.annotation.model.AnnotationExecutionNodeType;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAnnotationExecutionModePolicyTest {
    private final DefaultAnnotationExecutionModePolicy policy = new DefaultAnnotationExecutionModePolicy();

    @Test
    void selectsFullAutonomousWhenConfidenceAndPolicyAllowIt() {
        var decision = policy.decide(input(AssetType.DOCUMENT, 0.98, false, false, true, Set.of()));

        assertEquals(AnnotationExecutionMode.FULL_AUTONOMOUS, decision.mode());
        assertFalse(decision.externalTaskRequired());
        assertEquals(null, decision.preferredNodeType());
    }

    @Test
    void selectsMandatoryHumanWhenRiskRequiresSignoff() {
        var decision = policy.decide(input(AssetType.VIDEO, 0.99, true, false, true, Set.of()));

        assertEquals(AnnotationExecutionMode.MANDATORY_HUMAN, decision.mode());
        assertTrue(decision.externalTaskRequired());
        assertEquals(AnnotationExecutionNodeType.CVAT, decision.preferredNodeType());
    }

    @Test
    void selectsHumanReviewForLowerConfidenceDocumentTask() {
        var decision = policy.decide(input(AssetType.FORM, 0.80, false, false, true, Set.of()));

        assertEquals(AnnotationExecutionMode.HUMAN_REVIEW, decision.mode());
        assertTrue(decision.externalTaskRequired());
        assertEquals(AnnotationExecutionNodeType.LABEL_STUDIO, decision.preferredNodeType());
    }

    private static AnnotationExecutionModePolicyInput input(
            AssetType assetType,
            double confidence,
            boolean highRisk,
            boolean humanSignoffRequired,
            boolean autoApprovalAllowed,
            Set<String> tenantPolicies
    ) {
        return new AnnotationExecutionModePolicyInput(
                "tenant-a",
                "case-1",
                DomainType.ACCOUNTING,
                assetType,
                confidence,
                highRisk,
                humanSignoffRequired,
                autoApprovalAllowed,
                tenantPolicies,
                Map.of()
        );
    }
}
