package ai.datalithix.kanon.dataset.service;

import ai.datalithix.kanon.dataset.model.DatasetVersion;
import ai.datalithix.kanon.dataset.model.ExportFormat;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JsonlExportAdapter implements ExportAdapter {
    private static final Logger log = LoggerFactory.getLogger(JsonlExportAdapter.class);

    @Override
    public ExportFormat supportedFormat() {
        return ExportFormat.JSONL;
    }

    @Override
    public String export(DatasetVersion version, String basePath) {
        try {
            var dir = Path.of(basePath, version.tenantId(), version.datasetDefinitionId(),
                    "v" + version.versionNumber());
            Files.createDirectories(dir);
            var outputPath = dir.resolve("export.jsonl");
            try (var writer = new OutputStreamWriter(new FileOutputStream(outputPath.toFile()), StandardCharsets.UTF_8)) {
                for (var split : version.splits()) {
                    for (var recordId : split.annotationRecordIds()) {
                        var line = "{\"recordId\":\"" + escapeJson(recordId)
                                + "\",\"split\":\"" + escapeJson(split.splitType())
                                + "\",\"datasetVersionId\":\"" + escapeJson(version.datasetVersionId())
                                + "\",\"tenantId\":\"" + escapeJson(version.tenantId()) + "\"}";
                        writer.write(line);
                        writer.write("\n");
                    }
                }
            }
            log.info("Exported {} records as JSONL to {}", version.totalRecordCount(), outputPath);
            return outputPath.toUri().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export JSONL", e);
        }
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
