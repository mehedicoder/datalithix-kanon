package ai.datalithix.kanon.api.annotation;

import ai.datalithix.kanon.annotation.model.AnnotationSyncRecord;
import ai.datalithix.kanon.annotation.model.AnnotationTask;
import ai.datalithix.kanon.annotation.model.AnnotationTaskCreation;
import ai.datalithix.kanon.annotation.service.AnnotationTaskOrchestrationService;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.PageSpec;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.model.SortDirection;
import ai.datalithix.kanon.common.AssetType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.annotation.model.AnnotationExecutionNodeType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/annotation")
@Tag(name = "Annotation Tasks", description = "Create, sync, and retry annotation tasks on CVAT/Label Studio nodes")
public class AnnotationTaskController {
    private final AnnotationTaskOrchestrationService orchestrationService;

    public AnnotationTaskController(AnnotationTaskOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/tasks")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    @Operation(summary = "Create and push an annotation task",
            description = "Routes the task to the appropriate annotation node (CVAT or Label Studio) and records the sync")
    public AnnotationTaskCreation createTask(@RequestBody CreateTaskRequest request) {
        var task = new AnnotationTask(
                request.annotationTaskId, request.tenantId, request.caseId,
                request.workflowInstanceId, request.sourceTraceId, request.mediaAssetId,
                request.assetType, request.domainType, request.preferredNodeType,
                request.labels, request.payloadRefs, request.instructions,
                request.evidenceEventId, Instant.now()
        );
        return orchestrationService.pushTask(task);
    }

    @PostMapping("/tasks/{taskId}/sync")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    @Operation(summary = "Sync annotation result from external node",
            description = "Fetches and persists the annotation result from CVAT or Label Studio")
    public AnnotationSyncRecord syncTask(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @Parameter(description = "Node ID") @RequestParam String nodeId,
            @Parameter(description = "External task ID") @PathVariable String taskId) {
        try {
            return orchestrationService.syncResult(tenantId, nodeId, taskId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/tasks/{taskId}/retry")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN"})
    @Operation(summary = "Retry a failed annotation task",
            description = "Re-pushes the annotation task to the external node and creates a new sync record")
    public AnnotationSyncRecord retrySync(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @Parameter(description = "Annotation task ID") @PathVariable String taskId) {
        try {
            return orchestrationService.retry(tenantId, taskId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/sync-records")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    @Operation(summary = "List annotation sync records",
            description = "Paginated list of annotation sync records for a tenant")
    public PageResult<AnnotationSyncRecord> listSyncRecords(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return orchestrationService.findSyncRecords(
                new QuerySpec(tenantId, new PageSpec(page, size, null, SortDirection.ASC), null, null));
    }

    @GetMapping("/tasks/{taskId}/sync-records")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    @Operation(summary = "Get sync records for an annotation task")
    public java.util.List<AnnotationSyncRecord> getTaskSyncRecords(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @Parameter(description = "Annotation task ID") @PathVariable String taskId) {
        return orchestrationService.findByTaskId(tenantId, taskId);
    }

    @Schema(description = "Request to create an annotation task")
    public record CreateTaskRequest(
            @Schema(description = "Kanon annotation task ID") String annotationTaskId,
            @Schema(description = "Tenant ID", example = "demo-tenant") String tenantId,
            @Schema(description = "Case ID") String caseId,
            @Schema(description = "Workflow instance ID") String workflowInstanceId,
            @Schema(description = "Source trace ID") String sourceTraceId,
            @Schema(description = "Media asset ID") String mediaAssetId,
            @Schema(description = "Asset type") AssetType assetType,
            @Schema(description = "Domain type") DomainType domainType,
            @Schema(description = "Preferred annotation node type") AnnotationExecutionNodeType preferredNodeType,
            @Schema(description = "Labels to use") Set<String> labels,
            @Schema(description = "Payload references") Map<String, String> payloadRefs,
            @Schema(description = "Instructions for annotators") Map<String, String> instructions,
            @Schema(description = "Evidence event ID") String evidenceEventId) {}
}
