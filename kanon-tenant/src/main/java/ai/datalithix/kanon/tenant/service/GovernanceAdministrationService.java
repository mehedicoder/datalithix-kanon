package ai.datalithix.kanon.tenant.service;

import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.tenant.model.Membership;
import ai.datalithix.kanon.tenant.model.MembershipScope;
import ai.datalithix.kanon.tenant.model.Organization;
import ai.datalithix.kanon.tenant.model.Role;
import ai.datalithix.kanon.tenant.model.Tenant;
import ai.datalithix.kanon.tenant.model.UserAccount;
import ai.datalithix.kanon.tenant.model.Workspace;
import ai.datalithix.kanon.tenant.model.WorkspaceType;
import java.util.List;
import java.util.Set;

public interface GovernanceAdministrationService {
    List<Tenant> tenants();
    List<Organization> organizations(String tenantId);
    List<Workspace> workspaces(String tenantId, String organizationId);
    List<UserAccount> users();
    List<Membership> memberships();
    List<Role> roles();

    Tenant createTenant(String tenantKey, String name, String dataResidency, String defaultLocale);
    Organization createOrganization(String tenantId, String organizationKey, String name);
    Workspace createWorkspace(String tenantId, String organizationId, String workspaceKey, String name, WorkspaceType workspaceType, DomainType domainType);
    UserAccount createUser(String username, String email, String displayName, String rawPassword);
    Membership assignMembership(String userId, MembershipScope scope, String tenantId, String organizationId, String workspaceId, Set<String> roleKeys);

    Tenant updateTenant(String tenantId, String tenantKey, String name, String dataResidency, String defaultLocale);
    Tenant archiveTenant(String tenantId);
    Tenant restoreTenant(String tenantId);
    Organization updateOrganization(String tenantId, String organizationId, String organizationKey, String name);
    Organization archiveOrganization(String tenantId, String organizationId);
    Organization restoreOrganization(String tenantId, String organizationId);
    Workspace updateWorkspace(String tenantId, String organizationId, String workspaceId, String workspaceKey, String name, WorkspaceType workspaceType, DomainType domainType);
    Workspace archiveWorkspace(String tenantId, String organizationId, String workspaceId);
    Workspace restoreWorkspace(String tenantId, String organizationId, String workspaceId);
    UserAccount updateUser(String userId, String username, String email, String displayName);
    UserAccount archiveUser(String userId);
    UserAccount restoreUser(String userId);
}
