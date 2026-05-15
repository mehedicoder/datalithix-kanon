package ai.datalithix.kanon.config.service;

import ai.datalithix.kanon.config.model.KanonConfigurationPack;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class YamlConfigurationTemplateLoader implements ConfigurationTemplateLoader {
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public YamlConfigurationTemplateLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = new ObjectMapper(new YAMLFactory())
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    @Override
    public KanonConfigurationPack load(String location) {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new ConfigurationTemplateLoadException("Configuration template not found: " + location);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, KanonConfigurationPack.class);
        } catch (IOException ex) {
            throw new ConfigurationTemplateLoadException("Failed to load configuration template: " + location, ex);
        }
    }
}
