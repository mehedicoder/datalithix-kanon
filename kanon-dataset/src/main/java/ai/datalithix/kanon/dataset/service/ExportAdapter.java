package ai.datalithix.kanon.dataset.service;

import ai.datalithix.kanon.dataset.model.DatasetVersion;
import ai.datalithix.kanon.dataset.model.ExportFormat;

public interface ExportAdapter {
    ExportFormat supportedFormat();
    String export(DatasetVersion version, String basePath);
}
