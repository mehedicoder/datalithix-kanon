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
public class HuggingFaceExportAdapter implements ExportAdapter {
    private static final Logger log = LoggerFactory.getLogger(HuggingFaceExportAdapter.class);

    @Override
    public ExportFormat supportedFormat() {
        return ExportFormat.HUGGING_FACE;
    }

    @Override
    public String export(DatasetVersion version, String basePath) {
        try {
            var dir = Path.of(basePath, version.tenantId(), version.datasetDefinitionId(),
                    "v" + version.versionNumber());
            Files.createDirectories(dir);
            for (var split : version.splits()) {
                var splitFile = dir.resolve(split.splitType() + ".jsonl");
                try (var writer = new OutputStreamWriter(
                        new FileOutputStream(splitFile.toFile()), StandardCharsets.UTF_8)) {
                    for (var recordId : split.annotationRecordIds()) {
                        writer.write("{\"recordId\":\"");
                        writer.write(escapeJson(recordId));
                        writer.write("\",\"datasetVersionId\":\"");
                        writer.write(escapeJson(version.datasetVersionId()));
                        writer.write("\",\"tenantId\":\"");
                        writer.write(escapeJson(version.tenantId()));
                        writer.write("\"}\n");
                    }
                }
            }
            log.info("Exported HF dataset to {}", dir);
            return dir.toUri().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export HuggingFace dataset", e);
        }
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
