package ai.datalithix.kanon.config.service;

import ai.datalithix.kanon.config.model.ActiveConfigurationVersion;
import ai.datalithix.kanon.config.model.ConfigurationActivationState;
import ai.datalithix.kanon.config.model.ConfigurationType;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryActiveConfigurationVersionRepository implements ActiveConfigurationVersionRepository {
    private final Map<Key, ActiveConfigurationVersion> activeVersions = new ConcurrentHashMap<>();

    @Override
    public Optional<ActiveConfigurationVersion> findActive(
            String tenantId,
            ConfigurationType configurationType,
            String configurationId
    ) {
        return Optional.ofNullable(activeVersions.get(new Key(tenantId, configurationType, configurationId)))
                .filter(version -> version.activationState() == ConfigurationActivationState.ACTIVE);
    }

    @Override
    public List<ActiveConfigurationVersion> findActiveByTenant(String tenantId) {
        return activeVersions.values().stream()
                .filter(version -> version.activationState() == ConfigurationActivationState.ACTIVE)
                .filter(version -> version.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(ActiveConfigurationVersion::configurationType)
                        .thenComparing(ActiveConfigurationVersion::configurationId))
                .toList();
    }

    @Override
    public List<ActiveConfigurationVersion> findAllActive() {
        return activeVersions.values().stream()
                .filter(version -> version.activationState() == ConfigurationActivationState.ACTIVE)
                .sorted(Comparator.comparing(ActiveConfigurationVersion::tenantId)
                        .thenComparing(ActiveConfigurationVersion::configurationType)
                        .thenComparing(ActiveConfigurationVersion::configurationId))
                .toList();
    }

    @Override
    public ActiveConfigurationVersion save(ActiveConfigurationVersion version) {
        activeVersions.put(new Key(version.tenantId(), version.configurationType(), version.configurationId()), version);
        return version;
    }

    private record Key(String tenantId, ConfigurationType configurationType, String configurationId) {}
}
