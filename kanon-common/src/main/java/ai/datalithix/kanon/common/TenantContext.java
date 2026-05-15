package ai.datalithix.kanon.common;

import java.util.Set;

public record TenantContext(
        String tenantId,
        DomainType domainType,
        String countryCode,
        String regulatoryAct,
        boolean allowCloudModels,
        boolean preferLocalModels,
        Set<String> enabledPolicies
) {}
