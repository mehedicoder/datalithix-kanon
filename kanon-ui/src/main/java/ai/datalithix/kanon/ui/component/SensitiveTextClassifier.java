package ai.datalithix.kanon.ui.component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class SensitiveTextClassifier {
    private static final Set<String> SENSITIVE_KEY_TOKENS = Set.of(
            "password",
            "passwd",
            "secret",
            "token",
            "apikey",
            "api_key",
            "authorization",
            "credential",
            "privatekey",
            "private_key",
            "ssn",
            "iban",
            "creditcard",
            "credit_card"
    );
    private static final Pattern EMAIL = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)bearer\\s+[a-z0-9._~+/=-]{12,}");
    private static final Pattern API_KEY_ASSIGNMENT = Pattern.compile("(?i)(api[_-]?key|secret|token|password)\\s*[:=]\\s*[^\\s,;}{]+");

    private SensitiveTextClassifier() {}

    public static boolean shouldRedact(RedactionContext context, String fieldName, String value) {
        if (context == RedactionContext.SECRET
                || context == RedactionContext.PROMPT
                || context == RedactionContext.RESPONSE
                || context == RedactionContext.PAYLOAD_PREVIEW) {
            return true;
        }
        return hasSensitiveKey(fieldName) || hasSensitiveValue(value);
    }

    public static boolean hasSensitiveKey(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }
        String normalized = fieldName.toLowerCase(Locale.ROOT).replace("-", "_").replace(".", "_");
        String compact = normalized.replace("_", "");
        return SENSITIVE_KEY_TOKENS.stream().anyMatch(token -> normalized.contains(token) || compact.contains(token));
    }

    public static boolean hasSensitiveValue(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return EMAIL.matcher(value).find()
                || BEARER_TOKEN.matcher(value).find()
                || API_KEY_ASSIGNMENT.matcher(value).find();
    }
}
