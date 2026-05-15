package ai.datalithix.kanon.dataset.service;

import ai.datalithix.kanon.dataset.model.CurationRule;
import ai.datalithix.kanon.dataset.model.DatasetDefinition;
import ai.datalithix.kanon.dataset.model.DatasetVersion;
import ai.datalithix.kanon.dataset.model.SplitStrategy;
import ai.datalithix.kanon.dataset.model.ExportFormat;
import java.util.List;

public interface DatasetCurationService {
    DatasetVersion curate(DatasetDefinition definition, String actorId);
    DatasetVersion curateWithRule(DatasetDefinition definition, CurationRule rule, String actorId);
    List<ExportFormat> export(DatasetVersion version, List<ExportFormat> formats);
    DatasetVersion computeMetadata(DatasetVersion version);
}
