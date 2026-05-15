package ai.datalithix.kanon.bootstrap;

import ai.datalithix.kanon.config.model.ConfigValidationIssue;
import ai.datalithix.kanon.config.model.ConfigValidationSeverity;
import ai.datalithix.kanon.config.model.KanonConfigurationPack;
import ai.datalithix.kanon.config.service.ConfigurationSeedImporter;
import ai.datalithix.kanon.config.service.ConfigurationTemplateLoader;
import ai.datalithix.kanon.config.service.ConfigurationValidator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class BootstrapConfigurationPackLoader implements ApplicationRunner, Ordered {
    private static final Logger log = LoggerFactory.getLogger(BootstrapConfigurationPackLoader.class);
    private static final String SYSTEM_ACTOR = "system";
    private static final String BOOTSTRAP_REASON = "Bootstrap seed import";

    private final ConfigurationTemplateLoader templateLoader;
    private final ConfigurationValidator validator;
    private final ConfigurationSeedImporter seedImporter;

    public BootstrapConfigurationPackLoader(
            ConfigurationTemplateLoader templateLoader,
            ConfigurationValidator validator,
            ConfigurationSeedImporter seedImporter
    ) {
        this.templateLoader = templateLoader;
        this.validator = validator;
        this.seedImporter = seedImporter;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (SeedPack seedPack : seedPacks()) {
            KanonConfigurationPack pack = templateLoader.load(seedPack.location());
            validate(seedPack, pack);
            var activeVersions = seedImporter.importPack(seedPack.tenantId(), pack, SYSTEM_ACTOR, BOOTSTRAP_REASON);
            log.info(
                    "Loaded configuration pack {} for tenant {} with {} active configurations",
                    pack.id(),
                    seedPack.tenantId(),
                    activeVersions.size()
            );
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private void validate(SeedPack seedPack, KanonConfigurationPack pack) {
        var result = validator.validate(pack);
        if (result.valid()) {
            result.issues().stream()
                    .filter(issue -> issue.severity() == ConfigValidationSeverity.WARNING)
                    .forEach(issue -> log.warn(
                            "Configuration pack {} warning at {}: {}",
                            pack.id(),
                            issue.path(),
                            issue.message()
                    ));
            return;
        }

        String issues = result.issues().stream()
                .filter(issue -> issue.severity() == ConfigValidationSeverity.ERROR)
                .map(this::formatIssue)
                .reduce((left, right) -> left + "; " + right)
                .orElse("unknown validation error");
        throw new IllegalStateException(
                "Cannot bootstrap configuration pack from " + seedPack.location() + ": " + issues
        );
    }

    private String formatIssue(ConfigValidationIssue issue) {
        return issue.path() + " " + issue.message();
    }

    private List<SeedPack> seedPacks() {
        return List.of(
                new SeedPack("accounting-demo", "classpath:config/templates/accounting-pack.yml"),
                new SeedPack("hr-demo", "classpath:config/templates/hr-pack.yml")
        );
    }

    private record SeedPack(String tenantId, String location) {}
}
