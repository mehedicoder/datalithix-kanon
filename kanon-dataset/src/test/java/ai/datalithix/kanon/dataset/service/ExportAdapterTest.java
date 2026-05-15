package ai.datalithix.kanon.dataset.service;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.dataset.model.DatasetSplit;
import ai.datalithix.kanon.dataset.model.DatasetVersion;
import ai.datalithix.kanon.dataset.model.ExportFormat;
import ai.datalithix.kanon.dataset.model.SplitStrategy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExportAdapterTest {
    private static final String BASE_PATH = System.getProperty("java.io.tmpdir") + "/kanon-export-test";
    private DatasetVersion version;

    @BeforeEach
    void setUp() {
        var audit = new AuditMetadata(Instant.now(), "tester", Instant.now(), "tester", 1);
        var splits = List.of(
                new DatasetSplit("TRAIN", 0.7, List.of("rec-1", "rec-2")),
                new DatasetSplit("TEST", 0.3, List.of("rec-3"))
        );
        version = new DatasetVersion("dsv-1", "dd-1", "tenant-1", 1, null,
                SplitStrategy.RANDOM, splits, Map.of(), Map.of(), 3,
                Instant.now(), "tester", null, null, null, null, List.of(), audit);
    }

    @AfterEach
    void tearDown() throws IOException {
        var dir = Path.of(BASE_PATH);
        if (Files.exists(dir)) {
            try (var paths = Files.walk(dir).sorted(Comparator.reverseOrder())) {
                paths.forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
            }
        }
    }

    @Test
    void jsonlAdapterExportsAllRecords() throws IOException {
        var adapter = new JsonlExportAdapter();
        assertEquals(ExportFormat.JSONL, adapter.supportedFormat());

        var uri = adapter.export(version, BASE_PATH);
        assertNotNull(uri);

        var content = Files.readString(Path.of(BASE_PATH, "tenant-1", "dd-1", "v1", "export.jsonl"));
        assertTrue(content.contains("rec-1"));
        assertTrue(content.contains("rec-2"));
        assertTrue(content.contains("rec-3"));
        assertTrue(content.contains("TRAIN"));
        assertTrue(content.contains("TEST"));
        assertEquals(3, content.trim().split("\n").length);
    }

    @Test
    void jsonlAdapterHandlesEmptySplits() {
        var emptyVersion = new DatasetVersion("dsv-2", "dd-1", "tenant-1", 2, null,
                SplitStrategy.RANDOM, List.of(), Map.of(), Map.of(), 0,
                Instant.now(), "tester", null, null, null, null, List.of(),
                new AuditMetadata(Instant.now(), "tester", Instant.now(), "tester", 1));
        var adapter = new JsonlExportAdapter();
        var uri = adapter.export(emptyVersion, BASE_PATH);
        assertNotNull(uri);
    }

    @Test
    void parquetAdapterWritesFile() {
        var adapter = new ParquetExportAdapter();
        assertEquals(ExportFormat.PARQUET, adapter.supportedFormat());
        var uri = adapter.export(version, BASE_PATH);
        assertNotNull(uri);
        assertTrue(uri.contains("export.parquet"));
    }

    @Test
    void huggingFaceAdapterExportsPerSplit() throws IOException {
        var adapter = new HuggingFaceExportAdapter();
        assertEquals(ExportFormat.HUGGING_FACE, adapter.supportedFormat());
        var uri = adapter.export(version, BASE_PATH);
        assertNotNull(uri);

        var dir = Path.of(BASE_PATH, "tenant-1", "dd-1", "v1");
        assertTrue(Files.exists(dir.resolve("train.jsonl")));
        assertTrue(Files.exists(dir.resolve("test.jsonl")));
    }

    @Test
    void tfRecordAdapterWritesFile() {
        var adapter = new TfRecordExportAdapter();
        assertEquals(ExportFormat.TF_RECORD, adapter.supportedFormat());
        var uri = adapter.export(version, BASE_PATH);
        assertNotNull(uri);
        assertTrue(uri.contains("export.tfrecord"));
    }

    @Test
    void allAdaptersRegisteredViaService() {
        var adapters = List.of(
                new JsonlExportAdapter(),
                new ParquetExportAdapter(),
                new HuggingFaceExportAdapter(),
                new TfRecordExportAdapter()
        );
        var formats = adapters.stream().map(ExportAdapter::supportedFormat).toList();
        assertTrue(formats.containsAll(List.of(ExportFormat.JSONL, ExportFormat.PARQUET,
                ExportFormat.HUGGING_FACE, ExportFormat.TF_RECORD)));
    }
}
