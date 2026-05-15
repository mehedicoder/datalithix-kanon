package ai.datalithix.kanon.api.source;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.PageSpec;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.model.SortDirection;
import ai.datalithix.kanon.ingestion.model.IngestionRequest;
import ai.datalithix.kanon.ingestion.model.IngestionResult;
import ai.datalithix.kanon.ingestion.model.SourceDescriptor;
import ai.datalithix.kanon.ingestion.model.SourcePayload;
import ai.datalithix.kanon.ingestion.model.SourceTrace;
import ai.datalithix.kanon.ingestion.service.IngestionOrchestrationService;
import ai.datalithix.kanon.ingestion.service.SourceTraceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
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
@RequestMapping("/api/sources")
@Tag(name = "Source Ingestion", description = "Ingest source data through configured connectors")
public class IngestionController {
    private final IngestionOrchestrationService ingestionService;
    private final SourceTraceRepository sourceTraceRepository;

    public IngestionController(IngestionOrchestrationService ingestionService,
                               SourceTraceRepository sourceTraceRepository) {
        this.ingestionService = ingestionService;
        this.sourceTraceRepository = sourceTraceRepository;
    }

    @PostMapping("/ingest")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    @Operation(summary = "Ingest source data",
            description = "Routes an ingestion request to the appropriate connector and records traceability")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ingestion completed or accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid request or unsupported source type"),
            @ApiResponse(responseCode = "500", description = "Ingestion failed")
    })
    public IngestionResult ingest(@RequestBody IngestRequest request) {
        var ingestionRequest = new IngestionRequest(
                null, request.tenantId(), null, request.source(), request.payload(),
                request.idempotencyKey, request.caseId(), request.correlationId(),
                Instant.now(), request.attributes(), null
        );
        return ingestionService.ingest(ingestionRequest);
    }

    @GetMapping("/traces")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    @Operation(summary = "List source traces", description = "Paginated list of source traces for a tenant")
    public PageResult<SourceTrace> listTraces(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return sourceTraceRepository.findPage(
                new QuerySpec(tenantId, new PageSpec(page, size, null, SortDirection.ASC), null, null));
    }

    @GetMapping("/traces/{traceId}")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    @Operation(summary = "Get source trace by ID")
    public SourceTrace getTrace(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @Parameter(description = "Trace ID") @PathVariable String traceId) {
        return sourceTraceRepository.findById(tenantId, traceId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Trace not found: " + traceId));
    }

    @Schema(description = "Request payload to ingest source data")
    public record IngestRequest(
            @Schema(description = "Tenant ID", example = "demo-tenant") String tenantId,
            @Schema(description = "Source descriptor") SourceDescriptor source,
            @Schema(description = "Source payload") SourcePayload payload,
            @Schema(description = "Idempotency key for retry-safe ingestion") String idempotencyKey,
            @Schema(description = "Case ID to associate with this ingestion") String caseId,
            @Schema(description = "Correlation ID for traceability") String correlationId,
            @Schema(description = "Additional attributes") Map<String, String> attributes) {}
}
