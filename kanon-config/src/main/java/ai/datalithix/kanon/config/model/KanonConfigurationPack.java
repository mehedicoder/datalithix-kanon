package ai.datalithix.kanon.config.model;

import java.util.List;

public record KanonConfigurationPack(
        String id,
        String displayName,
        List<DomainConfiguration> domains,
        List<TenantConfiguration> tenants,
        List<WorkflowTemplate> workflows,
        List<AgentDefinition> agents,
        List<ModelRoutingPolicy> modelRoutingPolicies,
        List<ConnectorDefinition> connectors,
        List<PolicyTemplate> policies
) {}
