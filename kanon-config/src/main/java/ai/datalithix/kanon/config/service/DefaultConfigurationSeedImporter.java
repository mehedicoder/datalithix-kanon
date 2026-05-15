package ai.datalithix.kanon.config.service;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.config.model.ActiveConfigurationVersion;
import ai.datalithix.kanon.config.model.ConfigurationActivationState;
import ai.datalithix.kanon.config.model.ConfigurationType;
import ai.datalithix.kanon.config.model.KanonConfigurationPack;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DefaultConfigurationSeedImporter implements ConfigurationSeedImporter {
    private final ActiveConfigurationVersionRepository repository;
    private final ConfigurationValidator validator;
    private final ConfigurationRegistry registry;

    public DefaultConfigurationSeedImporter(
            ActiveConfigurationVersionRepository repository,
            ConfigurationValidator validator,
            ConfigurationRegistry registry
    ) {
        this.repository = repository;
        this.validator = validator;
        this.registry = registry;
    }

    @Override
    public List<ActiveConfigurationVersion> importPack(
            String tenantId,
            KanonConfigurationPack pack,
            String actorId,
            String reason
    ) {
        var validation = validator.validate(pack);
        if (!validation.valid()) {
            throw new ConfigurationTemplateLoadException("Cannot import invalid configuration pack: " + pack.id());
        }

        registry.register(pack);
        Instant now = Instant.now();
        List<ActiveConfigurationVersion> versions = new ArrayList<>();
        if (pack.domains() != null) {
            pack.domains().forEach(domain -> versions.add(save(tenantId, ConfigurationType.DOMAIN, domain.id(), pack.id(), actorId, reason, now)));
        }
        if (pack.tenants() != null) {
            pack.tenants().forEach(tenant -> versions.add(save(tenant.tenantId(), ConfigurationType.TENANT, tenant.tenantId(), pack.id(), actorId, reason, now)));
        }
        if (pack.workflows() != null) {
            pack.workflows().forEach(workflow -> versions.add(save(tenantId, ConfigurationType.WORKFLOW_TEMPLATE, workflow.id(), pack.id(), actorId, reason, now)));
        }
        if (pack.agents() != null) {
            pack.agents().forEach(agent -> versions.add(save(tenantId, ConfigurationType.AGENT_DEFINITION, agent.id(), pack.id(), actorId, reason, now)));
        }
        if (pack.modelRoutingPolicies() != null) {
            pack.modelRoutingPolicies().forEach(policy -> versions.add(save(tenantId, ConfigurationType.MODEL_ROUTING_POLICY, policy.id(), pack.id(), actorId, reason, now)));
        }
        if (pack.connectors() != null) {
            pack.connectors().forEach(connector -> versions.add(save(tenantId, ConfigurationType.CONNECTOR_DEFINITION, connector.id(), pack.id(), actorId, reason, now)));
        }
        if (pack.policies() != null) {
            pack.policies().forEach(policy -> versions.add(save(tenantId, ConfigurationType.POLICY_TEMPLATE, policy.id(), pack.id(), actorId, reason, now)));
        }
        return List.copyOf(versions);
    }

    private ActiveConfigurationVersion save(
            String tenantId,
            ConfigurationType type,
            String configurationId,
            String templateId,
            String actorId,
            String reason,
            Instant now
    ) {
        ActiveConfigurationVersion version = new ActiveConfigurationVersion(
                configurationId,
                tenantId,
                type,
                templateId,
                1,
                ConfigurationActivationState.ACTIVE,
                actorId,
                now,
                null,
                null,
                reason,
                new AuditMetadata(now, actorId, now, actorId, 1)
        );
        return repository.save(version);
    }
}
