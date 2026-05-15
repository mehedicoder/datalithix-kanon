package ai.datalithix.kanon.ui.layout;

import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.model.GovernanceStatus;
import ai.datalithix.kanon.tenant.model.UserAccount;
import ai.datalithix.kanon.tenant.model.Workspace;
import ai.datalithix.kanon.tenant.model.WorkspaceType;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.tenant.service.UserProfileService;
import ai.datalithix.kanon.tenant.service.WorkspaceRepository;
import ai.datalithix.kanon.ui.i18n.LocalizationService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.sidenav.SideNavItem;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainLayoutTest {
    private FakeUserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        UI.setCurrent(new UI());
        userProfileService = new FakeUserProfileService("de");
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void doesNotRenderViewLevelLanguageSelector() {
        MainLayout layout = layout();

        List<ComboBox> comboBoxes = descendants(layout.createHeader(), ComboBox.class).toList();

        assertFalse(comboBoxes.stream().anyMatch(comboBox -> "app.language".equals(comboBox.getPlaceholder())));
        assertFalse(comboBoxes.stream().anyMatch(comboBox -> "Language".equals(comboBox.getPlaceholder())));
        assertFalse(comboBoxes.stream().anyMatch(comboBox -> "Sprache".equals(comboBox.getPlaceholder())));
        assertEquals(0, userProfileService.updateCount);
    }

    @Test
    void appliesEffectiveLocaleFromTenantOrProfileService() {
        layout();

        assertEquals(Locale.GERMAN.getLanguage(), UI.getCurrent().getLocale().getLanguage());
    }

    @Test
    void exposesSignOutMenuItemAndUsesSpringLogoutEndpoint() {
        MainLayout layout = layout();

        boolean hasSignOut = descendants(layout.createHeader(), MenuBar.class)
                .flatMap(menu -> menu.getItems().stream())
                .anyMatch(item -> item.getSubMenu().getItems().size() == 3);

        assertTrue(hasSignOut);
        assertEquals("logout", layout.logoutLocation());
    }

    @Test
    void hidesAdministrationMenuItemsWithoutPermissions() {
        userProfileService = new FakeUserProfileService("en");
        MainLayout layout = layout(Set.of());

        List<String> paths = layout.administrationItems(context(Set.of())).stream()
                .map(SideNavItem::getPath)
                .toList();

        assertFalse(paths.contains("admin/tenants"));
        assertFalse(paths.contains("admin/security-audit"));
    }

    @Test
    void showsOnlyAuthorizedAdministrationMenuItems() {
        userProfileService = new FakeUserProfileService("en");
        MainLayout layout = layout(Set.of("tenant.update", "tenant.audit.read"));

        List<String> paths = layout.administrationItems(context(Set.of("tenant.update", "tenant.audit.read"))).stream()
                .map(SideNavItem::getPath)
                .toList();

        assertTrue(paths.contains("admin/tenants"));
        assertTrue(paths.contains("admin/security-audit"));
        assertFalse(paths.contains("admin/users"));
    }

    @Test
    void showsAnnotationNodesMenuForTenantConfigReaders() {
        userProfileService = new FakeUserProfileService("en");
        MainLayout layout = layout(Set.of("tenant.config.read"));

        List<String> paths = layout.administrationItems(context(Set.of("tenant.config.read"))).stream()
                .map(SideNavItem::getPath)
                .toList();

        assertTrue(paths.contains("admin/annotation-nodes"));
    }

    private MainLayout layout() {
        return layout(Set.of());
    }

    private MainLayout layout(Set<String> permissions) {
        return new MainLayout(
                new FakeCurrentUserContextService(permissions),
                new FakeWorkspaceRepository(),
                new LocalizationService(),
                userProfileService
        );
    }

    private static <T extends Component> Stream<T> descendants(Component root, Class<T> type) {
        Stream<T> self = type.isInstance(root) ? Stream.of(type.cast(root)) : Stream.empty();
        return Stream.concat(self, root.getChildren().flatMap(child -> descendants(child, type)));
    }

    private static AuditMetadata audit() {
        Instant now = Instant.parse("2026-04-17T00:00:00Z");
        return new AuditMetadata(now, "test", now, "test", 1);
    }

    private static CurrentUserContext context(Set<String> permissions) {
        return new CurrentUserContext(
                "user-1",
                "superadmin",
                "tenant-1",
                "org-1",
                "workspace-1",
                Set.of("PLATFORM_SUPER_ADMIN"),
                permissions,
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
        private final Set<String> permissions;

        private FakeCurrentUserContextService(Set<String> permissions) {
            this.permissions = permissions;
        }

        @Override
        public CurrentUserContext currentUser() {
            return context(permissions);
        }
    }

    private static class FakeWorkspaceRepository implements WorkspaceRepository {
        @Override
        public Workspace save(Workspace workspace) {
            return workspace;
        }

        @Override
        public Optional<Workspace> findById(String tenantId, String organizationId, String workspaceId) {
            return Optional.of(workspace());
        }

        @Override
        public List<Workspace> findByOrganizationId(String tenantId, String organizationId) {
            return List.of(workspace());
        }

        private Workspace workspace() {
            return new Workspace(
                    "workspace-1",
                    "tenant-1",
                    "org-1",
                    "administration",
                    "Administration",
                    WorkspaceType.ADMINISTRATION,
                    DomainType.CUSTOM,
                    GovernanceStatus.ACTIVE,
                    audit()
            );
        }
    }

    private static class FakeUserProfileService implements UserProfileService {
        private String effectiveLocale;
        private int updateCount;

        private FakeUserProfileService(String effectiveLocale) {
            this.effectiveLocale = effectiveLocale;
        }

        @Override
        public UserAccount currentUser() {
            return user(effectiveLocale);
        }

        @Override
        public String effectiveLocale() {
            return effectiveLocale;
        }

        @Override
        public UserAccount updatePreferredLocale(String preferredLocale) {
            updateCount++;
            effectiveLocale = preferredLocale;
            return currentUser();
        }
    }
}
