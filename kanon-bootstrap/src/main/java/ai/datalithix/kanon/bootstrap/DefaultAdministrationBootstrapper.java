package ai.datalithix.kanon.bootstrap;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.tenant.model.GovernanceDefaults;
import ai.datalithix.kanon.tenant.model.GovernanceStatus;
import ai.datalithix.kanon.tenant.model.Membership;
import ai.datalithix.kanon.tenant.model.MembershipScope;
import ai.datalithix.kanon.tenant.model.Organization;
import ai.datalithix.kanon.tenant.model.Role;
import ai.datalithix.kanon.tenant.model.RoleKeys;
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
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DefaultAdministrationBootstrapper implements ApplicationRunner {
    private static final String SYSTEM = "system";

    private final TenantRepository tenantRepository;
    private final OrganizationRepository organizationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final MembershipRepository membershipRepository;

    public DefaultAdministrationBootstrapper(
            TenantRepository tenantRepository,
            OrganizationRepository organizationRepository,
            WorkspaceRepository workspaceRepository,
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            MembershipRepository membershipRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.organizationRepository = organizationRepository;
        this.workspaceRepository = workspaceRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        AuditMetadata audit = audit();
        seedRoles(audit);
        tenantRepository.save(new Tenant(
                GovernanceDefaults.DEFAULT_TENANT_ID,
                GovernanceDefaults.DEFAULT_TENANT_KEY,
                GovernanceDefaults.DEFAULT_TENANT_NAME,
                GovernanceStatus.ACTIVE,
                "DEFAULT",
                "en",
                audit
        ));
        organizationRepository.save(new Organization(
                GovernanceDefaults.DEFAULT_ORGANIZATION_ID,
                GovernanceDefaults.DEFAULT_TENANT_ID,
                GovernanceDefaults.DEFAULT_ORGANIZATION_KEY,
                GovernanceDefaults.DEFAULT_ORGANIZATION_NAME,
                GovernanceStatus.ACTIVE,
                audit
        ));
        workspaceRepository.save(new Workspace(
                GovernanceDefaults.ADMINISTRATION_WORKSPACE_ID,
                GovernanceDefaults.DEFAULT_TENANT_ID,
                GovernanceDefaults.DEFAULT_ORGANIZATION_ID,
                GovernanceDefaults.ADMINISTRATION_WORKSPACE_KEY,
                GovernanceDefaults.ADMINISTRATION_WORKSPACE_NAME,
                WorkspaceType.ADMINISTRATION,
                null,
                GovernanceStatus.ACTIVE,
                audit
        ));
        userAccountRepository.save(new UserAccount(
                GovernanceDefaults.SUPER_ADMIN_USER_ID,
                GovernanceDefaults.SUPER_ADMIN_USERNAME,
                GovernanceDefaults.SUPER_ADMIN_EMAIL,
                GovernanceDefaults.SUPER_ADMIN_DISPLAY_NAME,
                "{noop}admin",
                "en",
                GovernanceStatus.ACTIVE,
                true,
                audit
        ));
        membershipRepository.save(new Membership(
                "membership-platform-superadmin",
                GovernanceDefaults.SUPER_ADMIN_USER_ID,
                MembershipScope.PLATFORM,
                null,
                null,
                null,
                GovernanceStatus.ACTIVE,
                audit.createdAt(),
                null,
                Set.of(RoleKeys.PLATFORM_SUPER_ADMIN),
                audit
        ));
        membershipRepository.save(new Membership(
                "membership-administration-superadmin",
                GovernanceDefaults.SUPER_ADMIN_USER_ID,
                MembershipScope.WORKSPACE,
                GovernanceDefaults.DEFAULT_TENANT_ID,
                GovernanceDefaults.DEFAULT_ORGANIZATION_ID,
                GovernanceDefaults.ADMINISTRATION_WORKSPACE_ID,
                GovernanceStatus.ACTIVE,
                audit.createdAt(),
                null,
                Set.of(RoleKeys.WORKSPACE_MANAGER, RoleKeys.AUDITOR),
                audit
        ));
    }

    private void seedRoles(AuditMetadata audit) {
        roleDefinitions().forEach((roleKey, definition) -> roleRepository.save(new Role(
                roleKey.toLowerCase().replace('_', '-'),
                roleKey,
                definition.name(),
                definition.scope(),
                true,
                definition.permissions(),
                audit
        )));
    }

    private static Map<String, RoleDefinition> roleDefinitions() {
        return Map.ofEntries(
                Map.entry(RoleKeys.PLATFORM_SUPER_ADMIN, new RoleDefinition("Platform Super Admin", RoleScope.PLATFORM, Set.of(
                        "platform.config.manage",
                        "platform.tenant.create", "platform.tenant.update", "platform.tenant.delete", "platform.tenant.restore",
                        "platform.organization.create", "platform.organization.update", "platform.organization.delete", "platform.organization.restore",
                        "platform.workspace.create", "platform.workspace.update", "platform.workspace.delete", "platform.workspace.restore",
                        "platform.user.create", "platform.user.update", "platform.user.delete", "platform.user.restore",
                        "platform.membership.assign",
                        "platform.role.assign", "platform.audit.read", "platform.evidence.read",
                        "platform.agent.read", "platform.agent.create", "platform.agent.update", "platform.agent.delete", "platform.agent.restore",
                        "platform.workflow.read", "platform.workflow.create", "platform.workflow.update", "platform.workflow.delete"
                ))),
                Map.entry(RoleKeys.PLATFORM_CONFIG_ADMIN, new RoleDefinition("Platform Config Admin", RoleScope.PLATFORM, Set.of(
                        "platform.config.read", "platform.config.manage", "platform.audit.read"
                ))),
                Map.entry(RoleKeys.TENANT_ADMIN, new RoleDefinition("Tenant Admin", RoleScope.TENANT, Set.of(
                        "tenant.read", "tenant.update", "tenant.config.manage", "tenant.organization.create",
                        "tenant.workspace.create", "tenant.user.create", "tenant.membership.assign",
                        "tenant.role.assign", "tenant.audit.read",
                        "tenant.agent.read", "tenant.agent.create", "tenant.agent.update", "tenant.agent.delete", "tenant.agent.restore",
                        "tenant.workflow.read", "tenant.workflow.create", "tenant.workflow.update", "tenant.workflow.delete"
                ))),
                Map.entry(RoleKeys.TENANT_CONFIG_ADMIN, new RoleDefinition("Tenant Config Admin", RoleScope.TENANT, Set.of(
                        "tenant.config.read", "tenant.config.manage", "tenant.model.manage", "tenant.policy.manage"
                ))),
                Map.entry(RoleKeys.ORGANIZATION_ADMIN, new RoleDefinition("Organization Admin", RoleScope.ORGANIZATION, Set.of(
                        "organization.read", "organization.update", "organization.workspace.create", "organization.membership.assign"
                ))),
                Map.entry(RoleKeys.WORKSPACE_MANAGER, new RoleDefinition("Workspace Manager", RoleScope.WORKSPACE, Set.of(
                        "workspace.read", "workspace.update", "workspace.membership.assign",
                        "workspace.workflow.read", "workspace.workflow.create", "workspace.workflow.update", "workspace.workflow.delete",
                        "workspace.agent.read", "workspace.agent.create", "workspace.agent.update", "workspace.agent.delete", "workspace.agent.restore",
                        "workspace.case.manage", "workspace.task.assign", "workspace.audit.read"
                ))),
                Map.entry(RoleKeys.ANNOTATOR, new RoleDefinition("Annotator", RoleScope.WORKSPACE, Set.of(
                        "workspace.read", "workspace.task.read-assigned", "workspace.annotation.create", "workspace.annotation.submit"
                ))),
                Map.entry(RoleKeys.REVIEWER, new RoleDefinition("Reviewer", RoleScope.WORKSPACE, Set.of(
                        "workspace.read", "workspace.task.read-assigned", "workspace.review.start", "workspace.review.complete"
                ))),
                Map.entry(RoleKeys.APPROVER, new RoleDefinition("Approver", RoleScope.WORKSPACE, Set.of(
                        "workspace.read", "workspace.approval.approve", "workspace.approval.reject", "workspace.export.mark-ready"
                ))),
                Map.entry(RoleKeys.AUDITOR, new RoleDefinition("Auditor", RoleScope.WORKSPACE, Set.of(
                        "workspace.read", "workspace.audit.read", "workspace.evidence.read"
                ))),
                Map.entry(RoleKeys.VIEWER, new RoleDefinition("Viewer", RoleScope.WORKSPACE, Set.of(
                        "workspace.read", "workspace.case.read", "workspace.task.read"
                ))),
                Map.entry(RoleKeys.MODEL_OPERATOR, new RoleDefinition("Model Operator", RoleScope.WORKSPACE, Set.of(
                        "workspace.model-route.read", "workspace.model-route.manage", "workspace.model.test"
                ))),
                Map.entry(RoleKeys.INTEGRATION_SERVICE_ACCOUNT, new RoleDefinition("Integration Service Account", RoleScope.WORKSPACE, Set.of(
                        "workspace.source.ingest", "workspace.task.create", "workspace.evidence.append", "workspace.connector.execute"
                )))
        );
    }

    private static AuditMetadata audit() {
        Instant now = Instant.now();
        return new AuditMetadata(now, SYSTEM, now, SYSTEM, 1);
    }

    private record RoleDefinition(String name, RoleScope scope, Set<String> permissions) {}
}
