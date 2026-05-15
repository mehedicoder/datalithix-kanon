package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.ingestion.model.SourceDescriptor;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySourceDescriptorRepository implements SourceDescriptorRepository {
    private final Map<String, SourceDescriptor> descriptors = new ConcurrentHashMap<>();

    private static String key(String tenantId, String sourceSystem, String sourceIdentifier) {
        return tenantId + ":" + sourceSystem + ":" + sourceIdentifier;
    }

    @Override
    public SourceDescriptor save(String tenantId, SourceDescriptor descriptor) {
        descriptors.put(key(tenantId, descriptor.sourceSystem(), descriptor.sourceIdentifier()), descriptor);
        return descriptor;
    }

    @Override
    public Optional<SourceDescriptor> findBySourceIdentity(String tenantId, String sourceSystem, String sourceIdentifier) {
        return Optional.ofNullable(descriptors.get(key(tenantId, sourceSystem, sourceIdentifier)));
    }

    @Override
    public PageResult<SourceDescriptor> findPage(QuerySpec query) {
        var matching = descriptors.entrySet().stream()
                .filter(e -> e.getKey().startsWith(query.tenantId() + ":"))
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparing(SourceDescriptor::sourceSystem))
                .toList();
        int limit = query.page().pageSize();
        int offset = query.page().pageNumber() * limit;
        var items = offset >= matching.size() ? java.util.List.<SourceDescriptor>of()
                : matching.subList(offset, Math.min(offset + limit, matching.size()));
        return new PageResult<>(items, query.page().pageNumber(), query.page().pageSize(), matching.size());
    }
}
