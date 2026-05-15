package ai.datalithix.kanon.config.service;

import ai.datalithix.kanon.config.model.ActiveConfigurationVersion;
import ai.datalithix.kanon.config.model.ConfigurationType;
import java.util.List;
import java.util.Optional;

public interface ActiveConfigurationVersionRepository {
    Optional<ActiveConfigurationVersion> findActive(String tenantId, ConfigurationType configurationType, String configurationId);

    List<ActiveConfigurationVersion> findActiveByTenant(String tenantId);

    List<ActiveConfigurationVersion> findAllActive();

    ActiveConfigurationVersion save(ActiveConfigurationVersion version);
}
