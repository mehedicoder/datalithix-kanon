package ai.datalithix.kanon.config.service;

import ai.datalithix.kanon.config.model.ConfigValidationIssue;
import ai.datalithix.kanon.config.model.ConfigValidationResult;
import ai.datalithix.kanon.config.model.ConfigValidationSeverity;
import ai.datalithix.kanon.config.model.AgentDefinition;
import ai.datalithix.kanon.config.model.ConnectorDefinition;
import ai.datalithix.kanon.config.model.DomainConfiguration;
import ai.datalithix.kanon.config.model.ModelRoutingPolicy;
import ai.datalithix.kanon.config.model.KanonConfigurationPack;
import ai.datalithix.kanon.config.model.PolicyTemplate;
import ai.datalithix.kanon.config.model.TenantConfiguration;
import ai.datalithix.kanon.config.model.WorkflowTemplate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RequiredFieldConfigurationValidator implements ConfigurationValidator {
    @Override
    public ConfigValidationResult validate(KanonConfigurationPack pack) {
        List<ConfigValidationIssue> issues = new ArrayList<>();
        require(pack.id(), "id", "Configuration pack id is required", issues);
        require(pack.displayName(), "displayName", "Configuration pack display name is required", issues);

        Set<String> globalIds = new HashSet<>();
        Set<String> domainIds = new HashSet<>();
        Set<String> tenantIds = new HashSet<>();
        Set<String> workflowIds = new HashSet<>();
        Set<String> agentIds = new HashSet<>();
        Set<String> routingPolicyIds = new HashSet<>();
        Set<String> connectorIds = new HashSet<>();
        Set<String> policyIds = new HashSet<>();

        if (pack.domains() != null) {
            for (DomainConfiguration domain : pack.domains()) {
                requireUnique(domain.id(), "domains[].id", globalIds, domainIds, issues);
                requireNotNull(domain.domainType(), "domains[%s].domainType".formatted(domain.id()), "Domain type is required", issues);
            }
        }
        if (pack.tenants() != null) {
            for (TenantConfiguration tenant : pack.tenants()) {
                requireUnique(tenant.tenantId(), "tenants[].tenantId", globalIds, tenantIds, issues);
                requireNotNull(tenant.domainType(), "tenants[%s].domainType".formatted(tenant.tenantId()), "Tenant domain type is required", issues);
            }
        }
        if (pack.workflows() != null) {
            for (WorkflowTemplate workflow : pack.workflows()) {
                requireUnique(workflow.id(), "workflows[].id", globalIds, workflowIds, issues);
                requireNotNull(workflow.domainType(), "workflows[%s].domainType".formatted(workflow.id()), "Workflow domain type is required", issues);
                requireNotNull(workflow.taskType(), "workflows[%s].taskType".formatted(workflow.id()), "Workflow task type is required", issues);
            }
        }
        if (pack.agents() != null) {
            for (AgentDefinition agent : pack.agents()) {
                requireUnique(agent.id(), "agents[].id", globalIds, agentIds, issues);
                require(agent.agentType(), "agents[%s].agentType".formatted(agent.id()), "Agent type is required", issues);
            }
        }
        if (pack.modelRoutingPolicies() != null) {
            for (ModelRoutingPolicy policy : pack.modelRoutingPolicies()) {
                requireUnique(policy.id(), "modelRoutingPolicies[].id", globalIds, routingPolicyIds, issues);
            }
        }
        if (pack.connectors() != null) {
            for (ConnectorDefinition connector : pack.connectors()) {
                requireUnique(connector.id(), "connectors[].id", globalIds, connectorIds, issues);
                requireNotNull(connector.sourceCategory(), "connectors[%s].sourceCategory".formatted(connector.id()), "Connector source category is required", issues);
                requireNotNull(connector.sourceType(), "connectors[%s].sourceType".formatted(connector.id()), "Connector source type is required", issues);
            }
        }
        if (pack.policies() != null) {
            for (PolicyTemplate policy : pack.policies()) {
                requireUnique(policy.id(), "policies[].id", globalIds, policyIds, issues);
            }
        }

        validateReferences(pack, workflowIds, agentIds, routingPolicyIds, connectorIds, policyIds, issues);
        return new ConfigValidationResult(List.copyOf(issues));
    }

    private void require(String value, String path, String message, List<ConfigValidationIssue> issues) {
        if (value == null || value.isBlank()) {
            issues.add(new ConfigValidationIssue(ConfigValidationSeverity.ERROR, path, message));
        }
    }

    private void requireNotNull(Object value, String path, String message, List<ConfigValidationIssue> issues) {
        if (value == null) {
            issues.add(new ConfigValidationIssue(ConfigValidationSeverity.ERROR, path, message));
        }
    }

    private void requireUnique(
            String value,
            String path,
            Set<String> globalIds,
            Set<String> typedIds,
            List<ConfigValidationIssue> issues
    ) {
        require(value, path, "Id is required", issues);
        if (value != null && !value.isBlank() && !typedIds.add(value)) {
            issues.add(new ConfigValidationIssue(ConfigValidationSeverity.ERROR, path, "Duplicate id: " + value));
        }
        if (value != null && !value.isBlank() && !globalIds.add(value)) {
            issues.add(new ConfigValidationIssue(ConfigValidationSeverity.WARNING, path, "Id is reused across configuration types: " + value));
        }
    }

    private void validateReferences(
            KanonConfigurationPack pack,
            Set<String> workflowIds,
            Set<String> agentIds,
            Set<String> routingPolicyIds,
            Set<String> connectorIds,
            Set<String> policyIds,
            List<ConfigValidationIssue> issues
    ) {
        if (pack.domains() != null) {
            for (DomainConfiguration domain : pack.domains()) {
                requireReferences(domain.workflowTemplateIds(), workflowIds, "domains[%s].workflowTemplateIds".formatted(domain.id()), issues);
                requireReferences(domain.agentDefinitionIds(), agentIds, "domains[%s].agentDefinitionIds".formatted(domain.id()), issues);
                requireReferences(domain.ruleIds(), collectRuleIds(pack.policies()), "domains[%s].ruleIds".formatted(domain.id()), issues);
            }
        }
        if (pack.tenants() != null) {
            for (TenantConfiguration tenant : pack.tenants()) {
                requireReference(tenant.modelRoutingPolicyId(), routingPolicyIds, "tenants[%s].modelRoutingPolicyId".formatted(tenant.tenantId()), issues);
                requireReferences(tenant.enabledConnectorIds(), connectorIds, "tenants[%s].enabledConnectorIds".formatted(tenant.tenantId()), issues);
                requireReferences(tenant.enabledPolicyIds(), policyIds, "tenants[%s].enabledPolicyIds".formatted(tenant.tenantId()), issues);
            }
        }
        if (pack.agents() != null) {
            for (AgentDefinition agent : pack.agents()) {
                requireReference(agent.modelRoutePolicy(), routingPolicyIds, "agents[%s].modelRoutePolicy".formatted(agent.id()), issues);
            }
        }
    }

    private Set<String> collectRuleIds(Collection<PolicyTemplate> policies) {
        Set<String> ruleIds = new HashSet<>();
        if (policies != null) {
            for (PolicyTemplate policy : policies) {
                if (policy.ruleIds() != null) {
                    ruleIds.addAll(policy.ruleIds());
                }
            }
        }
        return ruleIds;
    }

    private void requireReferences(Collection<String> values, Set<String> validIds, String path, List<ConfigValidationIssue> issues) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            requireReference(value, validIds, path, issues);
        }
    }

    private void requireReference(String value, Set<String> validIds, String path, List<ConfigValidationIssue> issues) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!validIds.contains(value)) {
            issues.add(new ConfigValidationIssue(ConfigValidationSeverity.ERROR, path, "Unknown reference: " + value));
        }
    }
}
