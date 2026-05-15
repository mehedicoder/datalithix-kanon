package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.model.GovernanceStatus;
import ai.datalithix.kanon.tenant.model.Membership;
import ai.datalithix.kanon.tenant.model.MembershipScope;
import ai.datalithix.kanon.tenant.model.Organization;
import ai.datalithix.kanon.tenant.model.Role;
import ai.datalithix.kanon.tenant.model.Tenant;
import ai.datalithix.kanon.tenant.model.UserAccount;
import ai.datalithix.kanon.tenant.model.Workspace;
import ai.datalithix.kanon.tenant.model.WorkspaceType;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.tenant.service.GovernanceAdministrationService;
import com.vaadin.flow.component.UI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantAdminViewTest {
    @BeforeEach
    void setUp() {
        UI.setCurrent(new UI());
        UI.getCurrent().setLocale(java.util.Locale.ENGLISH);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void hidesCreateActionWhenUserCannotCreateTenants() {
        TenantAdminView view = new TenantAdminView(new FakeGovernanceAdministrationService(), contextService(Set.of("tenant.update")));

        assertTrue(view.createToolbar().getComponentCount() == 1);
    }

    @Test
    void showsCreateActionWhenUserCanCreateTenants() {
        TenantAdminView view = new TenantAdminView(new FakeGovernanceAdministrationService(), contextService(Set.of("platform.tenant.create")));

        assertTrue(view.createToolbar().getComponentCount() == 2);
    }

    private static CurrentUserContextService contextService(Set<String> permissions) {
        return () -> new CurrentUserContext(
                "user-1",
                "operator",
                "tenant-1",
                "org-1",
                "workspace-1",
                Set.of(),
                permissions,
                List.of()
        );
    }

    private static AuditMetadata audit() {
        Instant now = Instant.parse("2026-04-18T00:00:00Z");
        return new AuditMetadata(now, "test", now, "test", 1);
    }

    private static class FakeGovernanceAdministrationService implements GovernanceAdministrationService {
        @Override
        public List<Tenant> tenants() {
            return List.of(new Tenant("tenant-1", "tenant-1", "Tenant One", GovernanceStatus.ACTIVE, "DEFAULT", "en", audit()));
        }

        @Override
        public List<Organization> organizations(String tenantId) {
            return List.of();
        }

        @Override
        public List<Workspace> workspaces(String tenantId, String organizationId) {
            return List.of();
        }

        @Override
        public List<UserAccount> users() {
            return List.of();
        }

        @Override
        public List<Membership> memberships() {
            return List.of();
        }

        @Override
        public List<Role> roles() {
            return List.of();
        }

        @Override
        public Tenant createTenant(String tenantKey, String name, String dataResidency, String defaultLocale) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Organization createOrganization(String tenantId, String organizationKey, String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Workspace createWorkspace(String tenantId, String organizationId, String workspaceKey, String name, WorkspaceType workspaceType, DomainType domainType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserAccount createUser(String username, String email, String displayName, String rawPassword) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Membership assignMembership(String userId, MembershipScope scope, String tenantId, String organizationId, String workspaceId, Set<String> roleKeys) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Tenant updateTenant(String tenantId, String tenantKey, String name, String dataResidency, String defaultLocale) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Tenant archiveTenant(String tenantId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Tenant restoreTenant(String tenantId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Organization updateOrganization(String tenantId, String organizationId, String organizationKey, String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Organization archiveOrganization(String tenantId, String organizationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Organization restoreOrganization(String tenantId, String organizationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Workspace updateWorkspace(String tenantId, String organizationId, String workspaceId, String workspaceKey, String name, WorkspaceType workspaceType, DomainType domainType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Workspace archiveWorkspace(String tenantId, String organizationId, String workspaceId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Workspace restoreWorkspace(String tenantId, String organizationId, String workspaceId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserAccount updateUser(String userId, String username, String email, String displayName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserAccount archiveUser(String userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserAccount restoreUser(String userId) {
            throw new UnsupportedOperationException();
        }
    }
}
