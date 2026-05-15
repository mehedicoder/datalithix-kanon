package ai.datalithix.kanon.config.service;

import ai.datalithix.kanon.config.model.AgentDefinition;
import ai.datalithix.kanon.config.model.ConnectorDefinition;
import ai.datalithix.kanon.config.model.DomainConfiguration;
import ai.datalithix.kanon.config.model.ModelRoutingPolicy;
import ai.datalithix.kanon.config.model.KanonConfigurationPack;
import ai.datalithix.kanon.config.model.PolicyTemplate;
import ai.datalithix.kanon.config.model.TenantConfiguration;
import ai.datalithix.kanon.config.model.WorkflowTemplate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryConfigurationRegistry implements ConfigurationRegistry {
    private final Map<String, KanonConfigurationPack> packs = new ConcurrentHashMap<>();
    private final Map<String, DomainConfiguration> domains = new ConcurrentHashMap<>();
    private final Map<String, TenantConfiguration> tenants = new ConcurrentHashMap<>();
    private final Map<String, WorkflowTemplate> workflows = new ConcurrentHashMap<>();
    private final Map<String, AgentDefinition> agents = new ConcurrentHashMap<>();
    private final Map<String, ModelRoutingPolicy> modelRoutingPolicies = new ConcurrentHashMap<>();
    private final Map<String, ConnectorDefinition> connectors = new ConcurrentHashMap<>();
    private final Map<String, PolicyTemplate> policies = new ConcurrentHashMap<>();

    @Override
    public void register(KanonConfigurationPack pack) {
        packs.put(pack.id(), pack);
        if (pack.domains() != null) {
            pack.domains().forEach(domain -> domains.put(domain.id(), domain));
        }
        if (pack.tenants() != null) {
            pack.tenants().forEach(tenant -> tenants.put(tenant.tenantId(), tenant));
        }
        if (pack.workflows() != null) {
            pack.workflows().forEach(workflow -> workflows.put(workflow.id(), workflow));
        }
        if (pack.agents() != null) {
            pack.agents().forEach(agent -> agents.put(agent.id(), agent));
        }
        if (pack.modelRoutingPolicies() != null) {
            pack.modelRoutingPolicies().forEach(policy -> modelRoutingPolicies.put(policy.id(), policy));
        }
        if (pack.connectors() != null) {
            pack.connectors().forEach(connector -> connectors.put(connector.id(), connector));
        }
        if (pack.policies() != null) {
            pack.policies().forEach(policy -> policies.put(policy.id(), policy));
        }
    }

    @Override
    public Optional<KanonConfigurationPack> pack(String id) {
        return Optional.ofNullable(packs.get(id));
    }

    @Override
    public Optional<DomainConfiguration> domain(String id) {
        return Optional.ofNullable(domains.get(id));
    }

    @Override
    public Optional<TenantConfiguration> tenant(String tenantId) {
        return Optional.ofNullable(tenants.get(tenantId));
    }

    @Override
    public Optional<WorkflowTemplate> workflow(String id) {
        return Optional.ofNullable(workflows.get(id));
    }

    @Override
    public Optional<AgentDefinition> agent(String id) {
        return Optional.ofNullable(agents.get(id));
    }

    @Override
    public Optional<ModelRoutingPolicy> modelRoutingPolicy(String id) {
        return Optional.ofNullable(modelRoutingPolicies.get(id));
    }

    @Override
    public Optional<ConnectorDefinition> connector(String id) {
        return Optional.ofNullable(connectors.get(id));
    }

    @Override
    public Optional<PolicyTemplate> policy(String id) {
        return Optional.ofNullable(policies.get(id));
    }

    @Override
    public List<KanonConfigurationPack> packs() {
        return packs.values().stream()
                .sorted(Comparator.comparing(KanonConfigurationPack::id))
                .toList();
    }

    @Override
    public List<DomainConfiguration> domains() {
        return domains.values().stream()
                .sorted(Comparator.comparing(DomainConfiguration::id))
                .toList();
    }

    @Override
    public List<TenantConfiguration> tenants() {
        return tenants.values().stream()
                .sorted(Comparator.comparing(TenantConfiguration::tenantId))
                .toList();
    }

    @Override
    public List<WorkflowTemplate> workflows() {
        return workflows.values().stream()
                .sorted(Comparator.comparing(WorkflowTemplate::id))
                .toList();
    }

    @Override
    public List<AgentDefinition> agents() {
        return agents.values().stream()
                .sorted(Comparator.comparing(AgentDefinition::id))
                .toList();
    }

    @Override
    public List<ModelRoutingPolicy> modelRoutingPolicies() {
        return modelRoutingPolicies.values().stream()
                .sorted(Comparator.comparing(ModelRoutingPolicy::id))
                .toList();
    }

    @Override
    public List<ConnectorDefinition> connectors() {
        return connectors.values().stream()
                .sorted(Comparator.comparing(ConnectorDefinition::id))
                .toList();
    }

    @Override
    public List<PolicyTemplate> policies() {
        return policies.values().stream()
                .sorted(Comparator.comparing(PolicyTemplate::id))
                .toList();
    }
}
