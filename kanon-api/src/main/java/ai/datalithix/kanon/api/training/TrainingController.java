package ai.datalithix.kanon.api.training;

import ai.datalithix.kanon.training.model.ComputeBackend;
import ai.datalithix.kanon.training.model.ComputeBackendType;
import ai.datalithix.kanon.training.model.HyperParameterConfig;
import ai.datalithix.kanon.training.model.TrainingJob;
import ai.datalithix.kanon.training.service.TrainingJobRepository;
import ai.datalithix.kanon.training.service.TrainingOrchestrationService;
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
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/training")
public class TrainingController {
    private final TrainingOrchestrationService orchestrationService;
    private final TrainingJobRepository trainingJobRepository;

    public TrainingController(
            TrainingOrchestrationService orchestrationService,
            TrainingJobRepository trainingJobRepository
    ) {
        this.orchestrationService = orchestrationService;
        this.trainingJobRepository = trainingJobRepository;
    }

    @PostMapping("/jobs")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER", "ROLE_MODEL_OPERATOR"})
    public TrainingJob submitJob(@RequestBody SubmitJobRequest request) {
        HyperParameterConfig hyperParams = new HyperParameterConfig(
                request.framework(), request.modelArchitecture(),
                request.epochs(), request.batchSize(), request.learningRate(),
                request.extraParams() == null ? Map.of() : request.extraParams());
        return orchestrationService.submitJob(
                request.tenantId(), request.datasetVersionId(), request.datasetDefinitionId(),
                request.computeBackendId(), request.modelName(), hyperParams, request.actorId());
    }

    @GetMapping("/jobs")
    public List<TrainingJob> listJobs(@RequestParam(defaultValue = "demo-tenant") String tenantId) {
        return orchestrationService.listJobs(tenantId);
    }

    @GetMapping("/jobs/{jobId}")
    public TrainingJob getJob(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                               @PathVariable String jobId) {
        return trainingJobRepository.findById(tenantId, jobId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Training job not found: " + jobId));
    }

    @PostMapping("/jobs/{jobId}/cancel")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_MODEL_OPERATOR"})
    public TrainingJob cancelJob(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                  @PathVariable String jobId) {
        return orchestrationService.cancelJob(tenantId, jobId);
    }

    @PostMapping("/backends")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN"})
    public ComputeBackend registerBackend(@RequestBody RegisterBackendRequest request) {
        ComputeBackend backend = new ComputeBackend(
                request.backendId(), request.tenantId(), request.backendType(), request.name(),
                request.endpointUrl(), request.credentialRef(), request.configuration(),
                true, false, null, null);
        return orchestrationService.registerBackend(backend);
    }

    @GetMapping("/backends")
    public List<ComputeBackend> listBackends(@RequestParam(defaultValue = "demo-tenant") String tenantId) {
        return trainingJobRepository.findBackendsByTenant(tenantId);
    }

    @GetMapping("/backends/{backendId}/health")
    public boolean healthCheck(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                @PathVariable String backendId) {
        return orchestrationService.healthCheckBackend(tenantId, backendId);
    }

    public record SubmitJobRequest(
            String tenantId, String datasetVersionId, String datasetDefinitionId,
            String computeBackendId, String modelName, String framework,
            String modelArchitecture, int epochs, int batchSize, double learningRate,
            Map<String, String> extraParams, String actorId) {}

    public record RegisterBackendRequest(
            String backendId, String tenantId, ComputeBackendType backendType,
            String name, String endpointUrl, String credentialRef,
            Map<String, String> configuration) {}
}
