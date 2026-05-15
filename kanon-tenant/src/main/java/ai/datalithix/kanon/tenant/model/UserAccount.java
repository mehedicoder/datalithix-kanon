package ai.datalithix.kanon.tenant.model;

import ai.datalithix.kanon.common.model.AuditMetadata;

public record UserAccount(
        String userId,
        String username,
        String email,
        String displayName,
        String passwordHash,
        String preferredLocale,
        GovernanceStatus status,
        boolean systemUser,
        AuditMetadata audit
) {
    public UserAccount {
        require(userId, "userId");
        require(username, "username");
        require(email, "email");
        require(displayName, "displayName");
        require(passwordHash, "passwordHash");
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (audit == null) {
            throw new IllegalArgumentException("audit is required");
        }
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
