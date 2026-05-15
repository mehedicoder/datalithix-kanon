package ai.datalithix.kanon.annotation.service.labelstudio;

import ai.datalithix.kanon.annotation.model.AnnotationNodeVerificationStep;
import ai.datalithix.kanon.annotation.model.AnnotationNodeVerificationStepStatus;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationNode;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationProviderType;
import ai.datalithix.kanon.annotation.service.AnnotationProviderVerificationClient;
import ai.datalithix.kanon.annotation.service.ProviderVerificationResult;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LabelStudioVerificationClient implements AnnotationProviderVerificationClient {
    private final HttpClient httpClient;

    public LabelStudioVerificationClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public ExternalAnnotationProviderType providerType() {
        return ExternalAnnotationProviderType.LABEL_STUDIO;
    }

    @Override
    public ProviderVerificationResult verify(ExternalAnnotationNode node, String resolvedSecret) {
        long startedAt = System.nanoTime();
        try {
            URI endpoint = URI.create(normalizeBaseUrl(node.baseUrl()) + "/api/projects/");
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Token " + resolvedSecret)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                return new ProviderVerificationResult(
                        new AnnotationNodeVerificationStep("api-authentication", AnnotationNodeVerificationStepStatus.FAILED, "401 Unauthorized", latencyMs),
                        null
                );
            }
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String version = response.headers().firstValue("X-Label-Studio-Version").orElse("unknown");
                return new ProviderVerificationResult(
                        new AnnotationNodeVerificationStep("api-authentication", AnnotationNodeVerificationStepStatus.PASSED, "Authenticated", latencyMs),
                        version
                );
            }
            return new ProviderVerificationResult(
                    new AnnotationNodeVerificationStep("api-authentication", AnnotationNodeVerificationStepStatus.FAILED, "HTTP " + response.statusCode(), latencyMs),
                    null
            );
        } catch (Exception exception) {
            long latencyMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            return new ProviderVerificationResult(
                    new AnnotationNodeVerificationStep("api-authentication", AnnotationNodeVerificationStepStatus.FAILED, exception.getClass().getSimpleName(), latencyMs),
                    null
            );
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
