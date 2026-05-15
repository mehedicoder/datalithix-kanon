package ai.datalithix.kanon.airouting.service;

import ai.datalithix.kanon.airouting.model.ModelExecutionPolicy;
import ai.datalithix.kanon.airouting.model.ModelInvocationRequest;
import ai.datalithix.kanon.airouting.model.ModelInvocationStatus;
import ai.datalithix.kanon.airouting.model.ModelProfile;
import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.runtime.ExecutionControls;
import ai.datalithix.kanon.common.runtime.RetryPolicy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LangChain4jModelInvocationServiceTest {
    @Test
    void invokesFakeChatModel() {
        ModelProfile profile = profile();
        LangChain4jModelInvocationService service = new LangChain4jModelInvocationService(
                profileKey -> Optional.of(profile),
                modelProfile -> new FakeChatModel("accepted:" + modelProfile.profileKey())
        );

        var result = service.invoke(new ModelInvocationRequest(
                "inv-1",
                "tenant-a",
                "case-1",
                "profile-a",
                AiTaskType.EXTRACTION,
                null,
                null,
                Map.of("prompt", "extract"),
                "corr-1",
                Instant.now(),
                null
        ));

        assertEquals(ModelInvocationStatus.COMPLETED, result.status());
        assertEquals("accepted:profile-a", result.outputRef());
        assertEquals("profile-a", result.profileKey());
    }

    @Test
    void returnsFailureResultWhenModelThrows() {
        ModelProfile profile = profile();
        LangChain4jModelInvocationService service = new LangChain4jModelInvocationService(
                profileKey -> Optional.of(profile),
                modelProfile -> messages -> {
                    throw new IllegalStateException("model unavailable");
                }
        );

        var result = service.invoke(new ModelInvocationRequest(
                "inv-2",
                "tenant-a",
                "case-1",
                "profile-a",
                AiTaskType.EXTRACTION,
                "extract",
                null,
                Map.of(),
                "corr-1",
                Instant.now(),
                null
        ));

        assertEquals(ModelInvocationStatus.FAILED, result.status());
        assertEquals("model unavailable", result.failureReason());
    }

    private static ModelProfile profile() {
        return new ModelProfile(
                "profile-a",
                "fake",
                "langchain4j",
                "fake-model",
                "Fake Model",
                null,
                true,
                false,
                true,
                Set.of(AiTaskType.EXTRACTION),
                "LOW",
                "LOW",
                "LOCAL",
                Set.of("test"),
                true,
                "HEALTHY",
                null,
                1,
                new ModelExecutionPolicy(
                        new ExecutionControls(Duration.ofSeconds(5), 1, 1, 60),
                        new RetryPolicy(1, Duration.ZERO, Duration.ZERO),
                        null,
                        true,
                        true,
                        true
                ),
                null
        );
    }

    private record FakeChatModel(String response) implements ChatLanguageModel {
        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            return Response.from(AiMessage.from(response));
        }
    }
}
