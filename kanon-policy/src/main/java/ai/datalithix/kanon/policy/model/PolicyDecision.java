package ai.datalithix.kanon.policy.model;

import java.util.List;

public record PolicyDecision(boolean allowed, List<String> activePolicies, String rationale) {}
