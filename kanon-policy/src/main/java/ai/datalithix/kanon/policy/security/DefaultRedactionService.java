package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.security.AccessControlContext;
import org.springframework.stereotype.Component;

@Component
public class DefaultRedactionService implements RedactionService {
    private static final String DEFAULT_REPLACEMENT = "[REDACTED]";

    @Override
    public String redact(String value, AccessControlContext context, RedactionPolicy policy) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (policy == null || policy.revealPermissions() == null || policy.revealPermissions().isEmpty()) {
            return DEFAULT_REPLACEMENT;
        }
        boolean canReveal = policy.revealPermissions().stream().anyMatch(context::hasPermission);
        if (canReveal) {
            return value;
        }
        if (policy.replacement() == null || policy.replacement().isBlank()) {
            return DEFAULT_REPLACEMENT;
        }
        return policy.replacement();
    }
}
