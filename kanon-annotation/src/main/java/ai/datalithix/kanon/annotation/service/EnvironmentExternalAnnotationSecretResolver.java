package ai.datalithix.kanon.annotation.service;

public class EnvironmentExternalAnnotationSecretResolver implements ExternalAnnotationSecretResolver {
    @Override
    public String resolve(String secretRef) {
        if (secretRef == null || secretRef.isBlank()) {
            throw new IllegalArgumentException("secretRef is required");
        }
        if (secretRef.startsWith("env:")) {
            String envKey = secretRef.substring("env:".length());
            String value = System.getenv(envKey);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Secret reference could not be resolved");
            }
            return value;
        }
        throw new IllegalStateException("Unsupported secret reference scheme");
    }
}
