package ai.datalithix.kanon.ui.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SensitiveTextClassifierTest {
    @Test
    void redactsKnownSensitiveFieldNames() {
        assertTrue(SensitiveTextClassifier.shouldRedact(RedactionContext.SENSITIVE_FIELD, "api_key", "abc"));
        assertTrue(SensitiveTextClassifier.shouldRedact(RedactionContext.SENSITIVE_FIELD, "user.password", "abc"));
        assertTrue(SensitiveTextClassifier.shouldRedact(RedactionContext.SENSITIVE_FIELD, "authorizationToken", "abc"));
    }

    @Test
    void redactsSensitiveValuePatterns() {
        assertTrue(SensitiveTextClassifier.shouldRedact(RedactionContext.SENSITIVE_FIELD, "comment", "email me at admin@example.com"));
        assertTrue(SensitiveTextClassifier.shouldRedact(RedactionContext.SENSITIVE_FIELD, "header", "Bearer abcdefghijklmnop"));
        assertTrue(SensitiveTextClassifier.shouldRedact(RedactionContext.SENSITIVE_FIELD, "config", "secret=abc123"));
    }

    @Test
    void keepsOrdinarySensitiveFieldContextVisible() {
        assertFalse(SensitiveTextClassifier.shouldRedact(RedactionContext.SENSITIVE_FIELD, "status", "APPROVED"));
    }

    @Test
    void alwaysRedactsExplicitSecretPromptResponseAndPayloadPreviewContexts() {
        assertTrue(SensitiveTextClassifier.shouldRedact(RedactionContext.SECRET, "name", "value"));
        assertTrue(SensitiveTextClassifier.shouldRedact(RedactionContext.PROMPT, "prompt", "ordinary prompt"));
        assertTrue(SensitiveTextClassifier.shouldRedact(RedactionContext.RESPONSE, "response", "ordinary response"));
        assertTrue(SensitiveTextClassifier.shouldRedact(RedactionContext.PAYLOAD_PREVIEW, "payload", "ordinary payload"));
    }
}
