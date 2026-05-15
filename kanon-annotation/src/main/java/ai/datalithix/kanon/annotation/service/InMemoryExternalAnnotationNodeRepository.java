package ai.datalithix.kanon.annotation.service;

import ai.datalithix.kanon.annotation.model.ExternalAnnotationNode;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationProviderType;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryExternalAnnotationNodeRepository implements ExternalAnnotationNodeRepository {
    private final ConcurrentMap<String, ExternalAnnotationNode> nodesByKey = new ConcurrentHashMap<>();

    @Override
    public ExternalAnnotationNode save(ExternalAnnotationNode node) {
        nodesByKey.put(key(node.tenantId(), node.nodeId()), node);
        return node;
    }

    @Override
    public Optional<ExternalAnnotationNode> findById(String tenantId, String nodeId) {
        if (tenantId == null || tenantId.isBlank() || nodeId == null || nodeId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(nodesByKey.get(key(tenantId, nodeId)));
    }

    @Override
    public List<ExternalAnnotationNode> findByTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return List.of();
        }
        return nodesByKey.values().stream()
                .filter(node -> node.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(node -> node.audit().updatedAt(), Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public List<ExternalAnnotationNode> findByTenantAndProvider(String tenantId, ExternalAnnotationProviderType providerType) {
        if (tenantId == null || tenantId.isBlank() || providerType == null) {
            return List.of();
        }
        return findByTenant(tenantId).stream()
                .filter(node -> node.providerType() == providerType)
                .toList();
    }

    @Override
    public void deleteById(String tenantId, String nodeId) {
        if (tenantId == null || tenantId.isBlank() || nodeId == null || nodeId.isBlank()) {
            return;
        }
        nodesByKey.remove(key(tenantId, nodeId));
    }

    @Override
    public PageResult<ExternalAnnotationNode> findPage(QuerySpec query) {
        List<ExternalAnnotationNode> all = nodesByKey.values().stream()
                .sorted(Comparator.comparing(node -> node.audit().updatedAt(), Comparator.reverseOrder()))
                .toList();
        return new PageResult<>(all, 0, all.size(), all.size());
    }

    private static String key(String tenantId, String nodeId) {
        return tenantId + "::" + nodeId;
    }
}
