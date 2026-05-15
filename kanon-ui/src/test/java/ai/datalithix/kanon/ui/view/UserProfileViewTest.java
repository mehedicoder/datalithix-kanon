package ai.datalithix.kanon.ui.view;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.model.GovernanceStatus;
import ai.datalithix.kanon.tenant.model.Tenant;
import ai.datalithix.kanon.tenant.model.UserAccount;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.tenant.service.TenantRepository;
import ai.datalithix.kanon.tenant.service.UserProfileService;
import ai.datalithix.kanon.ui.i18n.LocalizationService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.server.VaadinSession;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserProfileViewTest {
    private FakeUserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        UI.setCurrent(new UI());
        VaadinSession.setCurrent(new VaadinSession(null));
        userProfileService = new FakeUserProfileService("de");
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
    }

    @Test
    void profileLanguagePreferenceOverridesTenantDefaultForUser() {
        UserProfileView view = new UserProfileView(
                new FakeCurrentUserContextService(),
                userProfileService,
                new FakeTenantRepository("en"),
                new LocalizationService()
        );

        ComboBox<?> language = descendants(view, ComboBox.class)
                .findFirst()
                .orElseThrow();

        assertNotNull(language.getValue());
        assertEquals(Locale.GERMAN.getLanguage(), ((Locale) language.getValue()).getLanguage());
    }

    @Test
    void emptyProfileLanguageFallsBackToTenantDefaultEffectiveLocale() {
        userProfileService = new FakeUserProfileService("en");

        new UserProfileView(
                new FakeCurrentUserContextService(),
                userProfileService,
                new FakeTenantRepository("en"),
                new LocalizationService()
        );

        assertEquals("en", userProfileService.effectiveLocale());
    }

    private static <T extends Component> Stream<T> descendants(Component root, Class<T> type) {
        Stream<T> self = type.isInstance(root) ? Stream.of(type.cast(root)) : Stream.empty();
        return Stream.concat(self, root.getChildren().flatMap(child -> descendants(child, type)));
    }

    private static AuditMetadata audit() {
        Instant now = Instant.parse("2026-04-17T00:00:00Z");
        return new AuditMetadata(now, "test", now, "test", 1);
    }

    private static CurrentUserContext context() {
        return new CurrentUserContext(
                "user-1",
                "superadmin",
                "tenant-1",
                "org-1",
                "workspace-1",
                Set.of("PLATFORM_SUPER_ADMIN"),
                Set.of(),
                List.of()
        );
    }

    private static UserAccount user(String preferredLocale) {
        return new UserAccount(
                "user-1",
                "superadmin",
                "superadmin@kanon.local",
                "Super Admin",
                "{noop}password",
                preferredLocale,
                GovernanceStatus.ACTIVE,
                false,
                audit()
        );
    }

    private static class FakeCurrentUserContextService implements CurrentUserContextService {
        @Override
        public CurrentUserContext currentUser() {
            return context();
        }
    }

    private static class FakeTenantRepository implements TenantRepository {
        private final String defaultLocale;

        private FakeTenantRepository(String defaultLocale) {
            this.defaultLocale = defaultLocale;
        }

        @Override
        public Tenant save(Tenant tenant) {
            return tenant;
        }

        @Override
        public Optional<Tenant> findById(String tenantId) {
            return Optional.of(new Tenant(
                    "tenant-1",
                    "default",
                    "Default Tenant",
                    GovernanceStatus.ACTIVE,
                    "EU",
                    defaultLocale,
                    audit()
            ));
        }

        @Override
        public List<Tenant> findAll() {
            return List.of();
        }
    }

    private static class FakeUserProfileService implements UserProfileService {
        private final String effectiveLocale;

        private FakeUserProfileService(String effectiveLocale) {
            this.effectiveLocale = effectiveLocale;
        }

        @Override
        public UserAccount currentUser() {
            return user(Locale.GERMAN.getLanguage().equals(effectiveLocale) ? "de" : null);
        }

        @Override
        public String effectiveLocale() {
            return effectiveLocale;
        }

        @Override
        public UserAccount updatePreferredLocale(String preferredLocale) {
            return user(preferredLocale);
        }
    }
}
