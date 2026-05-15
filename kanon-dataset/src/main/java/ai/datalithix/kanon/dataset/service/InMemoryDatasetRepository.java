package ai.datalithix.kanon.dataset.service;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.dataset.model.DatasetDefinition;
import ai.datalithix.kanon.dataset.model.DatasetVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDatasetRepository implements DatasetRepository {
    private final Map<String, DatasetDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, DatasetVersion> versions = new ConcurrentHashMap<>();

    @Override
    public DatasetDefinition save(DatasetDefinition definition) {
        definitions.put(definition.datasetDefinitionId(), definition);
        return definition;
    }

    @Override
    public Optional<DatasetDefinition> findById(String tenantId, String datasetDefinitionId) {
        return Optional.ofNullable(definitions.get(datasetDefinitionId))
                .filter(d -> d.tenantId().equals(tenantId));
    }

    @Override
    public List<DatasetDefinition> findByName(String tenantId, String name) {
        return definitions.values().stream()
                .filter(d -> d.tenantId().equals(tenantId))
                .filter(d -> d.name().equals(name))
                .toList();
    }

    @Override
    public List<DatasetDefinition> findByTenant(String tenantId) {
        return definitions.values().stream()
                .filter(d -> d.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public void deleteById(String tenantId, String datasetDefinitionId) {
        findById(tenantId, datasetDefinitionId).ifPresent(d -> definitions.remove(d.datasetDefinitionId()));
    }

    @Override
    public DatasetVersion saveVersion(DatasetVersion version) {
        versions.put(version.datasetVersionId(), version);
        return version;
    }

    @Override
    public Optional<DatasetVersion> findVersionById(String tenantId, String datasetVersionId) {
        return Optional.ofNullable(versions.get(datasetVersionId))
                .filter(v -> v.tenantId().equals(tenantId));
    }

    @Override
    public List<DatasetVersion> findVersionsByDefinitionId(String tenantId, String datasetDefinitionId) {
        return versions.values().stream()
                .filter(v -> v.tenantId().equals(tenantId))
                .filter(v -> v.datasetDefinitionId().equals(datasetDefinitionId))
                .sorted((a, b) -> Integer.compare(b.versionNumber(), a.versionNumber()))
                .toList();
    }

    @Override
    public Optional<DatasetVersion> findLatestVersion(String tenantId, String datasetDefinitionId) {
        return findVersionsByDefinitionId(tenantId, datasetDefinitionId).stream().findFirst();
    }

    @Override
    public PageResult<DatasetDefinition> findPage(QuerySpec query) {
        List<DatasetDefinition> items = findByTenant(query.tenantId());
        int total = items.size();
        int offset = query.page().pageNumber() * query.page().pageSize();
        List<DatasetDefinition> page = items.stream()
                .skip(offset)
                .limit(query.page().pageSize())
                .toList();
        return new PageResult<>(page, query.page().pageNumber(), query.page().pageSize(), total);
    }
}
