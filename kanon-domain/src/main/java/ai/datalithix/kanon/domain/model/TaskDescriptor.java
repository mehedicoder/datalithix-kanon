package ai.datalithix.kanon.domain.model;

import ai.datalithix.kanon.common.AiTaskType;

public record TaskDescriptor(AiTaskType taskType, String caseId, String inputRef, String schemaVersion, boolean highRisk) {}
