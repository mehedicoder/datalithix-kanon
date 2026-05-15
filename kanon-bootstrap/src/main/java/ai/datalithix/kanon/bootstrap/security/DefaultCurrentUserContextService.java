package ai.datalithix.kanon.bootstrap.security;

import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.model.GovernanceDefaults;
import ai.datalithix.kanon.tenant.model.Membership;
import ai.datalithix.kanon.tenant.model.MembershipContext;
import ai.datalithix.kanon.tenant.model.UserAccount;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.tenant.service.MembershipRepository;
import ai.datalithix.kanon.tenant.service.RoleRepository;
import ai.datalithix.kanon.tenant.service.UserAccountRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class DefaultCurrentUserContextService implements CurrentUserContextService {
    private final UserAccountRepository userAccountRepository;
    private final MembershipRepository membershipRepository;
    private final RoleRepository roleRepository;

    public DefaultCurrentUserContextService(
            UserAccountRepository userAccountRepository,
            MembershipRepository membershipRepository,
            RoleRepository roleRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public CurrentUserContext currentUser() {
        String username = username();
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseGet(() -> new UserAccount(
                        GovernanceDefaults.SUPER_ADMIN_USER_ID,
                        username,
                        GovernanceDefaults.SUPER_ADMIN_EMAIL,
                        GovernanceDefaults.SUPER_ADMIN_DISPLAY_NAME,
                        "{noop}admin",
                        "en",
                        ai.datalithix.kanon.tenant.model.GovernanceStatus.ACTIVE,
                        true,
                        new ai.datalithix.kanon.common.model.AuditMetadata(java.time.Instant.now(), "system", java.time.Instant.now(), "system", 1)
                ));
        List<Membership> memberships = membershipRepository.findMembershipsByUserId(user.userId());
        Set<String> roleKeys = new HashSet<>();
        Set<String> permissions = new HashSet<>();
        List<MembershipContext> membershipContexts = memberships.stream()
                .map(membership -> {
                    Set<String> membershipPermissions = permissions(membership);
                    roleKeys.addAll(membership.roleKeys());
                    permissions.addAll(membershipPermissions);
                    return new MembershipContext(
                            membership.membershipId(),
                            membership.scope(),
                            membership.tenantId(),
                            membership.organizationId(),
                            membership.workspaceId(),
                            membership.roleKeys(),
                            membershipPermissions
                    );
                })
                .toList();

        Membership activeWorkspace = memberships.stream()
                .filter(membership -> membership.workspaceId() != null && !membership.workspaceId().isBlank())
                .findFirst()
                .orElse(null);

        return new CurrentUserContext(
                user.userId(),
                user.username(),
                activeWorkspace == null ? GovernanceDefaults.DEFAULT_TENANT_ID : activeWorkspace.tenantId(),
                activeWorkspace == null ? GovernanceDefaults.DEFAULT_ORGANIZATION_ID : activeWorkspace.organizationId(),
                activeWorkspace == null ? GovernanceDefaults.ADMINISTRATION_WORKSPACE_ID : activeWorkspace.workspaceId(),
                roleKeys,
                permissions,
                membershipContexts
        );
    }

    private Set<String> permissions(Membership membership) {
        Set<String> permissions = new HashSet<>();
        roleRepository.findByKeys(membership.roleKeys()).forEach(role -> permissions.addAll(role.permissions()));
        return Set.copyOf(permissions);
    }

    private static String username() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {
            return GovernanceDefaults.SUPER_ADMIN_USERNAME;
        }
        return authentication.getName();
    }
}
