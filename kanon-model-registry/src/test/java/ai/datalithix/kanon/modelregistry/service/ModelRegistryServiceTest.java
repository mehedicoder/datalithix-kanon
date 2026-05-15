package ai.datalithix.kanon.modelregistry.service;

import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import ai.datalithix.kanon.modelregistry.model.ModelEntry;
import ai.datalithix.kanon.modelregistry.model.ModelLifecycleStage;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelRegistryServiceTest {
    private final InMemoryModelRegistryRepository repository = new InMemoryModelRegistryRepository();
    private final CapturingEvidenceLedger ledger = new CapturingEvidenceLedger();
    private final DefaultModelRegistryService service = new DefaultModelRegistryService(repository, ledger);

    @Test
    void registersNewModel() {
        ModelEntry entry = service.registerModel("tenant-1", "test-model", "pytorch",
                "CLASSIFICATION", "HR", "s3://models/test/v1/model.pt",
                "tj-1", "dsv-1", "user-1");

        assertNotNull(entry);
        assertEquals("test-model", entry.modelName());
        assertEquals(1, entry.latestVersionNumber());
        assertEquals(ModelLifecycleStage.DEVELOPMENT.name(), entry.latestLifecycleStage());
    }

    @Test
    void registersModelWithInitialVersion() {
        service.registerModel("tenant-1", "test-model", "pytorch",
                "CLASSIFICATION", "HR", "s3://models/test/v1/model.pt",
                "tj-1", "dsv-1", "user-1");

        List<ModelVersion> versions = service.listVersions("tenant-1",
                repository.findEntriesByTenant("tenant-1").getFirst().modelEntryId());

        assertEquals(1, versions.size());
        assertEquals(1, versions.getFirst().versionNumber());
    }

    @Test
    void promotesModelToProduction() {
        ModelEntry entry = service.registerModel("tenant-1", "test-model", "pytorch",
                "CLASSIFICATION", "HR", "s3://models/test/v1/model.pt",
                "tj-1", "dsv-1", "user-1");
        ModelVersion version = service.listVersions("tenant-1", entry.modelEntryId()).getFirst();

        ModelVersion promoted = service.promoteModel("tenant-1", version.modelVersionId(),
                ModelLifecycleStage.PRODUCTION, "admin-1");

        assertEquals(ModelLifecycleStage.PRODUCTION, promoted.lifecycleStage());
        assertEquals("admin-1", promoted.promotedBy());
    }

    @Test
    void rejectsPromotionOfAlreadyPromotedModel() {
        ModelEntry entry = service.registerModel("tenant-1", "test-model", "pytorch",
                "CLASSIFICATION", "HR", "s3://models/test/v1/model.pt",
                "tj-1", "dsv-1", "user-1");
        ModelVersion version = service.listVersions("tenant-1", entry.modelEntryId()).getFirst();
        service.promoteModel("tenant-1", version.modelVersionId(), ModelLifecycleStage.PRODUCTION, "admin-1");

        assertThrows(IllegalStateException.class, () ->
                service.promoteModel("tenant-1", version.modelVersionId(),
                        ModelLifecycleStage.PRODUCTION, "admin-2"));
    }

    @Test
    void rejectsPromotionOfArchivedModel() {
        ModelEntry entry = service.registerModel("tenant-1", "test-model", "pytorch",
                "CLASSIFICATION", "HR", "s3://models/test/v1/model.pt",
                "tj-1", "dsv-1", "user-1");
        ModelVersion version = service.listVersions("tenant-1", entry.modelEntryId()).getFirst();
        service.promoteModel("tenant-1", version.modelVersionId(), ModelLifecycleStage.ARCHIVED, "admin-1");

        assertThrows(IllegalStateException.class, () ->
                service.promoteModel("tenant-1", version.modelVersionId(),
                        ModelLifecycleStage.PRODUCTION, "admin-2"));
    }

    @Test
    void rollsBackToPreviousVersion() {
        ModelEntry entry = service.registerModel("tenant-1", "test-model", "pytorch",
                "CLASSIFICATION", "HR", "s3://models/test/v1/model.pt",
                "tj-1", "dsv-1", "user-1");

        assertThrows(IllegalArgumentException.class, () ->
                service.rollbackModel("tenant-1", entry.modelEntryId(), 99, "admin-1"));
    }

    @Test
    void listsModelsByTenant() {
        service.registerModel("tenant-1", "m1", "pytorch", "CLASSIFICATION", "HR",
                "s3://m1", "tj-1", "dsv-1", "user-1");
        service.registerModel("tenant-1", "m2", "tensorflow", "EXTRACTION", "ACCOUNTING",
                "s3://m2", "tj-2", "dsv-2", "user-1");

        List<ModelEntry> models = service.listModels("tenant-1");

        assertEquals(2, models.size());
    }

    @Test
    void enforcesTenantIsolation() {
        service.registerModel("tenant-1", "m1", "pytorch", "CLASSIFICATION", "HR",
                "s3://m1", "tj-1", "dsv-1", "user-1");

        List<ModelEntry> models = service.listModels("other-tenant");

        assertTrue(models.isEmpty());
    }

    @Test
    void listsVersionsByModelEntry() {
        // Register creates first version automatically
        ModelEntry entry = service.registerModel("tenant-1", "m1", "pytorch", "CLASSIFICATION", "HR",
                "s3://m1", "tj-1", "dsv-1", "user-1");

        List<ModelVersion> versions = service.listVersions("tenant-1", entry.modelEntryId());

        assertEquals(1, versions.size());
    }

    @Test
    void findsModelEntryById() {
        ModelEntry entry = service.registerModel("tenant-1", "test", "pytorch", "CLASSIFICATION", "HR",
                "s3://test", "tj-1", "dsv-1", "user-1");

        ModelEntry found = service.getModelEntry("tenant-1", entry.modelEntryId());

        assertEquals(entry.modelEntryId(), found.modelEntryId());
    }

    private static class CapturingEvidenceLedger implements EvidenceLedger {
        private final List<Object> events = new java.util.ArrayList<>();
        @Override public void append(EvidenceEvent event) { events.add(event); }
    }
}
