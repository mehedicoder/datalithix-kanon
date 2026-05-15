package ai.datalithix.kanon.annotation.service;

public interface ExternalAnnotationNodeUsageGuard {
    long countActiveNonSyncedTasks(String tenantId, String nodeId);
}
