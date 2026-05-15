package ai.datalithix.kanon.config.service;

public class ConfigurationTemplateLoadException extends RuntimeException {
    public ConfigurationTemplateLoadException(String message) {
        super(message);
    }

    public ConfigurationTemplateLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
