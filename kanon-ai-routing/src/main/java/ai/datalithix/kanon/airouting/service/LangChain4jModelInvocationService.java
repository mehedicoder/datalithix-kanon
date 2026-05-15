package ai.datalithix.kanon.airouting.service;

import ai.datalithix.kanon.airouting.model.ModelInvocationRequest;
import ai.datalithix.kanon.airouting.model.ModelInvocationResult;
import ai.datalithix.kanon.airouting.model.ModelInvocationStatus;
import ai.datalithix.kanon.airouting.model.ModelProfile;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class LangChain4jModelInvocationService implements ModelInvocationService {
    private final ModelRegistry modelRegistry;
    private final LangChain4jChatModelProvider chatModelProvider;

    public LangChain4jModelInvocationService(ModelRegistry modelRegistry, LangChain4jChatModelProvider chatModelProvider) {
        this.modelRegistry = modelRegistry;
        this.chatModelProvider = chatModelProvider;
    }

    @Override
    public ModelInvocationResult invoke(ModelInvocationRequest request) {
        Instant startedAt = Instant.now();
        ModelProfile modelProfile = modelRegistry.findByProfileKey(request.profileKey())
                .orElseThrow(() -> new IllegalArgumentException("Unknown model profile: " + request.profileKey()));
        try {
            String prompt = prompt(request);
            Response<AiMessage> response = chatModelProvider.chatModel(modelProfile)
                    .generate(UserMessage.from(prompt));
            Instant completedAt = Instant.now();
            Map<String, String> metadata = new HashMap<>();
            if (response.tokenUsage() != null) {
                metadata.put("inputTokens", String.valueOf(response.tokenUsage().inputTokenCount()));
                metadata.put("outputTokens", String.valueOf(response.tokenUsage().outputTokenCount()));
                metadata.put("totalTokens", String.valueOf(response.tokenUsage().totalTokenCount()));
            }
            if (response.finishReason() != null) {
                metadata.put("finishReason", response.finishReason().name());
            }
            return new ModelInvocationResult(
                    request.invocationId(),
                    request.tenantId(),
                    modelProfile.profileKey(),
                    ModelInvocationStatus.COMPLETED,
                    response.content() == null ? null : response.content().text(),
                    "LangChain4j chat invocation completed",
                    null,
                    Duration.between(startedAt, completedAt),
                    "model-invocation-" + request.invocationId(),
                    metadata,
                    completedAt
            );
        } catch (RuntimeException exception) {
            Instant completedAt = Instant.now();
            return new ModelInvocationResult(
                    request.invocationId(),
                    request.tenantId(),
                    modelProfile.profileKey(),
                    ModelInvocationStatus.FAILED,
                    null,
                    "LangChain4j chat invocation failed",
                    exception.getMessage(),
                    Duration.between(startedAt, completedAt),
                    "model-invocation-" + request.invocationId(),
                    Map.of(),
                    completedAt
            );
        }
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
        throw new IllegalArgumentException("prompt, promptRef, or payloadRef is required");
    }
}
