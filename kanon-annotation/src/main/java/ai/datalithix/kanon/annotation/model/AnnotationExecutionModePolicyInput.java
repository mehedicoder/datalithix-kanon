package ai.datalithix.kanon.annotation.model;

import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import java.util.Map;
import java.util.Set;

public record AnnotationExecutionModePolicyInput(
        String tenantId,
        String caseId,
        DomainType domainType,
        AssetType assetType,
        double confidence,
        boolean highRisk,
        boolean humanSignoffRequired,
        boolean autoApprovalAllowed,
        Set<String> tenantPolicies,
        Map<String, String> attributes
) {
    public AnnotationExecutionModePolicyInput {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (domainType == null) {
            throw new IllegalArgumentException("domainType is required");
        }
        if (assetType == null) {
            throw new IllegalArgumentException("assetType is required");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
        tenantPolicies = tenantPolicies == null ? Set.of() : Set.copyOf(tenantPolicies);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
