package ai.datalithix.kanon.workflow.service;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.model.SortDirection;
import ai.datalithix.kanon.workflow.model.WorkflowInstance;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryWorkflowInstanceRepository implements WorkflowInstanceRepository {
    private final Map<Key, WorkflowInstance> workflowInstances = new ConcurrentHashMap<>();

    @Override
    public WorkflowInstance save(WorkflowInstance workflowInstance) {
        workflowInstances.put(new Key(workflowInstance.tenantId(), workflowInstance.workflowInstanceId()), workflowInstance);
        return workflowInstance;
    }

    @Override
    public Optional<WorkflowInstance> findById(String tenantId, String workflowInstanceId) {
        return Optional.ofNullable(workflowInstances.get(new Key(tenantId, workflowInstanceId)));
    }

    @Override
    public List<WorkflowInstance> findByCaseId(String tenantId, String caseId) {
        return workflowInstances.values().stream()
                .filter(workflowInstance -> workflowInstance.tenantId().equals(tenantId))
                .filter(workflowInstance -> caseId.equals(workflowInstance.caseId()))
                .sorted(Comparator.comparing(WorkflowInstance::startedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    public List<WorkflowInstance> findOpenReviewTasks(String tenantId, String assignedUserId) {
        return workflowInstances.values().stream()
                .filter(workflowInstance -> workflowInstance.tenantId().equals(tenantId))
                .filter(WorkflowInstance::reviewRequired)
                .filter(workflowInstance -> assignedUserId == null || assignedUserId.equals(workflowInstance.assignedUserId()))
                .filter(workflowInstance -> workflowInstance.completedAt() == null && workflowInstance.failedAt() == null)
                .sorted(Comparator.comparing(WorkflowInstance::dueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    public PageResult<WorkflowInstance> findPage(QuerySpec query) {
        List<WorkflowInstance> filtered = workflowInstances.values().stream()
                .filter(workflowInstance -> workflowInstance.tenantId().equals(query.tenantId()))
                .filter(workflowInstance -> matchesDimensions(workflowInstance, query.dimensions()))
                .sorted(comparator(query.page().sortBy(), query.page().sortDirection() == SortDirection.DESC))
                .toList();
        int fromIndex = Math.min(query.page().pageNumber() * query.page().pageSize(), filtered.size());
        int toIndex = Math.min(fromIndex + query.page().pageSize(), filtered.size());
        return new PageResult<>(filtered.subList(fromIndex, toIndex), query.page().pageNumber(), query.page().pageSize(), filtered.size());
    }

    private static boolean matchesDimensions(WorkflowInstance workflowInstance, Map<String, String> dimensions) {
        return dimensions.entrySet().stream().allMatch(entry -> switch (entry.getKey()) {
            case "workflowId" -> workflowInstance.workflowId().equals(entry.getValue());
            case "organizationId" -> workflowInstance.organizationId().equals(entry.getValue());
            case "workspaceId" -> workflowInstance.workspaceId().equals(entry.getValue());
            case "caseId" -> entry.getValue().equals(workflowInstance.caseId());
            case "assignedUserId" -> entry.getValue().equals(workflowInstance.assignedUserId());
            case "assignedMembershipId" -> entry.getValue().equals(workflowInstance.assignedMembershipId());
            case "reviewStatus" -> workflowInstance.reviewStatus().name().equals(entry.getValue());
            case "approvalStatus" -> workflowInstance.approvalStatus().name().equals(entry.getValue());
            default -> true;
        });
    }

    private static Comparator<WorkflowInstance> comparator(String sortBy, boolean descending) {
        Comparator<WorkflowInstance> comparator = switch (sortBy == null ? "" : sortBy) {
            case "dueAt" -> Comparator.comparing(WorkflowInstance::dueAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "completedAt" -> Comparator.comparing(WorkflowInstance::completedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "updatedAt" -> Comparator.comparing(workflowInstance -> workflowInstance.audit().updatedAt());
            default -> Comparator.comparing(WorkflowInstance::startedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        };
        return descending ? comparator.reversed() : comparator;
    }

    private record Key(String tenantId, String workflowInstanceId) {}
}
