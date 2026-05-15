package ai.datalithix.kanon.annotation.model;

import ai.datalithix.kanon.common.ActorType;
import java.time.Instant;

public record AnnotationRevision(String revisionId, String annotationId, int revisionNo, String previousValue, String newValue, ActorType changedByType, String changedById, String reason, Instant changedAt) {}
