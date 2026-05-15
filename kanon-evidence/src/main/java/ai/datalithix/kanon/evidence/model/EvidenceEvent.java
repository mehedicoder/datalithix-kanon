package ai.datalithix.kanon.evidence.model;

import ai.datalithix.kanon.common.ActorType;
import java.time.Instant;
import java.util.Map;

public record EvidenceEvent(String eventId, String tenantId, String caseId, String eventType, ActorType actorType, String actorId, String agentKey, String modelProfile, String policyVersion, Map<String, Object> beforeState, Map<String, Object> afterState, String rationale, Instant occurredAt) {}
