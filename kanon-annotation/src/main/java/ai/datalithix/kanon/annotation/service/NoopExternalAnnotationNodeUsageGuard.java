package ai.datalithix.kanon.annotation.service;

public class NoopExternalAnnotationNodeUsageGuard implements ExternalAnnotationNodeUsageGuard {
    @Override
    public long countActiveNonSyncedTasks(String tenantId, String nodeId) {
        return 0;
    }
}
