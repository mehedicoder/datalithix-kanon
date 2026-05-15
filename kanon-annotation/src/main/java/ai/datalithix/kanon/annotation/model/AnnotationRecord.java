package ai.datalithix.kanon.annotation.model;

public record AnnotationRecord(String annotationId, String caseId, String fieldName, String currentValue, String status, int revisionNo) {}
