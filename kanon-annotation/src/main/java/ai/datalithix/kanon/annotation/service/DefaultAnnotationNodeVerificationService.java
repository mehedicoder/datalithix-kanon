package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.AnnotationNodeVerificationResult;
import ai.datalithix.kanon.annotation.model.AnnotationNodeVerificationStep;
import ai.datalithix.kanon.annotation.model.AnnotationNodeVerificationStepStatus;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationNode;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationNodeStatus;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class DefaultAnnotationNodeVerificationService implements AnnotationNodeVerificationService {
    private final ExternalAnnotationSecretResolver secretResolver;
    private final Map<ai.datalithix.kanon.annotation.model.ExternalAnnotationProviderType, AnnotationProviderVerificationClient> providerClients;
    private final HttpClient httpClient;

    public DefaultAnnotationNodeVerificationService(
            ExternalAnnotationSecretResolver secretResolver,
            List<AnnotationProviderVerificationClient> providerClients
    ) {
        this(secretResolver, providerClients, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
    }

    public DefaultAnnotationNodeVerificationService(
            ExternalAnnotationSecretResolver secretResolver,
            List<AnnotationProviderVerificationClient> providerClients,
            HttpClient httpClient
    ) {
        this.secretResolver = secretResolver;
        this.providerClients = providerClients.stream()
                .collect(Collectors.toMap(AnnotationProviderVerificationClient::providerType, client -> client));
        this.httpClient = httpClient;
    }

    @Override
    public AnnotationNodeVerificationResult verify(ExternalAnnotationNode node) {
        Instant verifiedAt = Instant.now();
        long startedAt = System.nanoTime();
        try {
            VerificationTasks tasks = runVerificationTasks(node);
            AnnotationNodeVerificationStep dns = tasks.dnsStep();
            AnnotationNodeVerificationStep ping = tasks.pingStep();
            ProviderVerificationResult auth = tasks.providerResult();
            List<AnnotationNodeVerificationStep> steps = List.of(dns, ping, auth.authenticationStep());
            ExternalAnnotationNodeStatus status = resultingStatus(steps);
            long totalLatencyMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            return new AnnotationNodeVerificationResult(node.nodeId(), status, steps, auth.version(), totalLatencyMs, verifiedAt);
        } catch (Exception exception) {
            long totalLatencyMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            AnnotationNodeVerificationStep failedStep = new AnnotationNodeVerificationStep(
                    "dry-run",
                    AnnotationNodeVerificationStepStatus.FAILED,
                    exception.getClass().getSimpleName(),
                    totalLatencyMs
            );
            return new AnnotationNodeVerificationResult(
                    node.nodeId(),
                    ExternalAnnotationNodeStatus.OFFLINE,
                    List.of(failedStep),
                    null,
                    totalLatencyMs,
                    verifiedAt
            );
        }
    }

    private VerificationTasks runVerificationTasks(ExternalAnnotationNode node) throws ExecutionException, InterruptedException {
        // StructuredTaskScope is a preview API in some JDK builds; keep runtime behavior equivalent
        // with virtual-thread fan-out/fan-in when preview is not enabled.
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<AnnotationNodeVerificationStep> dns = executor.submit(() -> dnsStep(node.baseUrl()));
            Future<AnnotationNodeVerificationStep> ping = executor.submit(() -> pingStep(node.baseUrl()));
            Future<ProviderVerificationResult> auth = executor.submit(() -> providerClient(node).verify(node, secretResolver.resolve(node.secretRef())));
            return new VerificationTasks(dns.get(), ping.get(), auth.get());
        }
    }

    private AnnotationProviderVerificationClient providerClient(ExternalAnnotationNode node) {
        AnnotationProviderVerificationClient client = providerClients.get(node.providerType());
        if (client == null) {
            throw new IllegalStateException("No verification client registered for provider " + node.providerType());
        }
        return client;
    }

    private AnnotationNodeVerificationStep dnsStep(String baseUrl) {
        long startedAt = System.nanoTime();
        try {
            InetAddress.getByName(URI.create(baseUrl).getHost());
            long latencyMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            return new AnnotationNodeVerificationStep("dns-resolve", AnnotationNodeVerificationStepStatus.PASSED, "Resolved", latencyMs);
        } catch (Exception exception) {
            long latencyMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            return new AnnotationNodeVerificationStep("dns-resolve", AnnotationNodeVerificationStepStatus.FAILED, exception.getClass().getSimpleName(), latencyMs);
        }
    }

    private AnnotationNodeVerificationStep pingStep(String baseUrl) {
        long startedAt = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(3))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            long latencyMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            if (response.statusCode() >= 200 && response.statusCode() < 500) {
                return new AnnotationNodeVerificationStep("ping", AnnotationNodeVerificationStepStatus.PASSED, "Reachable", latencyMs);
            }
            return new AnnotationNodeVerificationStep("ping", AnnotationNodeVerificationStepStatus.FAILED, "HTTP " + response.statusCode(), latencyMs);
        } catch (Exception exception) {
            long latencyMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            return new AnnotationNodeVerificationStep("ping", AnnotationNodeVerificationStepStatus.FAILED, exception.getClass().getSimpleName(), latencyMs);
        }
    }

    private static ExternalAnnotationNodeStatus resultingStatus(List<AnnotationNodeVerificationStep> steps) {
        AnnotationNodeVerificationStep authStep = steps.stream()
                .filter(step -> "api-authentication".equals(step.stepName()))
                .findFirst()
                .orElse(null);
        if (authStep != null && authStep.status() == AnnotationNodeVerificationStepStatus.FAILED
                && authStep.detail() != null && authStep.detail().contains("401")) {
            return ExternalAnnotationNodeStatus.UNAUTHORIZED;
        }
        boolean failed = steps.stream().anyMatch(step -> step.status() == AnnotationNodeVerificationStepStatus.FAILED);
        return failed ? ExternalAnnotationNodeStatus.OFFLINE : ExternalAnnotationNodeStatus.ACTIVE;
    }

    private record VerificationTasks(
            AnnotationNodeVerificationStep dnsStep,
            AnnotationNodeVerificationStep pingStep,
            ProviderVerificationResult providerResult
    ) {
    }
}
