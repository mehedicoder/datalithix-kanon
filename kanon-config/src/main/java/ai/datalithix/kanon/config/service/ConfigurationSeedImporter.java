package ai.datalithix.kanon.config.service;

import ai.datalithix.kanon.config.model.ActiveConfigurationVersion;
import ai.datalithix.kanon.config.model.KanonConfigurationPack;
import java.util.List;

public interface ConfigurationSeedImporter {
    List<ActiveConfigurationVersion> importPack(String tenantId, KanonConfigurationPack pack, String actorId, String reason);
}
