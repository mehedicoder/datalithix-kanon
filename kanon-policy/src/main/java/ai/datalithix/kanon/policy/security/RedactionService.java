package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.security.AccessControlContext;

public interface RedactionService {
    String redact(String value, AccessControlContext context, RedactionPolicy policy);
}
