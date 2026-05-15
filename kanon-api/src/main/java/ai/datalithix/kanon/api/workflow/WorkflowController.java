package ai.datalithix.kanon.api.workflow;

import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.TenantContext;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.SortDirection;
import ai.datalithix.kanon.domain.model.TaskDescriptor;
import ai.datalithix.kanon.workflow.model.WorkflowDefinition;
import ai.datalithix.kanon.workflow.model.WorkflowActionRequest;
import ai.datalithix.kanon.workflow.model.WorkflowInstance;
import ai.datalithix.kanon.workflow.model.WorkflowPlan;
import ai.datalithix.kanon.workflow.model.WorkflowType;
import ai.datalithix.kanon.workflow.service.WorkflowDefinitionRepository;
import ai.datalithix.kanon.workflow.service.WorkflowInstanceRepository;
import ai.datalithix.kanon.workflow.service.WorkflowPlanner;
import ai.datalithix.kanon.workflow.service.WorkflowTaskCommandService;
import java.util.List;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static ai.datalithix.kanon.api.support.ApiQuerySupport.dimensions;
import static ai.datalithix.kanon.api.support.ApiQuerySupport.query;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {
    private final WorkflowPlanner workflowPlanner;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowTaskCommandService workflowTaskCommandService;

    public WorkflowController(
            WorkflowPlanner workflowPlanner,
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkflowInstanceRepository workflowInstanceRepository,
            WorkflowTaskCommandService workflowTaskCommandService
    ) {
        this.workflowPlanner = workflowPlanner;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.workflowTaskCommandService = workflowTaskCommandService;
    }

    @GetMapping("/plan")
    public WorkflowPlan plan(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                             @RequestParam(defaultValue = "ACCOUNTING") DomainType domainType,
                             @RequestParam(defaultValue = "DE") String countryCode,
                             @RequestParam(defaultValue = "EU_AI_ACT_2026") String regulatoryAct,
                             @RequestParam(defaultValue = "EXTRACTION") AiTaskType taskType,
                             @RequestParam(defaultValue = "case-001") String caseId,
                             @RequestParam(defaultValue = "true") boolean allowCloudModels,
                             @RequestParam(defaultValue = "false") boolean preferLocalModels) {
        TenantContext tenantContext = new TenantContext(
                tenantId,
                domainType,
                countryCode,
                regulatoryAct,
                allowCloudModels,
                preferLocalModels,
                java.util.Set.of("AUDIT_REQUIRED")
        );
        TaskDescriptor taskDescriptor = new TaskDescriptor(taskType, caseId, "memory://sample", "v1", false);
        return workflowPlanner.plan(tenantContext, taskDescriptor);
    }

    @GetMapping("/definitions")
    public PageResult<WorkflowDefinition> definitions(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "50") int size,
                                                      @RequestParam(required = false) String sortBy,
                                                      @RequestParam(defaultValue = "ASC") SortDirection sortDirection,
                                                      @RequestParam(required = false) WorkflowType workflowType,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String enabled) {
        return workflowDefinitionRepository.findPage(query(
                tenantId,
                page,
                size,
                sortBy,
                sortDirection,
                dimensions("workflowType", workflowType, "status", status, "enabled", enabled)
        ));
    }

    @GetMapping("/definitions/{workflowId}")
    public WorkflowDefinition definition(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                         @PathVariable String workflowId) {
        return workflowDefinitionRepository.findById(tenantId, workflowId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Workflow definition not found: " + workflowId));
    }

    @GetMapping("/instances")
    public PageResult<WorkflowInstance> instances(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size,
                                                  @RequestParam(required = false) String sortBy,
                                                  @RequestParam(defaultValue = "ASC") SortDirection sortDirection,
                                                  @RequestParam(required = false) String workflowId,
                                                  @RequestParam(required = false) String caseId,
                                                  @RequestParam(required = false) String assignedUserId,
                                                  @RequestParam(required = false) String reviewStatus,
                                                  @RequestParam(required = false) String approvalStatus) {
        return workflowInstanceRepository.findPage(query(
                tenantId,
                page,
                size,
                sortBy,
                sortDirection,
                dimensions(
                        "workflowId", workflowId,
                        "caseId", caseId,
                        "assignedUserId", assignedUserId,
                        "reviewStatus", reviewStatus,
                        "approvalStatus", approvalStatus
                )
        ));
    }

    @GetMapping("/instances/{workflowInstanceId}")
    public WorkflowInstance instance(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                     @PathVariable String workflowInstanceId) {
        return workflowInstanceRepository.findById(tenantId, workflowInstanceId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Workflow instance not found: " + workflowInstanceId));
    }

    @GetMapping("/cases/{caseId}/instances")
    public List<WorkflowInstance> instancesByCase(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                                  @PathVariable String caseId) {
        return workflowInstanceRepository.findByCaseId(tenantId, caseId);
    }

    @GetMapping("/review-tasks")
    public List<WorkflowInstance> reviewTasks(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                              @RequestParam(required = false) String assignedUserId) {
        return workflowInstanceRepository.findOpenReviewTasks(tenantId, assignedUserId);
    }

    @PostMapping("/instances/{workflowInstanceId}/review/start")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER", "ROLE_REVIEWER_ANNOTATOR"})
    public WorkflowInstance startReview(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                        @PathVariable String workflowInstanceId,
                                        @RequestBody(required = false) WorkflowActionBody body) {
        return workflowTaskCommandService.startReview(request(tenantId, workflowInstanceId, body));
    }

    @PostMapping("/instances/{workflowInstanceId}/review/complete")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER", "ROLE_REVIEWER_ANNOTATOR"})
    public WorkflowInstance completeReview(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                           @PathVariable String workflowInstanceId,
                                           @RequestBody(required = false) WorkflowActionBody body) {
        return workflowTaskCommandService.completeReview(request(tenantId, workflowInstanceId, body));
    }

    @PostMapping("/instances/{workflowInstanceId}/approve")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    public WorkflowInstance approve(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                    @PathVariable String workflowInstanceId,
                                    @RequestBody(required = false) WorkflowActionBody body) {
        return workflowTaskCommandService.approve(request(tenantId, workflowInstanceId, body));
    }

    @PostMapping("/instances/{workflowInstanceId}/reject")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    public WorkflowInstance reject(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                   @PathVariable String workflowInstanceId,
                                   @RequestBody(required = false) WorkflowActionBody body) {
        return workflowTaskCommandService.reject(request(tenantId, workflowInstanceId, body));
    }

    @PostMapping("/instances/{workflowInstanceId}/escalate")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    public WorkflowInstance escalate(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                     @PathVariable String workflowInstanceId,
                                     @RequestBody(required = false) WorkflowActionBody body) {
        return workflowTaskCommandService.escalate(request(tenantId, workflowInstanceId, body));
    }

    @PostMapping("/instances/{workflowInstanceId}/export-ready")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    public WorkflowInstance markExportReady(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                            @PathVariable String workflowInstanceId,
                                            @RequestBody(required = false) WorkflowActionBody body) {
        return workflowTaskCommandService.markExportReady(request(tenantId, workflowInstanceId, body));
    }

    private static WorkflowActionRequest request(String tenantId, String workflowInstanceId, WorkflowActionBody body) {
        WorkflowActionBody safeBody = body == null ? new WorkflowActionBody("system", null) : body;
        return new WorkflowActionRequest(tenantId, workflowInstanceId, safeBody.actorId(), safeBody.reason());
    }

    public record WorkflowActionBody(String actorId, String reason) {}
}
