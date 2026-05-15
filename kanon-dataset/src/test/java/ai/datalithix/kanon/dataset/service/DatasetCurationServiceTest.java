package ai.datalithix.kanon.dataset.service;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.dataset.model.CurationRule;
import ai.datalithix.kanon.dataset.model.DatasetDefinition;
import ai.datalithix.kanon.dataset.model.DatasetVersion;
import ai.datalithix.kanon.dataset.model.ExportFormat;
import ai.datalithix.kanon.dataset.model.SplitStrategy;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatasetCurationServiceTest {
    private final InMemoryDatasetRepository repository = new InMemoryDatasetRepository();
    private final CapturingEvidenceLedger ledger = new CapturingEvidenceLedger();
    private final DefaultDatasetCurationService service = new DefaultDatasetCurationService(repository, ledger, List.of());

    @Test
    void curatesDatasetWithDefaultSplits() {
        DatasetDefinition definition = createDefinition();
        repository.save(definition);

        DatasetVersion version = service.curate(definition, "user-1");

        assertNotNull(version);
        assertEquals(definition.datasetDefinitionId(), version.datasetDefinitionId());
        assertEquals(1, version.versionNumber());
        assertEquals("CURATED", version.exportStatus());
        assertEquals(3, version.splits().size());
    }

    @Test
    void curatesDatasetWithCustomRule() {
        DatasetDefinition definition = createDefinition();
        repository.save(definition);
        CurationRule rule = new CurationRule("rule-1", "Only approved",
                Set.of(), Set.of("APPROVED"), 0.9, Set.of(), true, true);

        DatasetVersion version = service.curateWithRule(definition, rule, "user-2");

        assertNotNull(version);
        assertEquals("rule-1", version.curationRuleId());
    }

    @Test
    void incrementsVersionNumberOnSubsequentCurations() {
        DatasetDefinition definition = createDefinition();
        repository.save(definition);

        DatasetVersion v1 = service.curate(definition, "user-1");
        DatasetDefinition updated = repository.findById(definition.tenantId(), definition.datasetDefinitionId()).orElseThrow();
        DatasetVersion v2 = service.curate(updated, "user-1");

        assertEquals(1, v1.versionNumber());
        assertEquals(2, v2.versionNumber());
    }

    @Test
    void computesLabelDistribution() {
        DatasetDefinition definition = createDefinition();
        repository.save(definition);

        DatasetVersion version = service.curate(definition, "user-1");

        assertEquals(3, version.totalRecordCount());
        assertFalse(version.labelDistribution().isEmpty());
    }

    @Test
    void exportsDatasetInRequestedFormats() {
        DatasetDefinition definition = createDefinition();
        repository.save(definition);
        DatasetVersion version = service.curate(definition, "user-1");

        List<ExportFormat> exported = service.export(version, List.of(ExportFormat.JSONL, ExportFormat.PARQUET));

        assertEquals(2, exported.size());
        DatasetVersion stored = repository.findVersionById(definition.tenantId(), version.datasetVersionId())
                .orElseThrow();
        assertEquals("EXPORTED", stored.exportStatus());
        assertNotNull(stored.exportArtifactUri());
    }

    @Test
    void rejectsCuratingInvalidDefinition() {
        assertThrows(NullPointerException.class, () -> service.curate(null, "user-1"));
    }

    @Test
    void rejectsCuratingWithInvalidSplitRatios() {
        assertThrows(IllegalArgumentException.class, () -> new DatasetDefinition(
                "def-1", "tenant-1", "bad", null, "HR",
                Set.of(), null, SplitStrategy.RANDOM, 0.5, 0.5, 0.5,
                List.of(ExportFormat.JSONL), null, true, 0,
                audit("user-1", 1)
        ));
    }

    private static DatasetDefinition createDefinition() {
        return new DatasetDefinition("def-1", "tenant-1", "test-dataset", "test",
                "ACCOUNTING", Set.of("ann-1", "ann-2", "ann-3"),
                null, SplitStrategy.RANDOM, 0.8, 0.1, 0.1,
                List.of(ExportFormat.JSONL), "EU", true, 0,
                audit("creator", 1));
    }

    private static AuditMetadata audit(String user, long version) {
        Instant now = Instant.now();
        return new AuditMetadata(now, user, now, user, version);
    }

    private static class CapturingEvidenceLedger implements EvidenceLedger {
        private final List<Object> events = new java.util.ArrayList<>();

        @Override
        public void append(EvidenceEvent event) { events.add(event); }

        List<Object> events() { return events; }
    }
}
