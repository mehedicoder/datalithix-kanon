package ai.datalithix.kanon.workflow.service;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.model.SortDirection;
import ai.datalithix.kanon.workflow.model.WorkflowDefinition;
import ai.datalithix.kanon.workflow.model.WorkflowType;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryWorkflowDefinitionRepository implements WorkflowDefinitionRepository {
    private final Map<Key, WorkflowDefinition> workflowDefinitions = new ConcurrentHashMap<>();

    @Override
    public WorkflowDefinition save(WorkflowDefinition workflowDefinition) {
        workflowDefinitions.put(new Key(workflowDefinition.tenantId(), workflowDefinition.workflowId()), workflowDefinition);
        return workflowDefinition;
    }

    @Override
    public Optional<WorkflowDefinition> findById(String tenantId, String workflowId) {
        return Optional.ofNullable(workflowDefinitions.get(new Key(tenantId, workflowId)));
    }

    @Override
    public void deleteById(String tenantId, String workflowId) {
        workflowDefinitions.remove(new Key(tenantId, workflowId));
    }

    @Override
    public List<WorkflowDefinition> findEnabledByTenant(String tenantId) {
        return workflowDefinitions.values().stream()
                .filter(workflowDefinition -> workflowDefinition.tenantId().equals(tenantId))
                .filter(WorkflowDefinition::enabled)
                .sorted(Comparator.comparing(WorkflowDefinition::name))
                .toList();
    }

    @Override
    public List<WorkflowDefinition> findEnabledByType(String tenantId, WorkflowType workflowType) {
        return findEnabledByTenant(tenantId).stream()
                .filter(workflowDefinition -> workflowDefinition.workflowType() == workflowType)
                .toList();
    }

    @Override
    public PageResult<WorkflowDefinition> findPage(QuerySpec query) {
        List<WorkflowDefinition> filtered = workflowDefinitions.values().stream()
                .filter(workflowDefinition -> workflowDefinition.tenantId().equals(query.tenantId()))
                .filter(workflowDefinition -> matchesDimensions(workflowDefinition, query.dimensions()))
                .sorted(comparator(query.page().sortBy(), query.page().sortDirection() == SortDirection.DESC))
                .toList();
        int fromIndex = Math.min(query.page().pageNumber() * query.page().pageSize(), filtered.size());
        int toIndex = Math.min(fromIndex + query.page().pageSize(), filtered.size());
        return new PageResult<>(filtered.subList(fromIndex, toIndex), query.page().pageNumber(), query.page().pageSize(), filtered.size());
    }

    private static boolean matchesDimensions(WorkflowDefinition workflowDefinition, Map<String, String> dimensions) {
        return dimensions.entrySet().stream().allMatch(entry -> switch (entry.getKey()) {
            case "workflowType" -> workflowDefinition.workflowType().name().equals(entry.getValue());
            case "organizationId" -> workflowDefinition.organizationId().equals(entry.getValue());
            case "workspaceId" -> workflowDefinition.workspaceId().equals(entry.getValue());
            case "status" -> workflowDefinition.status().name().equals(entry.getValue());
            case "enabled" -> Boolean.toString(workflowDefinition.enabled()).equals(entry.getValue());
            default -> true;
        });
    }

    private static Comparator<WorkflowDefinition> comparator(String sortBy, boolean descending) {
        Comparator<WorkflowDefinition> comparator = switch (sortBy == null ? "" : sortBy) {
            case "workflowType" -> Comparator.comparing(workflowDefinition -> workflowDefinition.workflowType().name());
            case "status" -> Comparator.comparing(workflowDefinition -> workflowDefinition.status().name());
            case "updatedAt" -> Comparator.comparing(workflowDefinition -> workflowDefinition.audit().updatedAt());
            default -> Comparator.comparing(WorkflowDefinition::name);
        };
        return descending ? comparator.reversed() : comparator;
    }

    private record Key(String tenantId, String workflowId) {}
}
