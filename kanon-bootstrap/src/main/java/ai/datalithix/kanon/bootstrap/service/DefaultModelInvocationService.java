package ai.datalithix.kanon.bootstrap.service;

import ai.datalithix.kanon.airouting.model.ModelInvocationRequest;
import ai.datalithix.kanon.airouting.model.ModelInvocationResult;
import ai.datalithix.kanon.airouting.model.ModelInvocationStatus;
import ai.datalithix.kanon.airouting.model.ModelProfile;
import ai.datalithix.kanon.airouting.service.ModelInvocationService;
import ai.datalithix.kanon.airouting.service.ModelProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DefaultModelInvocationService implements ModelInvocationService {
    private static final Logger log = LoggerFactory.getLogger(DefaultModelInvocationService.class);
    
    private final ModelProfileRepository modelProfileRepository;
    private final RestTemplate restTemplate;

    public DefaultModelInvocationService(ModelProfileRepository modelProfileRepository) {
        this(modelProfileRepository, new RestTemplate());
    }

    @Autowired
    DefaultModelInvocationService(ModelProfileRepository modelProfileRepository, RestTemplate restTemplate) {
        this.modelProfileRepository = modelProfileRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    public ModelInvocationResult invoke(ModelInvocationRequest request) {
        Instant startTime = Instant.now();
        
        try {
            // Get model profile
            ModelProfile profile = modelProfileRepository
                    .findByProfileKey(request.tenantId(), request.profileKey())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Model profile not found: " + request.profileKey()));

            // Check if model is enabled
            if (!profile.enabled()) {
                return createFailureResult(request, startTime, "Model is disabled");
            }

            // Invoke based on backend type
            if ("LOCAL_SERVER".equals(profile.backendType())) {
                return invokeLocalServer(request, profile, startTime);
            } else if ("API".equals(profile.backendType())) {
                return invokeApi(request, profile, startTime);
            } else {
                return createFailureResult(request, startTime, 
                        "Unsupported backend type: " + profile.backendType());
            }
            
        } catch (Exception e) {
            log.error("Model invocation failed for {}: {}", request.profileKey(), e.getMessage(), e);
            return createFailureResult(request, startTime, e.getMessage());
        }
    }

    private ModelInvocationResult invokeLocalServer(
            ModelInvocationRequest request,
            ModelProfile profile,
            Instant startTime) {
        
        try {
            // Validate base URL
            if (profile.baseUrl() == null || profile.baseUrl().isBlank()) {
                return createFailureResult(request, startTime,
                        "Model base URL is not configured");
            }
            
            // For Ollama, use the /api/generate endpoint
            String url = profile.baseUrl() + "/api/generate";
            log.info("Invoking model {} at URL: {}", profile.modelId(), url);
            
            // Get prompt from parameters
            Object promptObj = request.parameters().get("prompt");
            String prompt = promptObj != null ? promptObj.toString() : "Hello";
            log.info("Using prompt: {}", prompt);
            
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", profile.modelId());
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // Make request
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            
            if (response == null) {
                return createFailureResult(request, startTime, "No response from model");
            }
            
            // Extract response
            String responseText = (String) response.get("response");
            Duration latency = Duration.between(startTime, Instant.now());
            
            // Build metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("model", profile.modelId());
            metadata.put("backend", profile.backendType());
            if (response.containsKey("total_duration")) {
                metadata.put("total_duration_ns", response.get("total_duration").toString());
            }
            if (response.containsKey("load_duration")) {
                metadata.put("load_duration_ns", response.get("load_duration").toString());
            }
            if (response.containsKey("prompt_eval_count")) {
                metadata.put("prompt_tokens", response.get("prompt_eval_count").toString());
            }
            if (response.containsKey("eval_count")) {
                metadata.put("completion_tokens", response.get("eval_count").toString());
            }
            
            return new ModelInvocationResult(
                    request.invocationId(),
                    request.tenantId(),
                    request.profileKey(),
                    ModelInvocationStatus.COMPLETED,
                    responseText,
                    "Successfully invoked " + profile.modelName(),
                    null,
                    latency,
                    null,
                    metadata,
                    Instant.now()
            );
            
        } catch (Exception e) {
            log.error("Local server invocation failed: {}", e.getMessage(), e);
            return createFailureResult(request, startTime, 
                    "Failed to invoke local server: " + e.getMessage());
        }
    }

    private ModelInvocationResult invokeApi(
            ModelInvocationRequest request, 
            ModelProfile profile, 
            Instant startTime) {
        try {
            String provider = profile.provider() == null ? "" : profile.provider().toLowerCase(Locale.ROOT);
            String apiKey = resolveSecret(profile.secretRef());
            if (requiresApiKey(provider) && apiKey.isBlank()) {
                return createFailureResult(request, startTime, "API key secret reference could not be resolved");
            }
            if (provider.contains("anthropic")) {
                return invokeAnthropicCompatibleApi(request, profile, startTime, apiKey);
            }
            return invokeOpenAiCompatibleApi(request, profile, startTime, apiKey);
        } catch (Exception e) {
            log.error("API invocation failed: {}", e.getMessage(), e);
            return createFailureResult(request, startTime, "Failed to invoke API model: " + e.getMessage());
        }
    }

    private ModelInvocationResult invokeOpenAiCompatibleApi(
            ModelInvocationRequest request,
            ModelProfile profile,
            Instant startTime,
            String apiKey) {

        String url = apiEndpoint(profile, "https://api.openai.com/v1", "/chat/completions");
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", profile.modelId());
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt(request))));
        requestBody.put("stream", false);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                url,
                new HttpEntity<>(requestBody, apiHeaders(apiKey)),
                Map.class
        );
        if (response == null) {
            return createFailureResult(request, startTime, "No response from API model");
        }

        String responseText = openAiResponseText(response);
        Map<String, String> metadata = apiMetadata(profile, response);
        return createSuccessResult(request, profile, startTime, responseText, metadata);
    }

    private ModelInvocationResult invokeAnthropicCompatibleApi(
            ModelInvocationRequest request,
            ModelProfile profile,
            Instant startTime,
            String apiKey) {

        String url = apiEndpoint(profile, "https://api.anthropic.com/v1", "/messages");
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", profile.modelId());
        requestBody.put("max_tokens", 64);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt(request))));

        HttpHeaders headers = apiHeaders(apiKey);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                url,
                new HttpEntity<>(requestBody, headers),
                Map.class
        );
        if (response == null) {
            return createFailureResult(request, startTime, "No response from API model");
        }

        String responseText = anthropicResponseText(response);
        Map<String, String> metadata = apiMetadata(profile, response);
        return createSuccessResult(request, profile, startTime, responseText, metadata);
    }

    private ModelInvocationResult createSuccessResult(
            ModelInvocationRequest request,
            ModelProfile profile,
            Instant startTime,
            String responseText,
            Map<String, String> metadata) {

        return new ModelInvocationResult(
                request.invocationId(),
                request.tenantId(),
                request.profileKey(),
                ModelInvocationStatus.COMPLETED,
                responseText,
                "Successfully invoked " + profile.modelName(),
                null,
                Duration.between(startTime, Instant.now()),
                null,
                metadata,
                Instant.now()
        );
    }

    private static HttpHeaders apiHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            headers.setBearerAuth(apiKey);
        }
        return headers;
    }

    private static boolean requiresApiKey(String provider) {
        return provider.contains("openai") || provider.contains("anthropic") || provider.contains("azure");
    }

    private static String resolveSecret(String secretRef) {
        if (secretRef == null || secretRef.isBlank()) {
            return "";
        }
        if (secretRef.startsWith("env:")) {
            String variableName = secretRef.substring("env:".length());
            String value = System.getenv(variableName);
            return value == null ? "" : value;
        }
        return "";
    }

    private static String apiEndpoint(ModelProfile profile, String defaultBaseUrl, String endpoint) {
        String configuredBaseUrl = profile.baseUrl() == null || profile.baseUrl().isBlank()
                ? defaultBaseUrl
                : profile.baseUrl();
        String normalized = configuredBaseUrl.endsWith("/")
                ? configuredBaseUrl.substring(0, configuredBaseUrl.length() - 1)
                : configuredBaseUrl;
        if (normalized.endsWith(endpoint)) {
            return normalized;
        }
        return normalized + endpoint;
    }

    private static String prompt(ModelInvocationRequest request) {
        String prompt = request.parameters().get("prompt");
        if (prompt != null && !prompt.isBlank()) {
            return prompt;
        }
        if (request.promptRef() != null && !request.promptRef().isBlank()) {
            return request.promptRef();
        }
        if (request.payloadRef() != null && !request.payloadRef().isBlank()) {
            return request.payloadRef();
        }
        return "Hello";
    }

    private static String openAiResponseText(Map<String, Object> response) {
        Object choices = response.get("choices");
        if (choices instanceof List<?> choiceList && !choiceList.isEmpty()
                && choiceList.getFirst() instanceof Map<?, ?> firstChoice
                && firstChoice.get("message") instanceof Map<?, ?> message
                && message.get("content") instanceof String content) {
            return content;
        }
        return response.toString();
    }

    private static String anthropicResponseText(Map<String, Object> response) {
        Object content = response.get("content");
        if (content instanceof List<?> contentList && !contentList.isEmpty()
                && contentList.getFirst() instanceof Map<?, ?> firstContent
                && firstContent.get("text") instanceof String text) {
            return text;
        }
        return response.toString();
    }

    private static Map<String, String> apiMetadata(ModelProfile profile, Map<String, Object> response) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("model", profile.modelId());
        metadata.put("backend", profile.backendType());
        Object usage = response.get("usage");
        if (usage instanceof Map<?, ?> usageMap) {
            usageMap.forEach((key, value) -> metadata.put(String.valueOf(key), String.valueOf(value)));
        }
        return metadata;
    }

    private ModelInvocationResult createFailureResult(
            ModelInvocationRequest request, 
            Instant startTime, 
            String reason) {
        
        Duration latency = Duration.between(startTime, Instant.now());
        
        return new ModelInvocationResult(
                request.invocationId(),
                request.tenantId(),
                request.profileKey(),
                ModelInvocationStatus.FAILED,
                null,
                null,
                reason,
                latency,
                null,
                Map.of(),
                Instant.now()
        );
    }
}
