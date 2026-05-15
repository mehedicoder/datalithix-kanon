package ai.datalithix.kanon.bootstrap;

import ai.datalithix.kanon.config.model.ActiveConfigurationVersion;
import ai.datalithix.kanon.config.model.ConfigValidationResult;
import ai.datalithix.kanon.config.model.KanonConfigurationPack;
import ai.datalithix.kanon.config.service.ConfigurationSeedImporter;
import ai.datalithix.kanon.config.service.ConfigurationTemplateLoader;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BootstrapConfigurationPackLoaderTest {
    @Test
    void loadsAndImportsDefaultConfigurationPacks() throws Exception {
        FakeTemplateLoader templateLoader = new FakeTemplateLoader();
        FakeSeedImporter seedImporter = new FakeSeedImporter();
        BootstrapConfigurationPackLoader loader = new BootstrapConfigurationPackLoader(
                templateLoader,
                pack -> ConfigValidationResult.ok(),
                seedImporter
        );

        loader.run(new DefaultApplicationArguments());

        assertEquals(List.of(
                "classpath:config/templates/accounting-pack.yml",
                "classpath:config/templates/hr-pack.yml"
        ), templateLoader.locations);
        assertEquals(List.of(
                new ImportedPack("accounting-demo", "accounting-pack"),
                new ImportedPack("hr-demo", "hr-pack")
        ), seedImporter.importedPacks);
    }

    private static class FakeTemplateLoader implements ConfigurationTemplateLoader {
        private final List<String> locations = new ArrayList<>();

        @Override
        public KanonConfigurationPack load(String location) {
            locations.add(location);
            if (location.contains("accounting")) {
                return pack("accounting-pack");
            }
            return pack("hr-pack");
        }

        private KanonConfigurationPack pack(String id) {
            return new KanonConfigurationPack(id, id, null, null, null, null, null, null, null);
        }
    }

    private static class FakeSeedImporter implements ConfigurationSeedImporter {
        private final List<ImportedPack> importedPacks = new ArrayList<>();

        @Override
        public List<ActiveConfigurationVersion> importPack(
                String tenantId,
                KanonConfigurationPack pack,
                String actorId,
                String reason
        ) {
            importedPacks.add(new ImportedPack(tenantId, pack.id()));
            return List.of();
        }
    }

    private record ImportedPack(String tenantId, String packId) {}
}
