package ai.datalithix.kanon.agentruntime.model;

import java.util.Set;

public record AgentDescriptor(String agentKey, String displayName, Set<String> supportedDomains, Set<String> supportedTasks) {}
