package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.DataResidency;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.compliance.ComplianceClassification;
import ai.datalithix.kanon.common.compliance.DataClassification;
import ai.datalithix.kanon.common.security.AccessPurpose;
import ai.datalithix.kanon.common.security.SecurityDimensionSet;

public record ProtectedResource(
        String resourceType,
        String resourceId,
        SecurityDimensionSet dimensions
) {
    public ProtectedResource(
            String resourceType,
            String resourceId,
            String tenantId,
            DomainType domainType,
            String caseId,
            String ownerId,
            String assignedUserId,
            DataClassification dataClassification
    ) {
        this(resourceType, resourceId, new SecurityDimensionSet(
                tenantId,
                null,
                domainType,
                caseId,
                null,
                null,
                null,
                dataClassification,
                ComplianceClassification.NONE,
                DataResidency.UNKNOWN,
                null,
                ownerId,
                assignedUserId,
                null,
                AccessPurpose.REVIEW
        ));
    }

    public ProtectedResource {
        if (resourceType == null || resourceType.isBlank()) {
            throw new IllegalArgumentException("resourceType is required");
        }
        if (resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException("resourceId is required");
        }
        if (dimensions == null) {
            throw new IllegalArgumentException("dimensions are required");
        }
    }
}
