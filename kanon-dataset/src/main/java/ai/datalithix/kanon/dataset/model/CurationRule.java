package ai.datalithix.kanon.dataset.model;

import java.util.Set;

public record CurationRule(
        String ruleId,
        String name,
        Set<String> requiredDomains,
        Set<String> requiredReviewStatuses,
        Double minConfidence,
        Set<String> excludedClassifications,
        boolean includeOnlyApproved,
        boolean includeOnlyReviewed
) {
    public CurationRule {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId is required");
        }
        requiredDomains = requiredDomains == null ? Set.of() : Set.copyOf(requiredDomains);
        requiredReviewStatuses = requiredReviewStatuses == null ? Set.of() : Set.copyOf(requiredReviewStatuses);
        excludedClassifications = excludedClassifications == null ? Set.of() : Set.copyOf(excludedClassifications);
    }
}
