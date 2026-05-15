package ai.datalithix.kanon.config.service;

import ai.datalithix.kanon.config.model.KanonConfigurationPack;

public interface ConfigurationTemplateLoader {
    KanonConfigurationPack load(String location);
}
