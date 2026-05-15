package ai.datalithix.kanon.agentruntime.model;

import java.util.Map;

public record AgentExecutionResult(String agentKey, String caseId, boolean successful, Map<String, Object> output, String summary) {}
