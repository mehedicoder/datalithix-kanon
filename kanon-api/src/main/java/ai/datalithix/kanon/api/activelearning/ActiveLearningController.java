package ai.datalithix.kanon.api.activelearning;

import ai.datalithix.kanon.activelearning.model.ActiveLearningCycle;
import ai.datalithix.kanon.activelearning.model.ActiveLearningConfig;
import ai.datalithix.kanon.activelearning.model.CycleStatus;
import ai.datalithix.kanon.activelearning.model.SelectionStrategyType;
import ai.datalithix.kanon.activelearning.service.ActiveLearningCycleRepository;
import ai.datalithix.kanon.activelearning.service.ActiveLearningOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/active-learning")
@Tag(name = "Active Learning", description = "Active learning cycle management endpoints")
public class ActiveLearningController {
    private final ActiveLearningOrchestrator orchestrator;
    private final ActiveLearningCycleRepository cycleRepository;

    public ActiveLearningController(
            ActiveLearningOrchestrator orchestrator,
            ActiveLearningCycleRepository cycleRepository
    ) {
        this.orchestrator = orchestrator;
        this.cycleRepository = cycleRepository;
    }

    @GetMapping("/cycles")
    @Operation(summary = "List all active learning cycles", description = "Returns all cycles for a tenant, ordered by start date descending")
    @ApiResponse(responseCode = "200", description = "List of cycles retrieved")
    public List<ActiveLearningCycle> listCycles(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId) {
        return cycleRepository.findByTenant(tenantId);
    }

    @GetMapping("/cycles/{cycleId}")
    @Operation(summary = "Get a single active learning cycle by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cycle found"),
            @ApiResponse(responseCode = "404", description = "Cycle not found")
    })
    public ActiveLearningCycle getCycle(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @Parameter(description = "Cycle ID") @PathVariable String cycleId) {
        return cycleRepository.findById(tenantId, cycleId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Cycle not found: " + cycleId));
    }

    @PostMapping("/cycles")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER", "ROLE_MODEL_OPERATOR"})
    @Operation(summary = "Start a new active learning cycle",
            description = "Creates and launches a new active learning cycle with the given strategy and configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cycle started"),
            @ApiResponse(responseCode = "400", description = "Invalid configuration")
    })
    public ActiveLearningCycle startCycle(@RequestBody StartCycleRequest request) {
        var config = new ActiveLearningConfig(
                request.strategy(), request.minConfidenceThreshold(), request.maxRecordsPerCycle(),
                request.minNewRecordsForRetraining(), request.autoTriggerRetraining(),
                request.scheduleCron(), request.enabled(), request.strategyParams());
        return orchestrator.startCycle(
                request.tenantId(), request.modelEntryId(), request.modelVersionId(),
                request.sourceDatasetVersionId(), config, List.of(), request.actorId());
    }

    @Schema(description = "Request payload to start a new active learning cycle")
    public record StartCycleRequest(
            @Schema(description = "Tenant ID", example = "demo-tenant") String tenantId,
            @Schema(description = "Model entry ID") String modelEntryId,
            @Schema(description = "Model version ID") String modelVersionId,
            @Schema(description = "Source dataset version ID") String sourceDatasetVersionId,
            @Schema(description = "Selection strategy") SelectionStrategyType strategy,
            @Schema(description = "Minimum confidence threshold", example = "0.85") double minConfidenceThreshold,
            @Schema(description = "Maximum records per cycle", example = "100") int maxRecordsPerCycle,
            @Schema(description = "Minimum new records to trigger retraining", example = "50") int minNewRecordsForRetraining,
            @Schema(description = "Auto-trigger retraining when threshold met") boolean autoTriggerRetraining,
            @Schema(description = "Cron schedule expression") String scheduleCron,
            @Schema(description = "Whether the cycle config is enabled") boolean enabled,
            @Schema(description = "Strategy-specific parameters") Map<String, String> strategyParams,
            @Schema(description = "Actor ID who requested the cycle") String actorId) {}

    @Schema(description = "Request payload to update a cycle's status")
    public record UpdateStatusRequest(
            @Schema(description = "New status to transition to") CycleStatus newStatus,
            @Schema(description = "Target dataset version ID (for transitions that produce a dataset)") String targetDatasetVersionId,
            @Schema(description = "Actor ID performing the update") String actorId) {}
}
