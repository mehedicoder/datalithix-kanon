package ai.datalithix.kanon.dataset.service;

import ai.datalithix.kanon.common.service.PagedQueryPort;
import ai.datalithix.kanon.dataset.model.DatasetDefinition;
import ai.datalithix.kanon.dataset.model.DatasetVersion;
import java.util.List;
import java.util.Optional;

public interface DatasetRepository extends PagedQueryPort<DatasetDefinition> {
    DatasetDefinition save(DatasetDefinition definition);
    Optional<DatasetDefinition> findById(String tenantId, String datasetDefinitionId);
    List<DatasetDefinition> findByName(String tenantId, String name);
    List<DatasetDefinition> findByTenant(String tenantId);
    void deleteById(String tenantId, String datasetDefinitionId);

    DatasetVersion saveVersion(DatasetVersion version);
    Optional<DatasetVersion> findVersionById(String tenantId, String datasetVersionId);
    List<DatasetVersion> findVersionsByDefinitionId(String tenantId, String datasetDefinitionId);
    Optional<DatasetVersion> findLatestVersion(String tenantId, String datasetDefinitionId);
}
