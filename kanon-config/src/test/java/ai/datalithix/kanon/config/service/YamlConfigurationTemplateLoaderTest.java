package ai.datalithix.kanon.config.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.datalithix.kanon.config.model.AgentDefinition;
import ai.datalithix.kanon.config.model.ActiveConfigurationVersion;
import ai.datalithix.kanon.config.model.ConfigurationType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.config.model.DomainConfiguration;
import ai.datalithix.kanon.config.model.KanonConfigurationPack;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class YamlConfigurationTemplateLoaderTest {
    private final YamlConfigurationTemplateLoader loader = new YamlConfigurationTemplateLoader(new DefaultResourceLoader());
    private final RequiredFieldConfigurationValidator validator = new RequiredFieldConfigurationValidator();

    @Test
    void loadsAccountingPack() {
        KanonConfigurationPack pack = loader.load("classpath:config/templates/accounting-pack.yml");

        assertEquals("accounting-pack", pack.id());
        assertEquals(DomainType.ACCOUNTING, pack.domains().getFirst().domainType());
        assertEquals("accounting-demo", pack.tenants().getFirst().tenantId());
        assertEquals("invoice-processing", pack.workflows().getFirst().id());
        assertTrue(validator.validate(pack).valid());
    }

    @Test
    void loadsHrPack() {
        KanonConfigurationPack pack = loader.load("classpath:config/templates/hr-pack.yml");

        assertEquals("hr-pack", pack.id());
        assertEquals(DomainType.HR, pack.domains().getFirst().domainType());
        assertEquals("hr-demo", pack.tenants().getFirst().tenantId());
        assertEquals("candidate-screening", pack.workflows().getFirst().id());
        assertTrue(validator.validate(pack).valid());
    }

    @Test
    void rejectsMissingTemplate() {
        assertThrows(
                ConfigurationTemplateLoadException.class,
                () -> loader.load("classpath:config/templates/missing.yml")
        );
    }

    @Test
    void detectsDuplicateIds() {
        KanonConfigurationPack pack = loader.load("classpath:config/templates/accounting-pack.yml");
        List<DomainConfiguration> domains = new ArrayList<>(pack.domains());
        domains.add(pack.domains().getFirst());
        KanonConfigurationPack duplicatePack = new KanonConfigurationPack(
                pack.id(),
                pack.displayName(),
                domains,
                pack.tenants(),
                pack.workflows(),
                pack.agents(),
                pack.modelRoutingPolicies(),
                pack.connectors(),
                pack.policies()
        );

        assertFalse(validator.validate(duplicatePack).issues().isEmpty());
    }

    @Test
    void detectsBrokenModelRoutingReference() {
        KanonConfigurationPack pack = loader.load("classpath:config/templates/accounting-pack.yml");
        List<AgentDefinition> agents = new ArrayList<>(pack.agents());
        AgentDefinition original = agents.getFirst();
        agents.set(0, new AgentDefinition(
                original.id(),
                original.displayName(),
                original.agentType(),
                original.supportedDomains(),
                original.supportedTaskTypes(),
                original.supportedAssetTypes(),
                original.supportedSourceTypes(),
                original.inputSchemaRef(),
                original.outputSchemaRef(),
                "missing-routing-policy"
        ));
        KanonConfigurationPack brokenPack = new KanonConfigurationPack(
                pack.id(),
                pack.displayName(),
                pack.domains(),
                pack.tenants(),
                pack.workflows(),
                agents,
                pack.modelRoutingPolicies(),
                pack.connectors(),
                pack.policies()
        );

        assertFalse(validator.validate(brokenPack).valid());
    }

    @Test
    void importsActiveConfigurationVersions() {
        KanonConfigurationPack pack = loader.load("classpath:config/templates/accounting-pack.yml");
        InMemoryActiveConfigurationVersionRepository repository = new InMemoryActiveConfigurationVersionRepository();
        InMemoryConfigurationRegistry registry = new InMemoryConfigurationRegistry();
        DefaultConfigurationSeedImporter importer = new DefaultConfigurationSeedImporter(repository, validator, registry);

        List<ActiveConfigurationVersion> versions = importer.importPack("accounting-demo", pack, "admin", "Initial seed");

        assertFalse(versions.isEmpty());
        assertTrue(registry.domain("accounting").isPresent());
        ActiveConfigurationVersion activeWorkflow = repository.findActive(
                "accounting-demo",
                ConfigurationType.WORKFLOW_TEMPLATE,
                "invoice-processing"
        ).orElseThrow();
        assertEquals("accounting-pack", activeWorkflow.templateId());
        assertNotNull(activeWorkflow.activatedAt());
    }
}
