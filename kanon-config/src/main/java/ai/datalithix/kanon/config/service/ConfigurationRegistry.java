package ai.datalithix.kanon.config.service;

import ai.datalithix.kanon.config.model.AgentDefinition;
import ai.datalithix.kanon.config.model.ConnectorDefinition;
import ai.datalithix.kanon.config.model.DomainConfiguration;
import ai.datalithix.kanon.config.model.ModelRoutingPolicy;
import ai.datalithix.kanon.config.model.KanonConfigurationPack;
import ai.datalithix.kanon.config.model.PolicyTemplate;
import ai.datalithix.kanon.config.model.TenantConfiguration;
import ai.datalithix.kanon.config.model.WorkflowTemplate;
import java.util.List;
import java.util.Optional;

public interface ConfigurationRegistry {
    void register(KanonConfigurationPack pack);

    Optional<KanonConfigurationPack> pack(String id);

    Optional<DomainConfiguration> domain(String id);

    Optional<TenantConfiguration> tenant(String tenantId);

    Optional<WorkflowTemplate> workflow(String id);

    Optional<AgentDefinition> agent(String id);

    Optional<ModelRoutingPolicy> modelRoutingPolicy(String id);

    Optional<ConnectorDefinition> connector(String id);

    Optional<PolicyTemplate> policy(String id);

    List<KanonConfigurationPack> packs();

    List<DomainConfiguration> domains();

    List<TenantConfiguration> tenants();

    List<WorkflowTemplate> workflows();

    List<AgentDefinition> agents();

    List<ModelRoutingPolicy> modelRoutingPolicies();

    List<ConnectorDefinition> connectors();

    List<PolicyTemplate> policies();
}
