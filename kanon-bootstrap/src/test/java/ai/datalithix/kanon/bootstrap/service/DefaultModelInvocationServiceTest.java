package ai.datalithix.kanon.bootstrap.service;

import ai.datalithix.kanon.airouting.model.ModelExecutionPolicy;
import ai.datalithix.kanon.airouting.model.ModelInvocationRequest;
import ai.datalithix.kanon.airouting.model.ModelInvocationStatus;
import ai.datalithix.kanon.airouting.model.ModelProfile;
import ai.datalithix.kanon.airouting.service.ModelProfileRepository;
import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.runtime.ExecutionControls;
import ai.datalithix.kanon.common.runtime.RetryPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultModelInvocationServiceTest {
    @Test
    void invokesOpenAiCompatibleApiModel() {
        CapturingRestTemplate restTemplate = new CapturingRestTemplate(Map.of(
                "choices", List.of(Map.of("message", Map.of("content", "Connection successful"))),
                "usage", Map.of("total_tokens", 12)
        ));
        DefaultModelInvocationService service = new DefaultModelInvocationService(
                new SingleModelRepository(apiProfile("custom-api", "Custom", null)),
                restTemplate
        );

        var result = service.invoke(request("custom-api"));

        assertEquals(ModelInvocationStatus.COMPLETED, result.status());
        assertEquals("Connection successful", result.outputRef());
        assertEquals("http://model.test/v1/chat/completions", restTemplate.url);
        assertEquals("12", result.metadata().get("total_tokens"));
    }

    @Test
    void failsApiConnectionWhenRequiredSecretCannotBeResolved() {
        DefaultModelInvocationService service = new DefaultModelInvocationService(
                new SingleModelRepository(apiProfile("openai-api", "OpenAI", "env:KANON_TEST_MISSING_API_KEY")),
                new CapturingRestTemplate(Map.of())
        );

        var result = service.invoke(request("openai-api"));

        assertEquals(ModelInvocationStatus.FAILED, result.status());
        assertTrue(result.failureReason().contains("API key secret reference could not be resolved"));
    }

    private static ModelInvocationRequest request(String profileKey) {
        return new ModelInvocationRequest(
                "inv-1",
                "tenant-1",
                "case-1",
                profileKey,
                AiTaskType.REASONING,
                null,
                null,
                Map.of("prompt", "hello"),
                "corr-1",
                Instant.now(),
                null
        );
    }

    private static ModelProfile apiProfile(String profileKey, String provider, String secretRef) {
        return new ModelProfile(
                profileKey,
                provider,
                "API",
                "test-model",
                "Test API Model",
                "http://model.test/v1",
                false,
                false,
                true,
                Set.of(AiTaskType.REASONING),
                "LOW",
                "LOW",
                "CLOUD",
                Set.of("test"),
                true,
                "HEALTHY",
                secretRef,
                10,
                new ModelExecutionPolicy(
                        new ExecutionControls(Duration.ofSeconds(5), 1, 1, 60),
                        new RetryPolicy(1, Duration.ZERO, Duration.ZERO),
                        null,
                        true,
                        true,
                        true
                ),
                new AuditMetadata(Instant.parse("2026-04-18T00:00:00Z"), "test@tenant-1", Instant.parse("2026-04-18T00:00:00Z"), "test@tenant-1", 1)
        );
    }

    private static class CapturingRestTemplate extends RestTemplate {
        private final Map<String, Object> response;
        private String url;

        private CapturingRestTemplate(Map<String, Object> response) {
            this.response = response;
        }

        @Override
        public <T> T postForObject(String url, Object request, Class<T> responseType, Object... uriVariables) {
            this.url = url;
            return responseType.cast(response);
        }
    }

    private record SingleModelRepository(ModelProfile profile) implements ModelProfileRepository {
        @Override
        public ModelProfile save(ModelProfile modelProfile) {
            return modelProfile;
        }

        @Override
        public Optional<ModelProfile> findByProfileKey(String tenantId, String profileKey) {
            return profile.profileKey().equals(profileKey) ? Optional.of(profile) : Optional.empty();
        }

        @Override
        public List<ModelProfile> findEnabledByTenant(String tenantId) {
            return profile.enabled() ? List.of(profile) : List.of();
        }

        @Override
        public PageResult<ModelProfile> findPage(QuerySpec query) {
            return new PageResult<>(List.of(profile), query.page().pageNumber(), query.page().pageSize(), 1);
        }
    }
}
