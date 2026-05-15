package ai.datalithix.kanon.annotation.service;

public interface ExternalAnnotationSecretResolver {
    String resolve(String secretRef);
}
