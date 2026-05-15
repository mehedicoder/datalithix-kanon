package ai.datalithix.kanon.api.config;

import ai.datalithix.kanon.config.model.ActiveConfigurationVersion;
import ai.datalithix.kanon.config.model.AgentDefinition;
import ai.datalithix.kanon.config.model.ConnectorDefinition;
import ai.datalithix.kanon.config.model.DomainConfiguration;
import ai.datalithix.kanon.config.model.ModelRoutingPolicy;
import ai.datalithix.kanon.config.model.KanonConfigurationPack;
import ai.datalithix.kanon.config.model.PolicyTemplate;
import ai.datalithix.kanon.config.model.TenantConfiguration;
import ai.datalithix.kanon.config.model.WorkflowTemplate;
import ai.datalithix.kanon.config.service.ActiveConfigurationVersionRepository;
import ai.datalithix.kanon.config.service.ConfigurationRegistry;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/admin/config")
public class ConfigurationAdminController {
    private final ConfigurationRegistry registry;
    private final ActiveConfigurationVersionRepository activeConfigurationVersions;
    private final CurrentUserContextService currentUserContextService;

    public ConfigurationAdminController(
            ConfigurationRegistry registry,
            ActiveConfigurationVersionRepository activeConfigurationVersions,
            CurrentUserContextService currentUserContextService
    ) {
        this.registry = registry;
        this.activeConfigurationVersions = activeConfigurationVersions;
        this.currentUserContextService = currentUserContextService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        requireConfigRead();
        CurrentUserContext context = currentUserContextService.currentUser();
        return Map.of(
                "packs", registry.packs().size(),
                "domains", registry.domains().size(),
                "tenants", registry.tenants().size(),
                "workflows", registry.workflows().size(),
                "agents", registry.agents().size(),
                "modelRoutingPolicies", registry.modelRoutingPolicies().size(),
                "connectors", registry.connectors().size(),
                "policies", registry.policies().size(),
                "activeConfigurations", activeVersions(context, null).size()
        );
    }

    @GetMapping("/packs")
    public List<KanonConfigurationPack> packs() {
        requireConfigRead();
        return registry.packs();
    }

    @GetMapping("/packs/{id}")
    public KanonConfigurationPack pack(@PathVariable String id) {
        requireConfigRead();
        return registry.pack(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Configuration pack not found: " + id));
    }

    @GetMapping("/domains")
    public List<DomainConfiguration> domains() {
        requireConfigRead();
        return registry.domains();
    }

    @GetMapping("/tenants")
    public List<TenantConfiguration> tenants() {
        requireConfigRead();
        return registry.tenants();
    }

    @GetMapping("/workflows")
    public List<WorkflowTemplate> workflows() {
        requireConfigRead();
        return registry.workflows();
    }

    @GetMapping("/agents")
    public List<AgentDefinition> agents() {
        requireConfigRead();
        return registry.agents();
    }

    @GetMapping("/model-routing-policies")
    public List<ModelRoutingPolicy> modelRoutingPolicies() {
        requireConfigRead();
        return registry.modelRoutingPolicies();
    }

    @GetMapping("/connectors")
    public List<ConnectorDefinition> connectors() {
        requireConfigRead();
        return registry.connectors();
    }

    @GetMapping("/policies")
    public List<PolicyTemplate> policies() {
        requireConfigRead();
        return registry.policies();
    }

    @GetMapping("/active")
    public List<ActiveConfigurationVersion> active(@RequestParam(required = false) String tenantId) {
        CurrentUserContext context = currentUserContextService.currentUser();
        requireConfigRead();
        return activeVersions(context, tenantId);
    }

    private List<ActiveConfigurationVersion> activeVersions(CurrentUserContext context, String tenantId) {
        if (isPlatformConfigReader(context)) {
            if (tenantId == null || tenantId.isBlank()) {
                return activeConfigurationVersions.findAllActive();
            }
            return activeConfigurationVersions.findActiveByTenant(tenantId);
        }
        String effectiveTenantId = tenantId == null || tenantId.isBlank() ? context.activeTenantId() : tenantId;
        if (!context.activeTenantId().equals(effectiveTenantId)) {
            throw new ResponseStatusException(FORBIDDEN, "Configuration access denied outside tenant scope");
        }
        return activeConfigurationVersions.findActiveByTenant(effectiveTenantId);
    }

    private void requireConfigRead() {
        CurrentUserContext context = currentUserContextService.currentUser();
        if (isPlatformConfigReader(context)
                || has(context, "tenant.config.read")
                || has(context, "tenant.config.manage")) {
            return;
        }
        throw new ResponseStatusException(FORBIDDEN, "Configuration access denied");
    }

    private boolean isPlatformConfigReader(CurrentUserContext context) {
        return has(context, "platform.config.read") || has(context, "platform.config.manage");
    }

    private boolean has(CurrentUserContext context, String permission) {
        return context.permissions().contains(permission);
    }
}
