package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.compliance.DataClassification;
import ai.datalithix.kanon.common.runtime.ComponentHealth;
import ai.datalithix.kanon.common.runtime.HealthIndicator;
import ai.datalithix.kanon.common.security.AccessControlContext;
import ai.datalithix.kanon.common.security.AccessPurpose;
import ai.datalithix.kanon.common.security.Permission;
import ai.datalithix.kanon.common.security.SecurityRole;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SecurityConfigurationHealthIndicator implements HealthIndicator {
    private final AuthorizationService authorizationService;

    public SecurityConfigurationHealthIndicator(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public ComponentHealth health() {
        try {
            var context = new AccessControlContext(
                    "health-check", "system", Set.of(SecurityRole.PLATFORM_ADMIN), Set.of(),
                    Set.of(), Set.of(), Set.of(), AccessPurpose.REVIEW);
            var resource = new ProtectedResource("health-check", "health-check",
                    "health-check", DomainType.CUSTOM, null, null, null, DataClassification.PUBLIC);
            var result = authorizationService.authorize(
                    context, Permission.MODEL_VIEW_HEALTH, resource);
            return ComponentHealth.up("security-configuration",
                    "Authorization service is reachable: " + result.reason());
        } catch (Exception e) {
            return ComponentHealth.down("security-configuration",
                    "Security configuration error: " + e.getMessage());
        }
    }
}
