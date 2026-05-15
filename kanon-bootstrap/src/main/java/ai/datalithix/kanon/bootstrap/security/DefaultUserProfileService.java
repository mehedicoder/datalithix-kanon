package ai.datalithix.kanon.bootstrap.security;

import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.model.GovernanceStatus;
import ai.datalithix.kanon.tenant.model.Tenant;
import ai.datalithix.kanon.tenant.model.UserAccount;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.tenant.service.TenantRepository;
import ai.datalithix.kanon.tenant.service.UserAccountRepository;
import ai.datalithix.kanon.tenant.service.UserProfileService;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class DefaultUserProfileService implements UserProfileService {
    private static final String DEFAULT_LOCALE = "en";

    private final CurrentUserContextService currentUserContextService;
    private final UserAccountRepository userAccountRepository;
    private final TenantRepository tenantRepository;

    public DefaultUserProfileService(
            CurrentUserContextService currentUserContextService,
            UserAccountRepository userAccountRepository,
            TenantRepository tenantRepository
    ) {
        this.currentUserContextService = currentUserContextService;
        this.userAccountRepository = userAccountRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public UserAccount currentUser() {
        CurrentUserContext context = currentUserContextService.currentUser();
        return userAccountRepository.findByUserId(context.userId())
                .or(() -> userAccountRepository.findByUsername(context.username()))
                .orElseThrow(() -> new IllegalStateException("Current user account not found"));
    }

    @Override
    public String effectiveLocale() {
        UserAccount user = currentUser();
        if (hasText(user.preferredLocale())) {
            return user.preferredLocale();
        }
        CurrentUserContext context = currentUserContextService.currentUser();
        return tenantRepository.findById(context.activeTenantId())
                .map(Tenant::defaultLocale)
                .filter(DefaultUserProfileService::hasText)
                .orElse(DEFAULT_LOCALE);
    }

    @Override
    public UserAccount updatePreferredLocale(String preferredLocale) {
        CurrentUserContext context = currentUserContextService.currentUser();
        UserAccount existing = currentUser();
        return userAccountRepository.save(new UserAccount(
                existing.userId(),
                existing.username(),
                existing.email(),
                existing.displayName(),
                existing.passwordHash(),
                normalizeLocale(preferredLocale),
                existing.status() == null ? GovernanceStatus.ACTIVE : existing.status(),
                existing.systemUser(),
                updatedAudit(existing.audit(), context.username())
        ));
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

    private static String normalizeLocale(String locale) {
        return hasText(locale) ? locale.trim().toLowerCase(java.util.Locale.ROOT) : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
