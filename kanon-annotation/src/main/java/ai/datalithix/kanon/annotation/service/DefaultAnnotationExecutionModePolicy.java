package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationExecutionMode;
import ai.datalithix.kanon.annotation.model.AnnotationExecutionModeDecision;
import ai.datalithix.kanon.annotation.model.AnnotationExecutionModePolicyInput;
import ai.datalithix.kanon.annotation.model.AnnotationExecutionNodeType;
import ai.datalithix.kanon.common.AssetType;
import java.util.Map;

public class DefaultAnnotationExecutionModePolicy implements AnnotationExecutionModePolicy {
    private static final double DEFAULT_AUTO_APPROVAL_THRESHOLD = 0.95;
    private static final double DEFAULT_HUMAN_REVIEW_THRESHOLD = 0.75;

    @Override
    public AnnotationExecutionModeDecision decide(AnnotationExecutionModePolicyInput input) {
        if (input.humanSignoffRequired() || input.highRisk() || input.tenantPolicies().contains("MANDATORY_HUMAN_REVIEW")) {
            return decision(
                    AnnotationExecutionMode.MANDATORY_HUMAN,
                    true,
                    nodeType(input.assetType()),
                    "Mandatory human signoff required by risk, tenant policy, or task context",
                    input
            );
        }
        if (input.autoApprovalAllowed()
                && input.confidence() >= autoApprovalThreshold(input)
                && !input.tenantPolicies().contains("DISABLE_AUTO_APPROVAL")) {
            return decision(
                    AnnotationExecutionMode.FULL_AUTONOMOUS,
                    false,
                    null,
                    "Confidence and policy allow autonomous approval",
                    input
            );
        }
        return decision(
                AnnotationExecutionMode.HUMAN_REVIEW,
                true,
                nodeType(input.assetType()),
                input.confidence() < humanReviewThreshold(input)
                        ? "Confidence is below human review threshold"
                        : "Human review selected by policy",
                input
        );
    }

    private static AnnotationExecutionModeDecision decision(
            AnnotationExecutionMode mode,
            boolean externalTaskRequired,
            AnnotationExecutionNodeType preferredNodeType,
            String rationale,
            AnnotationExecutionModePolicyInput input
    ) {
        return new AnnotationExecutionModeDecision(
                mode,
                externalTaskRequired,
                preferredNodeType,
                rationale,
                Map.of(
                        "tenantId", input.tenantId(),
                        "caseId", input.caseId() == null ? "" : input.caseId(),
                        "domainType", input.domainType().name(),
                        "assetType", input.assetType().name(),
                        "confidence", Double.toString(input.confidence())
                )
        );
    }

    private static AnnotationExecutionNodeType nodeType(AssetType assetType) {
        return switch (assetType) {
            case DOCUMENT, AUDIO, DATASET, FORM, EMAIL -> AnnotationExecutionNodeType.LABEL_STUDIO;
            case IMAGE, VIDEO, SENSOR_READING, TELEMETRY -> AnnotationExecutionNodeType.CVAT;
            case ANNOTATION, UNKNOWN -> null;
        };
    }

    private static double autoApprovalThreshold(AnnotationExecutionModePolicyInput input) {
        return threshold(input.attributes().get("autoApprovalThreshold"), DEFAULT_AUTO_APPROVAL_THRESHOLD);
    }

    private static double humanReviewThreshold(AnnotationExecutionModePolicyInput input) {
        return threshold(input.attributes().get("humanReviewThreshold"), DEFAULT_HUMAN_REVIEW_THRESHOLD);
    }

    private static double threshold(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < 0.0 || parsed > 1.0) {
                return fallback;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
