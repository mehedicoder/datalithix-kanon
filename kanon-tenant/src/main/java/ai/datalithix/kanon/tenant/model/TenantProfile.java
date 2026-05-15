package ai.datalithix.kanon.tenant.model;

import ai.datalithix.kanon.common.DomainType;
import java.util.Map;
import java.util.Set;

public record TenantProfile(String tenantId, String displayName, DomainType domainType, String countryCode, String regulatoryAct, boolean allowCloudModels, boolean preferLocalModels, Set<String> enabledPolicies, Map<String, String> preferences) {}
