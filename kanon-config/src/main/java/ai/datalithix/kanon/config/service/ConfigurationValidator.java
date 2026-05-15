package ai.datalithix.kanon.config.service;

import ai.datalithix.kanon.config.model.ConfigValidationResult;
import ai.datalithix.kanon.config.model.KanonConfigurationPack;

public interface ConfigurationValidator {
    ConfigValidationResult validate(KanonConfigurationPack pack);
}
