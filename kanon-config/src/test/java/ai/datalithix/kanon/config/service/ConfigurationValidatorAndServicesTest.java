package ai.datalithix.kanon.config.service;

import static org.junit.jupiter.api.Assertions.*;

import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.DataResidency;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.config.model.ActiveConfigurationVersion;
import ai.datalithix.kanon.config.model.AgentDefinition;
import ai.datalithix.kanon.config.model.ConfigValidationResult;
import ai.datalithix.kanon.config.model.ConfigValidationSeverity;
import ai.datalithix.kanon.config.model.ConfigurationActivationState;
import ai.datalithix.kanon.config.model.ConfigurationType;
import ai.datalithix.kanon.config.model.ConnectorDefinition;
import ai.datalithix.kanon.config.model.DomainConfiguration;
import ai.datalithix.kanon.config.model.KanonConfigurationPack;
import ai.datalithix.kanon.config.model.ModelRoutingPolicy;
import ai.datalithix.kanon.config.model.PolicyTemplate;
import ai.datalithix.kanon.config.model.TenantConfiguration;
import ai.datalithix.kanon.config.model.WorkflowTemplate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConfigurationValidatorAndServicesTest {

    private RequiredFieldConfigurationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RequiredFieldConfigurationValidator();
    }

    private static KanonConfigurationPack validPack() {
        return new KanonConfigurationPack(
                "test-pack", "Test Pack",
                List.of(new DomainConfiguration("accounting", "Accounting", DomainType.ACCOUNTING,
                        List.of(), List.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of())),
                null, null, null, null, null, null
        );
    }

    @Nested
    class ValidatorTests {

        @Test
        void acceptsValidPack() {
            var result = validator.validate(validPack());
            assertTrue(result.valid());
        }

        @Test
        void rejectsNullPackId() {
            var pack = new KanonConfigurationPack(null, "name", null, null, null, null, null, null, null);
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "id");
        }

        @Test
        void rejectsBlankPackId() {
            var pack = new KanonConfigurationPack("", "name", null, null, null, null, null, null, null);
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "id");
        }

        @Test
        void rejectsNullDisplayName() {
            var pack = new KanonConfigurationPack("id", null, null, null, null, null, null, null, null);
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "displayName");
        }

        @Test
        void rejectsBlankDisplayName() {
            var pack = new KanonConfigurationPack("id", "", null, null, null, null, null, null, null);
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "displayName");
        }

        @Test
        void rejectsNullDomainType() {
            var pack = new KanonConfigurationPack("p", "p", List.of(
                    new DomainConfiguration("d", "d", null, List.of(), List.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of())
            ), null, null, null, null, null, null);
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "domainType");
        }

        @Test
        void rejectsDuplicateDomainIds() {
            var pack = new KanonConfigurationPack("p", "p", List.of(
                    new DomainConfiguration("dup", "d1", DomainType.ACCOUNTING, List.of(), List.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of()),
                    new DomainConfiguration("dup", "d2", DomainType.HR, List.of(), List.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of())
            ), null, null, null, null, null, null);
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "Duplicate id: dup");
        }

        @Test
        void rejectsDuplicateTenantIds() {
            var pack = new KanonConfigurationPack("p", "p", null, List.of(
                    new TenantConfiguration("t1", "t1", DomainType.ACCOUNTING, "DE", null, DataResidency.EU, true, false, Set.of(), Set.of(), null, Map.of()),
                    new TenantConfiguration("t1", "t2", DomainType.HR, "US", null, DataResidency.US, false, true, Set.of(), Set.of(), null, Map.of())
            ), null, null, null, null, null);
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "Duplicate id: t1");
        }

        @Test
        void rejectsDuplicateWorkflowIds() {
            var pack = new KanonConfigurationPack("p", "p", null, null, List.of(
                    new WorkflowTemplate("w", "w", DomainType.ACCOUNTING, AiTaskType.EXTRACTION, List.of(), "default", null),
                    new WorkflowTemplate("w", "w2", DomainType.HR, AiTaskType.CLASSIFICATION, List.of(), "default", null)
            ), null, null, null, null);
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "Duplicate id: w");
        }

        @Test
        void rejectsDuplicateAgentIds() {
            var pack = new KanonConfigurationPack("p", "p", null, null, null, List.of(
                    new AgentDefinition("a", "a", "extraction", Set.of(), Set.of(), Set.of(), Set.of(), null, null, null),
                    new AgentDefinition("a", "b", "classification", Set.of(), Set.of(), Set.of(), Set.of(), null, null, null)
            ), null, null, null);
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "Duplicate id: a");
        }

        @Test
        void rejectsDuplicateConnectorIds() {
            var pack = new KanonConfigurationPack("p", "p", null, null, null, null, null, List.of(
                    new ConnectorDefinition("c", "c", SourceCategory.DOCUMENT, SourceType.FILE_UPLOAD, true, false, Map.of()),
                    new ConnectorDefinition("c", "d", SourceCategory.SYSTEM, SourceType.REST_API, false, true, Map.of())
            ), null);
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "Duplicate id: c");
        }

        @Test
        void rejectsDuplicatePolicyIds() {
            var pack = new KanonConfigurationPack("p", "p", null, null, null, null, null, null, List.of(
                    new PolicyTemplate("pol", "pol", Set.of(), Set.of()),
                    new PolicyTemplate("pol", "pol2", Set.of(), Set.of())
            ));
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "Duplicate id: pol");
        }

        @Test
        void rejectsDuplicateModelRoutingPolicyIds() {
            var pack = new KanonConfigurationPack("p", "p", null, null, null, null, List.of(
                    new ModelRoutingPolicy("mrp", "mrp", Map.of()),
                    new ModelRoutingPolicy("mrp", "mrp2", Map.of())
            ), null, null);
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "Duplicate id: mrp");
        }

        @Test
        void warnsOnCrossTypeDuplicateId() {
            var pack = new KanonConfigurationPack("p", "p", List.of(
                    new DomainConfiguration("shared-id", "d", DomainType.ACCOUNTING, List.of(), List.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of())
            ), null, List.of(
                    new WorkflowTemplate("shared-id", "w", DomainType.ACCOUNTING, AiTaskType.EXTRACTION, List.of(), "default", null)
            ), null, null, null, null);
            var result = validator.validate(pack);
            assertTrue(result.issues().stream().anyMatch(i -> i.severity() == ConfigValidationSeverity.WARNING && i.message().contains("shared-id")));
        }

        @Test
        void validatesConnectorSourceCategoryNotNull() {
            var pack = new KanonConfigurationPack("p", "p", null, null, null, null, null, List.of(
                    new ConnectorDefinition("c", "c", null, SourceType.FILE_UPLOAD, true, false, Map.of())
            ), null);
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "sourceCategory");
        }

        @Test
        void validatesConnectorSourceTypeNotNull() {
            var pack = new KanonConfigurationPack("p", "p", null, null, null, null, null, List.of(
                    new ConnectorDefinition("c", "c", SourceCategory.DOCUMENT, null, true, false, Map.of())
            ), null);
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "sourceType");
        }

        @Test
        void validatesWorkflowDomainTypeNotNull() {
            var pack = new KanonConfigurationPack("p", "p", null, null, List.of(
                    new WorkflowTemplate("w", "w", null, AiTaskType.EXTRACTION, List.of(), "default", null)
            ), null, null, null, null);
            assertFalse(validator.validate(pack).valid());
        }

        @Test
        void validatesWorkflowTaskTypeNotNull() {
            var pack = new KanonConfigurationPack("p", "p", null, null, List.of(
                    new WorkflowTemplate("w", "w", DomainType.ACCOUNTING, null, List.of(), "default", null)
            ), null, null, null, null);
            assertFalse(validator.validate(pack).valid());
        }

        @Test
        void validatesNullAgentType() {
            var pack = new KanonConfigurationPack("p", "p", null, null, null, List.of(
                    new AgentDefinition("a", "a", null, Set.of(), Set.of(), Set.of(), Set.of(), null, null, null)
            ), null, null, null);
            assertFalse(validator.validate(pack).valid());
        }

        @Test
        void detectsBrokenDomainWorkflowReference() {
            var pack = new KanonConfigurationPack("p", "p", List.of(
                    new DomainConfiguration("d", "d", DomainType.ACCOUNTING, List.of(), List.of(), Set.of(), Set.of(), Set.of("nonexistent-workflow"), Set.of(), Set.of())
            ), null, null, null, null, null, null);
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "nonexistent-workflow");
        }

        @Test
        void detectsBrokenTenantConnectorReference() {
            var pack = new KanonConfigurationPack("p", "p", null, List.of(
                    new TenantConfiguration("t", "t", DomainType.ACCOUNTING, "DE", null, DataResidency.EU, true, false, Set.of("missing-connector"), Set.of(), null, Map.of())
            ), null, null, null, null, null);
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "missing-connector");
        }

        @Test
        void referencesAcrossConfigTypesAreValidated() {
            var pack = new KanonConfigurationPack("p", "p", List.of(
                    new DomainConfiguration("d", "d", DomainType.ACCOUNTING, List.of(), List.of(), Set.of(), Set.of("missing-agent"), Set.of(), Set.of(), Set.of())
            ), null, null, List.of(
                    new AgentDefinition("existing-agent", "ea", "extraction", Set.of(), Set.of(), Set.of(), Set.of(), null, null, "existing-routing")
            ), List.of(
                    new ModelRoutingPolicy("existing-routing", "er", Map.of())
            ), null, null);
            var result = validator.validate(pack);
            assertFalse(result.valid());
            assertContainsError(result, "missing-agent");
        }
    }

    @Nested
    class RegistryTests {
        private InMemoryConfigurationRegistry registry;

        @BeforeEach
        void setUp() {
            registry = new InMemoryConfigurationRegistry();
        }

        @Test
        void registersAndRetrievesPack() {
            var pack = validPack();
            registry.register(pack);
            assertTrue(registry.pack("test-pack").isPresent());
            assertEquals("Test Pack", registry.pack("test-pack").get().displayName());
        }

        @Test
        void returnsEmptyForUnknownPack() {
            assertTrue(registry.pack("unknown").isEmpty());
        }

        @Test
        void registersAndRetrievesDomain() {
            registry.register(validPack());
            assertTrue(registry.domain("accounting").isPresent());
        }

        @Test
        void returnsEmptyForUnknownDomain() {
            assertTrue(registry.domain("unknown").isEmpty());
        }

        @Test
        void registersAllComponentTypes() {
            var pack = new KanonConfigurationPack(
                    "full", "Full Pack",
                    List.of(new DomainConfiguration("d1", "D1", DomainType.ACCOUNTING, List.of(), List.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of())),
                    List.of(new TenantConfiguration("t1", "T1", DomainType.HR, "DE", null, DataResidency.EU, true, false, Set.of(), Set.of(), null, Map.of())),
                    List.of(new WorkflowTemplate("w1", "W1", DomainType.ACCOUNTING, AiTaskType.EXTRACTION, List.of(), "default", null)),
                    List.of(new AgentDefinition("a1", "A1", "extraction", Set.of(), Set.of(), Set.of(), Set.of(), null, null, null)),
                    List.of(new ModelRoutingPolicy("m1", "M1", Map.of())),
                    List.of(new ConnectorDefinition("c1", "C1", SourceCategory.DOCUMENT, SourceType.FILE_UPLOAD, true, false, Map.of())),
                    List.of(new PolicyTemplate("p1", "P1", Set.of(), Set.of()))
            );
            registry.register(pack);

            assertTrue(registry.pack("full").isPresent());
            assertTrue(registry.domain("d1").isPresent());
            assertTrue(registry.tenant("t1").isPresent());
            assertTrue(registry.workflow("w1").isPresent());
            assertTrue(registry.agent("a1").isPresent());
            assertTrue(registry.modelRoutingPolicy("m1").isPresent());
            assertTrue(registry.connector("c1").isPresent());
            assertTrue(registry.policy("p1").isPresent());

            assertEquals(1, registry.packs().size());
            assertEquals(1, registry.domains().size());
            assertEquals(1, registry.tenants().size());
            assertEquals(1, registry.workflows().size());
            assertEquals(1, registry.agents().size());
            assertEquals(1, registry.modelRoutingPolicies().size());
            assertEquals(1, registry.connectors().size());
            assertEquals(1, registry.policies().size());
        }

        @Test
        void overwritesExistingPackOnReRegister() {
            registry.register(validPack());
            var updated = new KanonConfigurationPack(
                    "test-pack", "Updated Pack",
                    List.of(new DomainConfiguration("new-domain", "New", DomainType.HR, List.of(), List.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of())),
                    null, null, null, null, null, null
            );
            registry.register(updated);

            assertEquals("Updated Pack", registry.pack("test-pack").get().displayName());
            assertTrue(registry.domain("new-domain").isPresent());
            assertTrue(registry.domain("accounting").isPresent());
        }

        @Test
        void handlesNullListsGracefully() {
            var pack = new KanonConfigurationPack("minimal", "Minimal", null, null, null, null, null, null, null);
            registry.register(pack);
            assertTrue(registry.pack("minimal").isPresent());
            assertTrue(registry.domains().isEmpty());
            assertTrue(registry.tenants().isEmpty());
        }
    }

    @Nested
    class SeedImporterTests {
        private InMemoryActiveConfigurationVersionRepository repository;
        private InMemoryConfigurationRegistry registry;
        private DefaultConfigurationSeedImporter importer;

        @BeforeEach
        void setUp() {
            repository = new InMemoryActiveConfigurationVersionRepository();
            registry = new InMemoryConfigurationRegistry();
            importer = new DefaultConfigurationSeedImporter(repository, validator, registry);
        }

        @Test
        void importsValidConfigPack() {
            var pack = validPack();
            var versions = importer.importPack("accounting-demo", pack, "admin", "seed");

            assertFalse(versions.isEmpty());
            assertTrue(registry.domain("accounting").isPresent());
            assertEquals(1, versions.size());
            assertEquals("admin", versions.getFirst().activatedBy());
        }

        @Test
        void rejectsInvalidPack() {
            var invalid = new KanonConfigurationPack(null, "name", null, null, null, null, null, null, null);
            assertThrows(ConfigurationTemplateLoadException.class,
                    () -> importer.importPack("tenant", invalid, "admin", "test"));
            assertTrue(registry.packs().isEmpty());
        }

        @Test
        void createsActiveConfigurationVersionsForAllSections() {
            var pack = new KanonConfigurationPack(
                    "full", "Full Pack",
                    List.of(new DomainConfiguration("d1", "D1", DomainType.ACCOUNTING, List.of(), List.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of())),
                    List.of(new TenantConfiguration("t1", "T1", DomainType.HR, "DE", null, DataResidency.EU, true, false, Set.of(), Set.of(), null, Map.of())),
                    List.of(new WorkflowTemplate("w1", "W1", DomainType.ACCOUNTING, AiTaskType.EXTRACTION, List.of(), "default", null)),
                    List.of(new AgentDefinition("a1", "A1", "extraction", Set.of(), Set.of(), Set.of(), Set.of(), null, null, null)),
                    List.of(new ModelRoutingPolicy("m1", "M1", Map.of())),
                    List.of(new ConnectorDefinition("c1", "C1", SourceCategory.DOCUMENT, SourceType.FILE_UPLOAD, true, false, Map.of())),
                    List.of(new PolicyTemplate("p1", "P1", Set.of(), Set.of()))
            );
            var versions = importer.importPack("tenant1", pack, "admin", "seed");

            assertEquals(7, versions.size());
            assertEquals(1, versions.stream().filter(v -> v.configurationType() == ConfigurationType.DOMAIN).count());
            assertEquals(1, versions.stream().filter(v -> v.configurationType() == ConfigurationType.TENANT).count());
            assertEquals(1, versions.stream().filter(v -> v.configurationType() == ConfigurationType.WORKFLOW_TEMPLATE).count());
            assertEquals(1, versions.stream().filter(v -> v.configurationType() == ConfigurationType.AGENT_DEFINITION).count());
            assertEquals(1, versions.stream().filter(v -> v.configurationType() == ConfigurationType.MODEL_ROUTING_POLICY).count());
            assertEquals(1, versions.stream().filter(v -> v.configurationType() == ConfigurationType.CONNECTOR_DEFINITION).count());
            assertEquals(1, versions.stream().filter(v -> v.configurationType() == ConfigurationType.POLICY_TEMPLATE).count());
        }

        @Test
        void allImportedVersionsAreActive() {
            var pack = validPack();
            var versions = importer.importPack("tenant", pack, "admin", "seed");
            assertTrue(versions.stream().allMatch(v -> v.activationState() == ConfigurationActivationState.ACTIVE));
        }
    }

    @Nested
    class ActiveConfigVersionRepositoryTests {
        private InMemoryActiveConfigurationVersionRepository repository;

        @BeforeEach
        void setUp() {
            repository = new InMemoryActiveConfigurationVersionRepository();
        }

        private ActiveConfigurationVersion version(String configId, String tenantId, ConfigurationType type, ConfigurationActivationState state) {
            var now = Instant.now();
            return new ActiveConfigurationVersion(
                    configId, tenantId, type, "template", 1, state,
                    "admin", now, null, null, "test",
                    new AuditMetadata(now, "admin", now, "admin", 1)
            );
        }

        @Test
        void findsActiveVersion() {
            var v = version("d1", "t1", ConfigurationType.DOMAIN, ConfigurationActivationState.ACTIVE);
            repository.save(v);
            var found = repository.findActive("t1", ConfigurationType.DOMAIN, "d1");
            assertTrue(found.isPresent());
            assertEquals("d1", found.get().configurationId());
        }

        @Test
        void doesNotFindInactiveVersion() {
            var v = version("d1", "t1", ConfigurationType.DOMAIN, ConfigurationActivationState.INACTIVE);
            repository.save(v);
            assertTrue(repository.findActive("t1", ConfigurationType.DOMAIN, "d1").isEmpty());
        }

        @Test
        void doesNotFindArchivedVersion() {
            var v = version("d1", "t1", ConfigurationType.DOMAIN, ConfigurationActivationState.ARCHIVED);
            repository.save(v);
            assertTrue(repository.findActive("t1", ConfigurationType.DOMAIN, "d1").isEmpty());
        }

        @Test
        void doesNotFindDraftVersion() {
            var v = version("d1", "t1", ConfigurationType.DOMAIN, ConfigurationActivationState.DRAFT);
            repository.save(v);
            assertTrue(repository.findActive("t1", ConfigurationType.DOMAIN, "d1").isEmpty());
        }

        @Test
        void findsActiveByTenant() {
            repository.save(version("d1", "t1", ConfigurationType.DOMAIN, ConfigurationActivationState.ACTIVE));
            repository.save(version("d2", "t1", ConfigurationType.DOMAIN, ConfigurationActivationState.ACTIVE));
            repository.save(version("d3", "t2", ConfigurationType.DOMAIN, ConfigurationActivationState.ACTIVE));

            assertEquals(2, repository.findActiveByTenant("t1").size());
            assertEquals(1, repository.findActiveByTenant("t2").size());
        }

        @Test
        void tenantQueryExcludesInactive() {
            repository.save(version("d1", "t1", ConfigurationType.DOMAIN, ConfigurationActivationState.ACTIVE));
            repository.save(version("d2", "t1", ConfigurationType.DOMAIN, ConfigurationActivationState.INACTIVE));
            assertEquals(1, repository.findActiveByTenant("t1").size());
        }

        @Test
        void findAllActiveReturnsOnlyActive() {
            repository.save(version("d1", "t1", ConfigurationType.DOMAIN, ConfigurationActivationState.ACTIVE));
            repository.save(version("d2", "t2", ConfigurationType.DOMAIN, ConfigurationActivationState.INACTIVE));
            repository.save(version("d3", "t1", ConfigurationType.DOMAIN, ConfigurationActivationState.DRAFT));

            assertEquals(1, repository.findAllActive().size());
        }

        @Test
        void saveOverwritesExistingKey() {
            var v1 = version("d1", "t1", ConfigurationType.DOMAIN, ConfigurationActivationState.ACTIVE);
            repository.save(v1);
            var v2 = version("d1", "t1", ConfigurationType.DOMAIN, ConfigurationActivationState.INACTIVE);
            repository.save(v2);

            assertTrue(repository.findActive("t1", ConfigurationType.DOMAIN, "d1").isEmpty());
            assertTrue(repository.findAllActive().isEmpty());
        }

        @Test
        void returnsEmptyForUnknownTenant() {
            assertTrue(repository.findActiveByTenant("nonexistent").isEmpty());
        }
    }

    private static void assertContainsError(ConfigValidationResult result, String substring) {
        assertTrue(
                result.issues().stream()
                        .anyMatch(i -> i.severity() == ConfigValidationSeverity.ERROR
                                && (i.path().contains(substring) || i.message().contains(substring))),
                "Expected error containing: " + substring + " in issues: " + result.issues()
        );
    }
}
