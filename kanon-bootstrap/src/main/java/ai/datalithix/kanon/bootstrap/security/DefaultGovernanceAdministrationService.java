package ai.datalithix.kanon.bootstrap.security;

import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.common.security.AccessPurpose;
import ai.datalithix.kanon.common.security.SecurityAuditEvent;
import ai.datalithix.kanon.common.security.SecurityDimensionSet;
import ai.datalithix.kanon.common.security.SecurityEventType;
import ai.datalithix.kanon.policy.security.SecurityAuditEventPublisher;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.model.GovernanceDefaults;
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
import ai.datalithix.kanon.tenant.service.MembershipRepository;
import ai.datalithix.kanon.tenant.service.OrganizationRepository;
import ai.datalithix.kanon.tenant.service.RoleRepository;
import ai.datalithix.kanon.tenant.service.TenantRepository;
import ai.datalithix.kanon.tenant.service.UserAccountRepository;
import ai.datalithix.kanon.tenant.service.WorkspaceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DefaultGovernanceAdministrationService implements GovernanceAdministrationService {
    private final CurrentUserContextService currentUserContextService;
    private final TenantRepository tenantRepository;
    private final OrganizationRepository organizationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserAccountRepository userAccountRepository;
    private final MembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<SecurityAuditEventPublisher> securityAuditPublisher;

    public DefaultGovernanceAdministrationService(
            CurrentUserContextService currentUserContextService,
            TenantRepository tenantRepository,
            OrganizationRepository organizationRepository,
            WorkspaceRepository workspaceRepository,
            UserAccountRepository userAccountRepository,
            MembershipRepository membershipRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            ObjectProvider<SecurityAuditEventPublisher> securityAuditPublisher
    ) {
        this.currentUserContextService = currentUserContextService;
        this.tenantRepository = tenantRepository;
        this.organizationRepository = organizationRepository;
        this.workspaceRepository = workspaceRepository;
        this.userAccountRepository = userAccountRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityAuditPublisher = securityAuditPublisher;
    }

    @Override
    public List<Tenant> tenants() {
        CurrentUserContext context = current();
        if (has(context, "platform.tenant.create")) {
            return tenantRepository.findAll();
        }
        return tenantRepository.findById(context.activeTenantId()).stream().toList();
    }

    @Override
    public List<Organization> organizations(String tenantId) {
        CurrentUserContext context = current();
        String effectiveTenantId = valueOrDefault(tenantId, context.activeTenantId());
        requireTenantRead(context, effectiveTenantId);
        List<Organization> organizations = organizationRepository.findByTenantId(effectiveTenantId);
        if (has(context, "platform.organization.create") || has(context, "tenant.organization.create")) {
            return organizations;
        }
        return organizations.stream()
                .filter(organization -> organization.organizationId().equals(context.activeOrganizationId()))
                .toList();
    }

    @Override
    public List<Workspace> workspaces(String tenantId, String organizationId) {
        CurrentUserContext context = current();
        String effectiveTenantId = valueOrDefault(tenantId, context.activeTenantId());
        String effectiveOrganizationId = valueOrDefault(organizationId, context.activeOrganizationId());
        requireOrganizationRead(context, effectiveTenantId, effectiveOrganizationId);
        List<Workspace> workspaces = workspaceRepository.findByOrganizationId(effectiveTenantId, effectiveOrganizationId);
        if (has(context, "platform.workspace.create") || has(context, "tenant.workspace.create") || has(context, "organization.workspace.create")) {
            return workspaces;
        }
        return workspaces.stream()
                .filter(workspace -> workspace.workspaceId().equals(context.activeWorkspaceId()))
                .toList();
    }

    @Override
    public List<UserAccount> users() {
        CurrentUserContext context = current();
        requireAny(context, "platform.user.create", "tenant.user.create", "organization.membership.assign", "workspace.membership.assign");
        if (has(context, "platform.user.create") || has(context, "platform.membership.assign")) {
            return userAccountRepository.findAllUsers();
        }
        Set<String> scopedUserIds = membershipRepository.findAllMemberships().stream()
                .filter(membership -> inScope(context, membership))
                .map(Membership::userId)
                .collect(java.util.stream.Collectors.toSet());
        return userAccountRepository.findAllUsers().stream()
                .filter(user -> user.userId().equals(context.userId()) || scopedUserIds.contains(user.userId()))
                .toList();
    }

    @Override
    public List<Membership> memberships() {
        CurrentUserContext context = current();
        requireAny(context, "platform.membership.assign", "tenant.membership.assign", "organization.membership.assign", "workspace.membership.assign");
        if (has(context, "platform.membership.assign")) {
            return membershipRepository.findAllMemberships();
        }
        return membershipRepository.findAllMemberships().stream()
                .filter(membership -> inScope(context, membership))
                .toList();
    }

    @Override
    public List<Role> roles() {
        requireAny(current(), "platform.role.assign", "tenant.role.assign", "organization.membership.assign", "workspace.membership.assign");
        return roleRepository.findAllRoles();
    }

    @Override
    public Tenant createTenant(String tenantKey, String name, String dataResidency, String defaultLocale) {
        CurrentUserContext context = current();
        require(context, "platform.tenant.create");
        String key = normalize(tenantKey);
        return tenantRepository.save(new Tenant(
                key,
                key,
                name,
                GovernanceStatus.ACTIVE,
                blankToDefault(dataResidency, "DEFAULT"),
                blankToDefault(defaultLocale, "en"),
                audit(context.username())
        ));
    }

    @Override
    public Organization createOrganization(String tenantId, String organizationKey, String name) {
        CurrentUserContext context = current();
        String effectiveTenantId = valueOrDefault(tenantId, context.activeTenantId());
        if (has(context, "platform.organization.create")) {
            // allowed
        } else {
            require(context, "tenant.organization.create");
            requireSameTenant(context, effectiveTenantId);
        }
        String key = normalize(organizationKey);
        return organizationRepository.save(new Organization(
                key,
                effectiveTenantId,
                key,
                name,
                GovernanceStatus.ACTIVE,
                audit(context.username())
        ));
    }

    @Override
    public Workspace createWorkspace(String tenantId, String organizationId, String workspaceKey, String name, WorkspaceType workspaceType, DomainType domainType) {
        CurrentUserContext context = current();
        String effectiveTenantId = valueOrDefault(tenantId, context.activeTenantId());
        String effectiveOrganizationId = valueOrDefault(organizationId, context.activeOrganizationId());
        if (has(context, "platform.workspace.create")) {
            // allowed
        } else if (has(context, "tenant.workspace.create")) {
            requireSameTenant(context, effectiveTenantId);
        } else {
            require(context, "organization.workspace.create");
            requireSameOrganization(context, effectiveTenantId, effectiveOrganizationId);
        }
        String key = normalize(workspaceKey);
        return workspaceRepository.save(new Workspace(
                key,
                effectiveTenantId,
                effectiveOrganizationId,
                key,
                name,
                workspaceType == null ? WorkspaceType.BUSINESS : workspaceType,
                domainType,
                GovernanceStatus.ACTIVE,
                audit(context.username())
        ));
    }

    @Override
    public UserAccount createUser(String username, String email, String displayName, String rawPassword) {
        CurrentUserContext context = current();
        requireAny(context, "platform.user.create", "tenant.user.create", "organization.membership.assign");
        String userId = normalize(username);
        return userAccountRepository.save(new UserAccount(
                userId,
                username,
                email,
                displayName,
                passwordEncoder.encode(blankToDefault(rawPassword, "change-me")),
                null,
                GovernanceStatus.ACTIVE,
                false,
                audit(context.username())
        ));
    }

    @Override
    public Membership assignMembership(String userId, MembershipScope scope, String tenantId, String organizationId, String workspaceId, Set<String> roleKeys) {
        CurrentUserContext context = current();
        MembershipScope effectiveScope = scope == null ? MembershipScope.WORKSPACE : scope;
        String effectiveTenantId = effectiveScope == MembershipScope.PLATFORM ? null : valueOrDefault(tenantId, context.activeTenantId());
        String effectiveOrganizationId = requiresOrganization(effectiveScope) ? valueOrDefault(organizationId, context.activeOrganizationId()) : null;
        String effectiveWorkspaceId = effectiveScope == MembershipScope.WORKSPACE ? valueOrDefault(workspaceId, context.activeWorkspaceId()) : null;
        requireMembershipPermission(context, effectiveScope, effectiveTenantId, effectiveOrganizationId);
        String membershipId = normalize(userId + "-" + effectiveScope + "-" + UUID.randomUUID());
        Membership membership = membershipRepository.save(new Membership(
                membershipId,
                userId,
                effectiveScope,
                effectiveTenantId,
                effectiveOrganizationId,
                effectiveWorkspaceId,
                GovernanceStatus.ACTIVE,
                Instant.now(),
                null,
                roleKeys,
                audit(context.username())
        ));
        publish(context, SecurityEventType.ROLE_ASSIGNMENT_CHANGED, "SUCCESS", "membership assigned", effectiveTenantId, Map.of(
                "membershipId", membership.membershipId(),
                "userId", userId,
                "scope", effectiveScope.name()
        ));
        return membership;
    }

    @Override
    public Tenant updateTenant(String tenantId, String tenantKey, String name, String dataResidency, String defaultLocale) {
        CurrentUserContext context = current();
        if (has(context, "platform.tenant.update")) {
            // allowed
        } else {
            require(context, "tenant.update");
            requireSameTenant(context, tenantId);
        }
        Tenant existing = tenantRepository.findById(tenantId).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        Tenant updated = tenantRepository.save(new Tenant(
                existing.tenantId(),
                normalize(tenantKey),
                blankToDefault(name, existing.name()),
                existing.status(),
                blankToDefault(dataResidency, existing.dataResidency()),
                blankToDefault(defaultLocale, existing.defaultLocale()),
                updatedAudit(existing.audit(), context.username())
        ));
        publish(context, SecurityEventType.PERMISSION_CHANGED, "SUCCESS", "tenant updated", tenantId, Map.of("resource", "tenant"));
        return updated;
    }

    @Override
    public Tenant archiveTenant(String tenantId) {
        CurrentUserContext context = current();
        require(context, "platform.tenant.delete");
        Tenant existing = tenantRepository.findById(tenantId).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        if (GovernanceDefaults.DEFAULT_TENANT_ID.equals(existing.tenantId())) {
            throw new IllegalArgumentException("Default tenant cannot be deleted");
        }
        return tenantRepository.save(new Tenant(
                existing.tenantId(),
                existing.tenantKey(),
                existing.name(),
                GovernanceStatus.ARCHIVED,
                existing.dataResidency(),
                existing.defaultLocale(),
                updatedAudit(existing.audit(), context.username())
        ));
    }

    @Override
    public Tenant restoreTenant(String tenantId) {
        CurrentUserContext context = current();
        require(context, "platform.tenant.restore");
        Tenant existing = tenantRepository.findById(tenantId).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        return tenantRepository.save(new Tenant(
                existing.tenantId(),
                existing.tenantKey(),
                existing.name(),
                GovernanceStatus.ACTIVE,
                existing.dataResidency(),
                existing.defaultLocale(),
                updatedAudit(existing.audit(), context.username())
        ));
    }

    @Override
    public Organization updateOrganization(String tenantId, String organizationId, String organizationKey, String name) {
        CurrentUserContext context = current();
        if (has(context, "platform.organization.update")) {
            // allowed
        } else if (has(context, "tenant.organization.update")) {
            requireSameTenant(context, tenantId);
        } else {
            require(context, "organization.update");
            requireSameOrganization(context, tenantId, organizationId);
        }
        Organization existing = organizationRepository.findById(tenantId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        Organization updated = organizationRepository.save(new Organization(
                existing.organizationId(),
                existing.tenantId(),
                normalize(organizationKey),
                blankToDefault(name, existing.name()),
                existing.status(),
                updatedAudit(existing.audit(), context.username())
        ));
        publish(context, SecurityEventType.PERMISSION_CHANGED, "SUCCESS", "organization updated", tenantId, Map.of("resource", "organization", "organizationId", organizationId));
        return updated;
    }

    @Override
    public Organization archiveOrganization(String tenantId, String organizationId) {
        CurrentUserContext context = current();
        require(context, "platform.organization.delete");
        Organization existing = organizationRepository.findById(tenantId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        if (GovernanceDefaults.DEFAULT_TENANT_ID.equals(existing.tenantId())
                && GovernanceDefaults.DEFAULT_ORGANIZATION_ID.equals(existing.organizationId())) {
            throw new IllegalArgumentException("Default organization cannot be deleted");
        }
        return organizationRepository.save(new Organization(
                existing.organizationId(),
                existing.tenantId(),
                existing.organizationKey(),
                existing.name(),
                GovernanceStatus.ARCHIVED,
                updatedAudit(existing.audit(), context.username())
        ));
    }

    @Override
    public Organization restoreOrganization(String tenantId, String organizationId) {
        CurrentUserContext context = current();
        require(context, "platform.organization.restore");
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        if (tenant.status() != GovernanceStatus.ACTIVE) {
            throw new IllegalArgumentException("Tenant must be active before restoring organization");
        }
        Organization existing = organizationRepository.findById(tenantId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        return organizationRepository.save(new Organization(
                existing.organizationId(),
                existing.tenantId(),
                existing.organizationKey(),
                existing.name(),
                GovernanceStatus.ACTIVE,
                updatedAudit(existing.audit(), context.username())
        ));
    }

    @Override
    public Workspace updateWorkspace(String tenantId, String organizationId, String workspaceId, String workspaceKey, String name, WorkspaceType workspaceType, DomainType domainType) {
        CurrentUserContext context = current();
        if (has(context, "platform.workspace.update")) {
            // allowed
        } else if (has(context, "tenant.workspace.update")) {
            requireSameTenant(context, tenantId);
        } else if (has(context, "organization.workspace.update")) {
            requireSameOrganization(context, tenantId, organizationId);
        } else {
            require(context, "workspace.update");
            requireSameWorkspace(context, tenantId, organizationId, workspaceId);
        }
        Workspace existing = workspaceRepository.findById(tenantId, organizationId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
        Workspace updated = workspaceRepository.save(new Workspace(
                existing.workspaceId(),
                existing.tenantId(),
                existing.organizationId(),
                normalize(workspaceKey),
                blankToDefault(name, existing.name()),
                workspaceType == null ? existing.workspaceType() : workspaceType,
                domainType,
                existing.status(),
                updatedAudit(existing.audit(), context.username())
        ));
        publish(context, SecurityEventType.PERMISSION_CHANGED, "SUCCESS", "workspace updated", tenantId, Map.of("resource", "workspace", "workspaceId", workspaceId));
        return updated;
    }

    @Override
    public Workspace archiveWorkspace(String tenantId, String organizationId, String workspaceId) {
        CurrentUserContext context = current();
        require(context, "platform.workspace.delete");
        Workspace existing = workspaceRepository.findById(tenantId, organizationId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
        if (GovernanceDefaults.DEFAULT_TENANT_ID.equals(existing.tenantId())
                && GovernanceDefaults.DEFAULT_ORGANIZATION_ID.equals(existing.organizationId())
                && GovernanceDefaults.ADMINISTRATION_WORKSPACE_ID.equals(existing.workspaceId())) {
            throw new IllegalArgumentException("Administration workspace cannot be deleted");
        }
        return workspaceRepository.save(new Workspace(
                existing.workspaceId(),
                existing.tenantId(),
                existing.organizationId(),
                existing.workspaceKey(),
                existing.name(),
                existing.workspaceType(),
                existing.domainType(),
                GovernanceStatus.ARCHIVED,
                updatedAudit(existing.audit(), context.username())
        ));
    }

    @Override
    public Workspace restoreWorkspace(String tenantId, String organizationId, String workspaceId) {
        CurrentUserContext context = current();
        require(context, "platform.workspace.restore");
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        if (tenant.status() != GovernanceStatus.ACTIVE) {
            throw new IllegalArgumentException("Tenant must be active before restoring workspace");
        }
        Organization organization = organizationRepository.findById(tenantId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        if (organization.status() != GovernanceStatus.ACTIVE) {
            throw new IllegalArgumentException("Organization must be active before restoring workspace");
        }
        Workspace existing = workspaceRepository.findById(tenantId, organizationId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
        return workspaceRepository.save(new Workspace(
                existing.workspaceId(),
                existing.tenantId(),
                existing.organizationId(),
                existing.workspaceKey(),
                existing.name(),
                existing.workspaceType(),
                existing.domainType(),
                GovernanceStatus.ACTIVE,
                updatedAudit(existing.audit(), context.username())
        ));
    }

    @Override
    public UserAccount updateUser(String userId, String username, String email, String displayName) {
        CurrentUserContext context = current();
        if (!has(context, "platform.user.update")) {
            requireSameScopedUser(context, userId, "tenant.user.update", "organization.user.update");
        }
        UserAccount existing = userAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return userAccountRepository.save(new UserAccount(
                existing.userId(),
                blankToDefault(username, existing.username()),
                blankToDefault(email, existing.email()),
                blankToDefault(displayName, existing.displayName()),
                existing.passwordHash(),
                existing.preferredLocale(),
                existing.status(),
                existing.systemUser(),
                updatedAudit(existing.audit(), context.username())
        ));
    }

    @Override
    public UserAccount archiveUser(String userId) {
        CurrentUserContext context = current();
        require(context, "platform.user.delete");
        UserAccount existing = userAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (existing.systemUser()) {
            throw new IllegalArgumentException("System users cannot be deleted");
        }
        return userAccountRepository.save(new UserAccount(
                existing.userId(),
                existing.username(),
                existing.email(),
                existing.displayName(),
                existing.passwordHash(),
                existing.preferredLocale(),
                GovernanceStatus.ARCHIVED,
                existing.systemUser(),
                updatedAudit(existing.audit(), context.username())
        ));
    }

    @Override
    public UserAccount restoreUser(String userId) {
        CurrentUserContext context = current();
        require(context, "platform.user.restore");
        UserAccount existing = userAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return userAccountRepository.save(new UserAccount(
                existing.userId(),
                existing.username(),
                existing.email(),
                existing.displayName(),
                existing.passwordHash(),
                existing.preferredLocale(),
                GovernanceStatus.ACTIVE,
                existing.systemUser(),
                updatedAudit(existing.audit(), context.username())
        ));
    }

    private CurrentUserContext current() {
        return currentUserContextService.currentUser();
    }

    private void requireTenantRead(CurrentUserContext context, String tenantId) {
        if (has(context, "platform.tenant.create")) {
            return;
        }
        requireSameTenant(context, tenantId);
    }

    private void requireOrganizationRead(CurrentUserContext context, String tenantId, String organizationId) {
        if (has(context, "platform.organization.create") || has(context, "tenant.organization.create")) {
            requireTenantRead(context, tenantId);
            return;
        }
        requireSameOrganization(context, tenantId, organizationId);
    }

    private void requireMembershipPermission(CurrentUserContext context, MembershipScope scope, String tenantId, String organizationId) {
        if (has(context, "platform.membership.assign")) {
            return;
        }
        if (scope == MembershipScope.TENANT) {
            require(context, "tenant.membership.assign");
            requireSameTenant(context, tenantId);
            return;
        }
        if (scope == MembershipScope.ORGANIZATION || scope == MembershipScope.WORKSPACE) {
            if (has(context, "tenant.membership.assign")) {
                requireSameTenant(context, tenantId);
                return;
            }
            require(context, "organization.membership.assign");
            requireSameOrganization(context, tenantId, organizationId);
            return;
        }
        require(context, "platform.membership.assign");
    }

    private static boolean inScope(CurrentUserContext context, Membership membership) {
        if (membership.tenantId() == null) {
            return false;
        }
        if (!membership.tenantId().equals(context.activeTenantId())) {
            return false;
        }
        return membership.organizationId() == null || membership.organizationId().equals(context.activeOrganizationId());
    }

    private static boolean requiresOrganization(MembershipScope scope) {
        return scope == MembershipScope.ORGANIZATION || scope == MembershipScope.WORKSPACE;
    }

    private void requireSameTenant(CurrentUserContext context, String tenantId) {
        if (!context.activeTenantId().equals(tenantId)) {
            denied(context, "Access denied outside tenant scope");
            throw new SecurityException("Access denied outside tenant scope");
        }
    }

    private void requireSameOrganization(CurrentUserContext context, String tenantId, String organizationId) {
        requireSameTenant(context, tenantId);
        if (!context.activeOrganizationId().equals(organizationId)) {
            denied(context, "Access denied outside organization scope");
            throw new SecurityException("Access denied outside organization scope");
        }
    }

    private void requireSameWorkspace(CurrentUserContext context, String tenantId, String organizationId, String workspaceId) {
        requireSameOrganization(context, tenantId, organizationId);
        if (!context.activeWorkspaceId().equals(workspaceId)) {
            denied(context, "Access denied outside workspace scope");
            throw new SecurityException("Access denied outside workspace scope");
        }
    }

    private void requireSameScopedUser(CurrentUserContext context, String userId, String tenantPermission, String organizationPermission) {
        if (has(context, tenantPermission)) {
            boolean sameTenant = membershipRepository.findMembershipsByUserId(userId).stream()
                    .anyMatch(membership -> context.activeTenantId().equals(membership.tenantId()));
            if (sameTenant) {
                return;
            }
        }
        if (has(context, organizationPermission)) {
            boolean sameOrganization = membershipRepository.findMembershipsByUserId(userId).stream()
                    .anyMatch(membership -> context.activeTenantId().equals(membership.tenantId())
                            && context.activeOrganizationId().equals(membership.organizationId()));
            if (sameOrganization) {
                return;
            }
        }
        denied(context, "Missing scoped user update permission");
        throw new SecurityException("Access denied");
    }

    private void requireAny(CurrentUserContext context, String... permissions) {
        for (String permission : permissions) {
            if (has(context, permission)) {
                return;
            }
        }
        denied(context, "Missing any permission: " + String.join(", ", permissions));
        throw new SecurityException("Access denied");
    }

    private void require(CurrentUserContext context, String permission) {
        if (!has(context, permission)) {
            denied(context, "Missing permission: " + permission);
            throw new SecurityException("Missing permission: " + permission);
        }
    }

    private void denied(CurrentUserContext context, String reason) {
        publish(context, SecurityEventType.UNAUTHORIZED_ACCESS_DENIED, "DENIED", reason, context.activeTenantId(), Map.of());
    }

    private void publish(CurrentUserContext context, SecurityEventType eventType, String outcome, String reason, String tenantId, Map<String, String> attributes) {
        securityAuditPublisher.ifAvailable(publisher -> publisher.publish(new SecurityAuditEvent(
                "security-" + UUID.randomUUID(),
                valueOrDefault(tenantId, context.activeTenantId()),
                context.userId(),
                eventType,
                new SecurityDimensionSet(
                        valueOrDefault(tenantId, context.activeTenantId()),
                        context.activeOrganizationId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        context.userId(),
                        context.userId(),
                        null,
                        AccessPurpose.ADMINISTRATION
                ),
                outcome,
                reason,
                Instant.now(),
                attributes
        )));
    }

    private static boolean has(CurrentUserContext context, String permission) {
        return context.permissions().contains(permission);
    }

    private static AuditMetadata audit(String actor) {
        Instant now = Instant.now();
        return new AuditMetadata(now, actor, now, actor, 1);
    }

    private static AuditMetadata updatedAudit(AuditMetadata existing, String actor) {
        return new AuditMetadata(
                existing.createdAt(),
                existing.createdBy(),
                Instant.now(),
                actor,
                existing.version() + 1
        );
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "id-" + UUID.randomUUID();
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
