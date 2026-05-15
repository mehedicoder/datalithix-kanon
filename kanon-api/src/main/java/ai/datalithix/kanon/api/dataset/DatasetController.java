package ai.datalithix.kanon.api.dataset;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.model.SortDirection;
import ai.datalithix.kanon.dataset.model.CurationRule;
import ai.datalithix.kanon.dataset.model.DatasetDefinition;
import ai.datalithix.kanon.dataset.model.DatasetVersion;
import ai.datalithix.kanon.dataset.model.ExportFormat;
import ai.datalithix.kanon.dataset.model.SplitStrategy;
import ai.datalithix.kanon.dataset.service.DatasetCurationService;
import ai.datalithix.kanon.dataset.service.DatasetRepository;
import java.util.List;
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

import static ai.datalithix.kanon.api.support.ApiQuerySupport.dimensions;
import static ai.datalithix.kanon.api.support.ApiQuerySupport.query;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {
    private final DatasetRepository datasetRepository;
    private final DatasetCurationService curationService;

    public DatasetController(DatasetRepository datasetRepository, DatasetCurationService curationService) {
        this.datasetRepository = datasetRepository;
        this.curationService = curationService;
    }

    @GetMapping
    public PageResult<DatasetDefinition> listDefinitions(
            @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "ASC") SortDirection sortDirection,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String domainType,
            @RequestParam(required = false) String enabled) {
        return datasetRepository.findPage(query(tenantId, page, size, sortBy, sortDirection,
                dimensions("name", name, "domainType", domainType, "enabled", enabled)));
    }

    @GetMapping("/{datasetId}")
    public DatasetDefinition getDefinition(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                            @PathVariable String datasetId) {
        return datasetRepository.findById(tenantId, datasetId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Dataset not found: " + datasetId));
    }

    @PostMapping
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    public DatasetDefinition createDefinition(@RequestBody CreateDatasetRequest request) {
        DatasetDefinition def = new DatasetDefinition(
                request.datasetDefinitionId(), request.tenantId(), request.name(),
                request.description(), request.domainType(), Set.of(),
                null, SplitStrategy.RANDOM, 0.8, 0.1, 0.1,
                List.of(ExportFormat.JSONL), "EU", true, 0,
                new ai.datalithix.kanon.common.model.AuditMetadata(
                        java.time.Instant.now(), request.actorId(),
                        java.time.Instant.now(), request.actorId(), 1));
        return datasetRepository.save(def);
    }

    @GetMapping("/{datasetId}/versions")
    public List<DatasetVersion> listVersions(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                              @PathVariable String datasetId) {
        return datasetRepository.findVersionsByDefinitionId(tenantId, datasetId);
    }

    @GetMapping("/{datasetId}/versions/latest")
    public DatasetVersion latestVersion(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                         @PathVariable String datasetId) {
        return datasetRepository.findLatestVersion(tenantId, datasetId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No versions for dataset: " + datasetId));
    }

    @PostMapping("/{datasetId}/curate")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    public DatasetVersion curate(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                  @PathVariable String datasetId,
                                  @RequestBody CurateRequest request) {
        DatasetDefinition def = datasetRepository.findById(tenantId, datasetId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Dataset not found: " + datasetId));
        if (request.rule() != null) {
            return curationService.curateWithRule(def, request.rule(), request.actorId());
        }
        return curationService.curate(def, request.actorId());
    }

    @PostMapping("/versions/{versionId}/export")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    public List<ExportFormat> exportVersion(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                             @PathVariable String versionId,
                                             @RequestBody ExportRequest request) {
        DatasetVersion version = datasetRepository.findVersionById(tenantId, versionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Version not found: " + versionId));
        return curationService.export(version, request.formats());
    }

    public record CreateDatasetRequest(
            String datasetDefinitionId, String tenantId, String name, String description,
            String domainType, String actorId) {}

    public record CurateRequest(String actorId, CurationRule rule) {}

    public record ExportRequest(List<ExportFormat> formats) {}
}
