package ai.datalithix.kanon.common.security;

import ai.datalithix.kanon.common.DataResidency;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.compliance.ComplianceClassification;
import ai.datalithix.kanon.common.compliance.DataClassification;

public record SecurityDimensionSet(
        String tenantId,
        String organizationUnit,
        DomainType domainType,
        String caseId,
        String workflowId,
        String sourceTraceId,
        String mediaAssetId,
        DataClassification dataClassification,
        ComplianceClassification complianceClassification,
        DataResidency dataResidency,
        String retentionPolicy,
        String ownerId,
        String assignedUserId,
        String assignedGroupId,
        AccessPurpose purpose
) {
    public SecurityDimensionSet {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
    }
}
