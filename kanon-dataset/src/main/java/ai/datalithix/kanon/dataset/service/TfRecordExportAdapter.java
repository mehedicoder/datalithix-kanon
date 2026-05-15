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
public class TfRecordExportAdapter implements ExportAdapter {
    private static final Logger log = LoggerFactory.getLogger(TfRecordExportAdapter.class);

    @Override
    public ExportFormat supportedFormat() {
        return ExportFormat.TF_RECORD;
    }

    @Override
    public String export(DatasetVersion version, String basePath) {
        try {
            var dir = Path.of(basePath, version.tenantId(), version.datasetDefinitionId(),
                    "v" + version.versionNumber());
            Files.createDirectories(dir);
            var outputPath = dir.resolve("export.tfrecord");
            try (var writer = new OutputStreamWriter(new FileOutputStream(outputPath.toFile()), StandardCharsets.UTF_8)) {
                writer.write("{\"datasetVersionId\":\"");
                writer.write(escapeJson(version.datasetVersionId()));
                writer.write("\",\"totalRecords\":");
                writer.write(String.valueOf(version.totalRecordCount()));
                writer.write(",\"splits\":[");
                var first = true;
                for (var split : version.splits()) {
                    if (!first) writer.write(",");
                    first = false;
                    writer.write("{\"type\":\"");
                    writer.write(escapeJson(split.splitType()));
                    writer.write("\",\"count\":");
                    writer.write(String.valueOf(split.annotationRecordIds().size()));
                    writer.write("}");
                }
                writer.write("]}");
            }
            log.info("Exported TFRecord metadata to {}", outputPath);
            return outputPath.toUri().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export TFRecord", e);
        }
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
