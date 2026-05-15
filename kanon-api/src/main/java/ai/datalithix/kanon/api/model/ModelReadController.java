package ai.datalithix.kanon.api.model;

import ai.datalithix.kanon.airouting.model.ChatModelRoute;
import ai.datalithix.kanon.airouting.model.ModelProfile;
import ai.datalithix.kanon.airouting.service.ModelProfileRepository;
import ai.datalithix.kanon.airouting.service.ModelRouter;
import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.TenantContext;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.SortDirection;
import ai.datalithix.kanon.domain.model.TaskDescriptor;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static ai.datalithix.kanon.api.support.ApiQuerySupport.dimensions;
import static ai.datalithix.kanon.api.support.ApiQuerySupport.query;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;

@RestController
@RequestMapping("/api/models")
public class ModelReadController {
    private final ModelRouter modelRouter;
    private final ObjectProvider<ModelProfileRepository> modelProfileRepository;

    public ModelReadController(ModelRouter modelRouter, ObjectProvider<ModelProfileRepository> modelProfileRepository) {
        this.modelRouter = modelRouter;
        this.modelProfileRepository = modelProfileRepository;
    }

    @GetMapping("/route")
    public ChatModelRoute route(@RequestParam(defaultValue = "demo-tenant") String tenantId,
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
                Set.of("AUDIT_REQUIRED")
        );
        return modelRouter.resolve(tenantContext, new TaskDescriptor(taskType, caseId, "memory://sample", "v1", false));
    }

    @GetMapping("/profiles")
    public PageResult<ModelProfile> profiles(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size,
                                             @RequestParam(required = false) String sortBy,
                                             @RequestParam(defaultValue = "ASC") SortDirection sortDirection,
                                             @RequestParam(required = false) String enabled,
                                             @RequestParam(required = false) String provider,
                                             @RequestParam(required = false) String backendType) {
        return repository().findPage(query(
                tenantId,
                page,
                size,
                sortBy,
                sortDirection,
                dimensions("enabled", enabled, "provider", provider, "backendType", backendType)
        ));
    }

    @GetMapping("/profiles/{profileKey}")
    public ModelProfile profile(@RequestParam(defaultValue = "demo-tenant") String tenantId,
                                @PathVariable String profileKey) {
        return repository().findByProfileKey(tenantId, profileKey)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Model profile not found: " + profileKey));
    }

    private ModelProfileRepository repository() {
        ModelProfileRepository repository = modelProfileRepository.getIfAvailable();
        if (repository == null) {
            throw new ResponseStatusException(NOT_IMPLEMENTED, "Model profile repository is not configured");
        }
        return repository;
    }
}
