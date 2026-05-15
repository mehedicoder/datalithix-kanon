package ai.datalithix.kanon.api.modelregistry;

import ai.datalithix.kanon.modelregistry.model.DeploymentConfig;
import ai.datalithix.kanon.modelregistry.model.DeploymentTarget;
import ai.datalithix.kanon.modelregistry.model.EvaluationRun;
import ai.datalithix.kanon.modelregistry.model.ModelEntry;
import ai.datalithix.kanon.modelregistry.model.ModelLifecycleStage;
import ai.datalithix.kanon.modelregistry.model.ModelVersion;
import ai.datalithix.kanon.modelregistry.service.DeploymentService;
import ai.datalithix.kanon.modelregistry.service.EvaluationService;
import ai.datalithix.kanon.modelregistry.service.ModelRegistryService;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/models")
public class ModelRegistryController {
    private final ModelRegistryService modelRegistryService;
    private final EvaluationService evaluationService;
    private final DeploymentService deploymentService;

    public ModelRegistryController(
            ModelRegistryService modelRegistryService,
            EvaluationService evaluationService,
            DeploymentService deploymentService
    ) {
        this.modelRegistryService = modelRegistryService;
        this.evaluationService = evaluationService;
        this.deploymentService = deploymentService;
    }

    @GetMapping
    public List<ModelEntry> listModels(@RequestParam(defaultValue = "demo-tenant") String tenantId) {
        return modelRegistryService.listModels(tenantId);
    }

    @GetMapping("/{modelEntryId}")
    public ModelEntry getModelEntry(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                     @PathVariable String modelEntryId) {
        return modelRegistryService.getModelEntry(tenantId, modelEntryId);
    }

    @PostMapping
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER", "ROLE_MODEL_OPERATOR"})
    public ModelEntry registerModel(@RequestBody RegisterModelRequest request) {
        return modelRegistryService.registerModel(
                request.tenantId(), request.modelName(), request.framework(),
                request.taskType(), request.domainType(), request.artifactUri(),
                request.trainingJobId(), request.datasetVersionId(), request.actorId());
    }

    @GetMapping("/{modelEntryId}/versions")
    public List<ModelVersion> listVersions(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                            @PathVariable String modelEntryId) {
        return modelRegistryService.listVersions(tenantId, modelEntryId);
    }

    @PostMapping("/versions/{versionId}/promote")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_MODEL_OPERATOR"})
    public ModelVersion promoteVersion(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                        @PathVariable String versionId,
                                        @RequestBody PromoteRequest request) {
        return modelRegistryService.promoteModel(tenantId, versionId, request.targetStage(), request.actorId());
    }

    @PostMapping("/{modelEntryId}/rollback")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN"})
    public ModelVersion rollbackToVersion(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                           @PathVariable String modelEntryId,
                                           @RequestBody RollbackRequest request) {
        return modelRegistryService.rollbackModel(tenantId, modelEntryId, request.targetVersion(), request.actorId());
    }

    @PostMapping("/versions/{versionId}/evaluate")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_MODEL_OPERATOR"})
    public EvaluationRun evaluateVersion(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                          @PathVariable String versionId,
                                          @RequestBody EvaluateRequest request) {
        return evaluationService.evaluateWithThreshold(
                tenantId, versionId, request.testDatasetVersionId(), request.minThreshold(), request.actorId());
    }

    @GetMapping("/versions/{versionId}/evaluations")
    public List<EvaluationRun> getEvaluations(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                               @PathVariable String versionId) {
        return evaluationService.getEvaluationHistory(tenantId, versionId);
    }

    @PostMapping("/versions/{versionId}/deploy")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_MODEL_OPERATOR"})
    public DeploymentTarget deployVersion(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                           @PathVariable String versionId,
                                           @RequestBody DeployRequest request) {
        DeploymentConfig config = new DeploymentConfig(
                request.deploymentStrategy(), request.environmentVariables(),
                request.healthCheckEndpoint(), request.replicas(), request.autoRollback());
        return deploymentService.deploy(tenantId, versionId, request.targetType(), request.endpointUrl(), config, request.actorId());
    }

    @GetMapping("/deployments")
    public List<DeploymentTarget> activeDeployments(@RequestParam(defaultValue = "demo-tenant") String tenantId) {
        return deploymentService.getActiveDeployments(tenantId);
    }

    @PostMapping("/deployments/{deploymentId}/rollback")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN"})
    public DeploymentTarget rollbackDeployment(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                                @PathVariable String deploymentId,
                                                @RequestBody Map<String, String> body) {
        return deploymentService.rollback(tenantId, deploymentId, body.getOrDefault("actorId", "system"));
    }

    @PostMapping("/deployments/{deploymentId}/health-check")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_MODEL_OPERATOR"})
    public boolean healthCheckDeployment(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                          @PathVariable String deploymentId) {
        return deploymentService.healthCheck(tenantId, deploymentId);
    }

    public record RegisterModelRequest(
            String tenantId, String modelName, String framework, String taskType,
            String domainType, String artifactUri, String trainingJobId,
            String datasetVersionId, String actorId) {}

    public record PromoteRequest(ModelLifecycleStage targetStage, String actorId) {}

    public record RollbackRequest(int targetVersion, String actorId) {}

    public record EvaluateRequest(String testDatasetVersionId, double minThreshold, String actorId) {}

    public record DeployRequest(
            String targetType, String endpointUrl, String deploymentStrategy,
            Map<String, String> environmentVariables, String healthCheckEndpoint,
            int replicas, boolean autoRollback, String actorId) {}
}
