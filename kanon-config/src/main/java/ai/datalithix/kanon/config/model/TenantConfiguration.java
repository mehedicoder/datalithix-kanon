package ai.datalithix.kanon.config.model;

import ai.datalithix.kanon.common.DataResidency;
import ai.datalithix.kanon.common.DomainType;
import java.util.Map;
import java.util.Set;

public record TenantConfiguration(
        String tenantId,
        String displayName,
        DomainType domainType,
        String countryCode,
        String regulatoryAct,
        DataResidency dataResidency,
        boolean allowCloudModels,
        boolean preferLocalModels,
        Set<String> enabledPolicyIds,
        Set<String> enabledConnectorIds,
        String modelRoutingPolicyId,
        Map<String, String> preferences
) {}
