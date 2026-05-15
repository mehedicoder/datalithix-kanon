package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.tenant.model.GovernanceStatus;
import ai.datalithix.kanon.tenant.model.Membership;
import ai.datalithix.kanon.tenant.model.MembershipScope;
import ai.datalithix.kanon.tenant.model.Organization;
import ai.datalithix.kanon.tenant.model.Role;
import ai.datalithix.kanon.tenant.model.RoleScope;
import ai.datalithix.kanon.tenant.model.Tenant;
import ai.datalithix.kanon.tenant.model.UserAccount;
import ai.datalithix.kanon.tenant.model.Workspace;
import ai.datalithix.kanon.tenant.model.WorkspaceType;
import ai.datalithix.kanon.tenant.service.MembershipRepository;
import ai.datalithix.kanon.tenant.service.OrganizationRepository;
import ai.datalithix.kanon.tenant.service.RoleRepository;
import ai.datalithix.kanon.tenant.service.TenantRepository;
import ai.datalithix.kanon.tenant.service.UserAccountRepository;
import ai.datalithix.kanon.tenant.service.WorkspaceRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.audit;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.instant;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.joinStrings;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.optionalEnum;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.stringSet;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.timestamp;

@Repository
@Profile("!test")
public class PostgresGovernanceRepository implements TenantRepository, OrganizationRepository, WorkspaceRepository,
        UserAccountRepository, RoleRepository, MembershipRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresGovernanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Tenant save(Tenant tenant) {
        jdbcTemplate.update("""
                        INSERT INTO tenant (
                            tenant_id, tenant_key, name, status, data_residency, default_locale,
                            created_at, created_by, updated_at, updated_by, audit_version
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (tenant_id)
                        DO UPDATE SET
                            tenant_key = EXCLUDED.tenant_key,
                            name = EXCLUDED.name,
                            status = EXCLUDED.status,
                            data_residency = EXCLUDED.data_residency,
                            default_locale = EXCLUDED.default_locale,
                            updated_at = EXCLUDED.updated_at,
                            updated_by = EXCLUDED.updated_by,
                            audit_version = tenant.audit_version + 1
                        """,
                tenant.tenantId(), tenant.tenantKey(), tenant.name(), tenant.status().name(),
                tenant.dataResidency(), tenant.defaultLocale(), timestamp(tenant.audit().createdAt()),
                tenant.audit().createdBy(), timestamp(tenant.audit().updatedAt()), tenant.audit().updatedBy(),
                tenant.audit().version()
        );
        return findById(tenant.tenantId()).orElse(tenant);
    }

    @Override
    public Optional<Tenant> findById(String tenantId) {
        return jdbcTemplate.query("SELECT * FROM tenant WHERE tenant_id = ?", this::mapTenant, tenantId).stream().findFirst();
    }

    @Override
    public List<Tenant> findAll() {
        return jdbcTemplate.query("SELECT * FROM tenant ORDER BY name ASC", this::mapTenant);
    }

    @Override
    public Organization save(Organization organization) {
        jdbcTemplate.update("""
                        INSERT INTO organization (
                            organization_id, tenant_id, organization_key, name, status,
                            created_at, created_by, updated_at, updated_by, audit_version
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (tenant_id, organization_id)
                        DO UPDATE SET
                            organization_key = EXCLUDED.organization_key,
                            name = EXCLUDED.name,
                            status = EXCLUDED.status,
                            updated_at = EXCLUDED.updated_at,
                            updated_by = EXCLUDED.updated_by,
                            audit_version = organization.audit_version + 1
                        """,
                organization.organizationId(), organization.tenantId(), organization.organizationKey(),
                organization.name(), organization.status().name(), timestamp(organization.audit().createdAt()),
                organization.audit().createdBy(), timestamp(organization.audit().updatedAt()),
                organization.audit().updatedBy(), organization.audit().version()
        );
        return findById(organization.tenantId(), organization.organizationId()).orElse(organization);
    }

    @Override
    public Optional<Organization> findById(String tenantId, String organizationId) {
        return jdbcTemplate.query("""
                        SELECT * FROM organization
                        WHERE tenant_id = ? AND organization_id = ?
                        """,
                this::mapOrganization,
                tenantId,
                organizationId
        ).stream().findFirst();
    }

    @Override
    public List<Organization> findByTenantId(String tenantId) {
        return jdbcTemplate.query("""
                        SELECT * FROM organization
                        WHERE tenant_id = ?
                        ORDER BY name ASC
                        """,
                this::mapOrganization,
                tenantId
        );
    }

    @Override
    public Workspace save(Workspace workspace) {
        jdbcTemplate.update("""
                        INSERT INTO workspace (
                            workspace_id, tenant_id, organization_id, workspace_key, name, workspace_type,
                            domain_type, status, created_at, created_by, updated_at, updated_by, audit_version
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (tenant_id, organization_id, workspace_id)
                        DO UPDATE SET
                            workspace_key = EXCLUDED.workspace_key,
                            name = EXCLUDED.name,
                            workspace_type = EXCLUDED.workspace_type,
                            domain_type = EXCLUDED.domain_type,
                            status = EXCLUDED.status,
                            updated_at = EXCLUDED.updated_at,
                            updated_by = EXCLUDED.updated_by,
                            audit_version = workspace.audit_version + 1
                        """,
                workspace.workspaceId(), workspace.tenantId(), workspace.organizationId(), workspace.workspaceKey(),
                workspace.name(), workspace.workspaceType().name(), name(workspace.domainType()), workspace.status().name(),
                timestamp(workspace.audit().createdAt()), workspace.audit().createdBy(),
                timestamp(workspace.audit().updatedAt()), workspace.audit().updatedBy(), workspace.audit().version()
        );
        return findById(workspace.tenantId(), workspace.organizationId(), workspace.workspaceId()).orElse(workspace);
    }

    @Override
    public Optional<Workspace> findById(String tenantId, String organizationId, String workspaceId) {
        return jdbcTemplate.query("""
                        SELECT * FROM workspace
                        WHERE tenant_id = ? AND organization_id = ? AND workspace_id = ?
                        """,
                this::mapWorkspace,
                tenantId,
                organizationId,
                workspaceId
        ).stream().findFirst();
    }

    @Override
    public List<Workspace> findByOrganizationId(String tenantId, String organizationId) {
        return jdbcTemplate.query("""
                        SELECT * FROM workspace
                        WHERE tenant_id = ? AND organization_id = ?
                        ORDER BY name ASC
                        """,
                this::mapWorkspace,
                tenantId,
                organizationId
        );
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
        jdbcTemplate.update("""
                        INSERT INTO user_account (
                            user_id, username, email, display_name, password_hash, preferred_locale, status, is_system_user,
                            created_at, created_by, updated_at, updated_by, audit_version
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (user_id)
                        DO UPDATE SET
                            username = EXCLUDED.username,
                            email = EXCLUDED.email,
                            display_name = EXCLUDED.display_name,
                            password_hash = EXCLUDED.password_hash,
                            preferred_locale = EXCLUDED.preferred_locale,
                            status = EXCLUDED.status,
                            is_system_user = EXCLUDED.is_system_user,
                            updated_at = EXCLUDED.updated_at,
                            updated_by = EXCLUDED.updated_by,
                            audit_version = user_account.audit_version + 1
                        """,
                userAccount.userId(), userAccount.username(), userAccount.email(), userAccount.displayName(),
                userAccount.passwordHash(), userAccount.preferredLocale(), userAccount.status().name(),
                userAccount.systemUser(), timestamp(userAccount.audit().createdAt()),
                userAccount.audit().createdBy(), timestamp(userAccount.audit().updatedAt()),
                userAccount.audit().updatedBy(), userAccount.audit().version()
        );
        return findByUserId(userAccount.userId()).orElse(userAccount);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return jdbcTemplate.query("SELECT * FROM user_account WHERE username = ?", this::mapUserAccount, username)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<UserAccount> findByUserId(String userId) {
        return jdbcTemplate.query("SELECT * FROM user_account WHERE user_id = ?", this::mapUserAccount, userId)
                .stream()
                .findFirst();
    }

    @Override
    public List<UserAccount> findAllUsers() {
        return jdbcTemplate.query("SELECT * FROM user_account ORDER BY display_name ASC, username ASC", this::mapUserAccount);
    }

    @Override
    public Role save(Role role) {
        jdbcTemplate.update("""
                        INSERT INTO kanon_role (
                            role_id, role_key, name, allowed_scope, system_role, permissions,
                            created_at, created_by, updated_at, updated_by, audit_version
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (role_id)
                        DO UPDATE SET
                            role_key = EXCLUDED.role_key,
                            name = EXCLUDED.name,
                            allowed_scope = EXCLUDED.allowed_scope,
                            system_role = EXCLUDED.system_role,
                            permissions = EXCLUDED.permissions,
                            updated_at = EXCLUDED.updated_at,
                            updated_by = EXCLUDED.updated_by,
                            audit_version = kanon_role.audit_version + 1
                        """,
                role.roleId(), role.roleKey(), role.name(), role.allowedScope().name(), role.systemRole(),
                joinStrings(role.permissions()), timestamp(role.audit().createdAt()), role.audit().createdBy(),
                timestamp(role.audit().updatedAt()), role.audit().updatedBy(), role.audit().version()
        );
        return findByKey(role.roleKey()).orElse(role);
    }

    @Override
    public Optional<Role> findByKey(String roleKey) {
        return jdbcTemplate.query("SELECT * FROM kanon_role WHERE role_key = ?", this::mapRole, roleKey).stream().findFirst();
    }

    @Override
    public List<Role> findByKeys(Collection<String> roleKeys) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return List.of();
        }
        return roleKeys.stream().map(this::findByKey).flatMap(Optional::stream).toList();
    }

    @Override
    public List<Role> findAllRoles() {
        return jdbcTemplate.query("SELECT * FROM kanon_role ORDER BY allowed_scope ASC, role_key ASC", this::mapRole);
    }

    @Override
    public Membership save(Membership membership) {
        jdbcTemplate.update("""
                        INSERT INTO membership (
                            membership_id, user_id, scope, tenant_id, organization_id, workspace_id, status,
                            starts_at, expires_at, created_at, created_by, updated_at, updated_by, audit_version
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (membership_id)
                        DO UPDATE SET
                            user_id = EXCLUDED.user_id,
                            scope = EXCLUDED.scope,
                            tenant_id = EXCLUDED.tenant_id,
                            organization_id = EXCLUDED.organization_id,
                            workspace_id = EXCLUDED.workspace_id,
                            status = EXCLUDED.status,
                            starts_at = EXCLUDED.starts_at,
                            expires_at = EXCLUDED.expires_at,
                            updated_at = EXCLUDED.updated_at,
                            updated_by = EXCLUDED.updated_by,
                            audit_version = membership.audit_version + 1
                        """,
                membership.membershipId(), membership.userId(), membership.scope().name(), membership.tenantId(),
                membership.organizationId(), membership.workspaceId(), membership.status().name(),
                timestamp(membership.startsAt()), timestamp(membership.expiresAt()), timestamp(membership.audit().createdAt()),
                membership.audit().createdBy(), timestamp(membership.audit().updatedAt()),
                membership.audit().updatedBy(), membership.audit().version()
        );
        jdbcTemplate.update("DELETE FROM membership_role WHERE membership_id = ?", membership.membershipId());
        findByKeys(membership.roleKeys()).forEach(role -> jdbcTemplate.update(
                "INSERT INTO membership_role (membership_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                membership.membershipId(),
                role.roleId()
        ));
        return findByMembershipId(membership.membershipId()).orElse(membership);
    }

    @Override
    public Optional<Membership> findByMembershipId(String membershipId) {
        return jdbcTemplate.query("SELECT * FROM membership WHERE membership_id = ?", this::mapMembership, membershipId)
                .stream()
                .findFirst();
    }

    @Override
    public List<Membership> findMembershipsByUserId(String userId) {
        return jdbcTemplate.query("""
                        SELECT * FROM membership
                        WHERE user_id = ? AND status = 'ACTIVE'
                        ORDER BY scope ASC, tenant_id ASC NULLS FIRST, organization_id ASC NULLS FIRST, workspace_id ASC NULLS FIRST
                        """,
                this::mapMembership,
                userId
        );
    }

    @Override
    public List<Membership> findByWorkspaceId(String tenantId, String organizationId, String workspaceId) {
        return jdbcTemplate.query("""
                        SELECT * FROM membership
                        WHERE tenant_id = ? AND organization_id = ? AND workspace_id = ? AND status = 'ACTIVE'
                        ORDER BY user_id ASC
                        """,
                this::mapMembership,
                tenantId,
                organizationId,
                workspaceId
        );
    }

    @Override
    public List<Membership> findAllMemberships() {
        return jdbcTemplate.query("""
                        SELECT * FROM membership
                        ORDER BY scope ASC, tenant_id ASC NULLS FIRST, organization_id ASC NULLS FIRST, workspace_id ASC NULLS FIRST, user_id ASC
                        """,
                this::mapMembership
        );
    }

    private Tenant mapTenant(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Tenant(
                resultSet.getString("tenant_id"),
                resultSet.getString("tenant_key"),
                resultSet.getString("name"),
                GovernanceStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("data_residency"),
                resultSet.getString("default_locale"),
                audit(resultSet)
        );
    }

    private Organization mapOrganization(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Organization(
                resultSet.getString("organization_id"),
                resultSet.getString("tenant_id"),
                resultSet.getString("organization_key"),
                resultSet.getString("name"),
                GovernanceStatus.valueOf(resultSet.getString("status")),
                audit(resultSet)
        );
    }

    private Workspace mapWorkspace(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Workspace(
                resultSet.getString("workspace_id"),
                resultSet.getString("tenant_id"),
                resultSet.getString("organization_id"),
                resultSet.getString("workspace_key"),
                resultSet.getString("name"),
                WorkspaceType.valueOf(resultSet.getString("workspace_type")),
                optionalEnum(resultSet.getString("domain_type"), DomainType::valueOf),
                GovernanceStatus.valueOf(resultSet.getString("status")),
                audit(resultSet)
        );
    }

    private UserAccount mapUserAccount(ResultSet resultSet, int rowNumber) throws SQLException {
        return new UserAccount(
                resultSet.getString("user_id"),
                resultSet.getString("username"),
                resultSet.getString("email"),
                resultSet.getString("display_name"),
                resultSet.getString("password_hash"),
                resultSet.getString("preferred_locale"),
                GovernanceStatus.valueOf(resultSet.getString("status")),
                resultSet.getBoolean("is_system_user"),
                audit(resultSet)
        );
    }

    private Role mapRole(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Role(
                resultSet.getString("role_id"),
                resultSet.getString("role_key"),
                resultSet.getString("name"),
                RoleScope.valueOf(resultSet.getString("allowed_scope")),
                resultSet.getBoolean("system_role"),
                stringSet(resultSet.getString("permissions")),
                audit(resultSet)
        );
    }

    private Membership mapMembership(ResultSet resultSet, int rowNumber) throws SQLException {
        String membershipId = resultSet.getString("membership_id");
        return new Membership(
                membershipId,
                resultSet.getString("user_id"),
                MembershipScope.valueOf(resultSet.getString("scope")),
                resultSet.getString("tenant_id"),
                resultSet.getString("organization_id"),
                resultSet.getString("workspace_id"),
                GovernanceStatus.valueOf(resultSet.getString("status")),
                instant(resultSet, "starts_at"),
                instant(resultSet, "expires_at"),
                membershipRoleKeys(membershipId),
                audit(resultSet)
        );
    }

    private java.util.Set<String> membershipRoleKeys(String membershipId) {
        return java.util.Set.copyOf(jdbcTemplate.queryForList("""
                        SELECT r.role_key
                        FROM membership_role mr
                        JOIN kanon_role r ON r.role_id = mr.role_id
                        WHERE mr.membership_id = ?
                        ORDER BY r.role_key ASC
                        """,
                String.class,
                membershipId
        ));
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
