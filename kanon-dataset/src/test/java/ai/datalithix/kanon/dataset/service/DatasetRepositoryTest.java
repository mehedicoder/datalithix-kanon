package ai.datalithix.kanon.dataset.service;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.PageSpec;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.model.SortDirection;
import ai.datalithix.kanon.dataset.model.DatasetDefinition;
import ai.datalithix.kanon.dataset.model.DatasetVersion;
import ai.datalithix.kanon.dataset.model.ExportFormat;
import ai.datalithix.kanon.dataset.model.SplitStrategy;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatasetRepositoryTest {
    private final InMemoryDatasetRepository repository = new InMemoryDatasetRepository();

    @Test
    void savesAndFindsDefinitionById() {
        DatasetDefinition def = createDefinition();
        repository.save(def);

        var found = repository.findById("tenant-1", "def-1");

        assertTrue(found.isPresent());
        assertEquals("test-dataset", found.get().name());
    }

    @Test
    void enforcesTenantIsolationOnFindById() {
        repository.save(createDefinition());

        var found = repository.findById("other-tenant", "def-1");

        assertTrue(found.isEmpty());
    }

    @Test
    void findsDefinitionsByTenant() {
        repository.save(createDefinition("def-1", "tenant-1"));
        repository.save(createDefinition("def-2", "tenant-1"));
        repository.save(createDefinition("def-3", "tenant-2"));

        var tenant1Defs = repository.findByTenant("tenant-1");
        var tenant2Defs = repository.findByTenant("tenant-2");

        assertEquals(2, tenant1Defs.size());
        assertEquals(1, tenant2Defs.size());
    }

    @Test
    void deletesDefinitionById() {
        repository.save(createDefinition());

        repository.deleteById("tenant-1", "def-1");

        assertTrue(repository.findById("tenant-1", "def-1").isEmpty());
    }

    @Test
    void deleteEnforcesTenantIsolation() {
        repository.save(createDefinition());

        repository.deleteById("other-tenant", "def-1");

        assertTrue(repository.findById("tenant-1", "def-1").isPresent());
    }

    @Test
    void findsByName() {
        repository.save(createDefinition("def-1", "tenant-1"));

        var found = repository.findByName("tenant-1", "test-dataset");

        assertEquals(1, found.size());
    }

    @Test
    void savesAndFindsVersions() {
        repository.save(createDefinition());
        DatasetVersion version = createVersion("v-1", "def-1", 1);
        repository.saveVersion(version);

        var found = repository.findVersionById("tenant-1", "v-1");

        assertTrue(found.isPresent());
        assertEquals(1, found.get().versionNumber());
    }

    @Test
    void findsVersionsByDefinitionId() {
        repository.save(createDefinition());
        repository.saveVersion(createVersion("v-1", "def-1", 1));
        repository.saveVersion(createVersion("v-2", "def-1", 2));

        var versions = repository.findVersionsByDefinitionId("tenant-1", "def-1");

        assertEquals(2, versions.size());
    }

    @Test
    void findsLatestVersion() {
        repository.save(createDefinition());
        repository.saveVersion(createVersion("v-1", "def-1", 1));
        repository.saveVersion(createVersion("v-2", "def-1", 2));

        var latest = repository.findLatestVersion("tenant-1", "def-1");

        assertTrue(latest.isPresent());
        assertEquals(2, latest.get().versionNumber());
    }

    @Test
    void supportsPagination() {
        for (int i = 0; i < 10; i++) {
            repository.save(createDefinition("def-" + i, "tenant-1"));
        }

        PageResult<DatasetDefinition> page = repository.findPage(
                new QuerySpec("tenant-1", new PageSpec(0, 3, "name", SortDirection.ASC), List.of(), java.util.Map.of()));

        assertEquals(10, page.totalItems());
        assertEquals(3, page.items().size());
    }

    private static DatasetDefinition createDefinition() {
        return createDefinition("def-1", "tenant-1");
    }

    private static DatasetDefinition createDefinition(String id, String tenantId) {
        return new DatasetDefinition(id, tenantId, "test-dataset", "test",
                "ACCOUNTING", Set.of(), null, SplitStrategy.RANDOM, 0.8, 0.1, 0.1,
                List.of(ExportFormat.JSONL), "EU", true, 0,
                audit("creator", 1));
    }

    private static DatasetVersion createVersion(String id, String defId, int versionNum) {
        return new DatasetVersion(id, defId, "tenant-1", versionNum, null,
                SplitStrategy.RANDOM, List.of(), java.util.Map.of(), java.util.Map.of(),
                100, Instant.now(), "creator", "CURATED", null, null, null,
                List.of(), audit("creator", 1));
    }

    private static AuditMetadata audit(String user, long version) {
        Instant now = Instant.now();
        return new AuditMetadata(now, user, now, user, version);
    }
}
