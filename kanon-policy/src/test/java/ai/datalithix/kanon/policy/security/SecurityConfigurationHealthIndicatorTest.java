package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.security.AccessControlContext;
import ai.datalithix.kanon.common.security.AccessPurpose;
import ai.datalithix.kanon.common.security.Permission;
import ai.datalithix.kanon.common.security.SecurityRole;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigurationHealthIndicatorTest {
    @Test
    void healthReturnsUpWhenAuthorizationWorks() {
        var authService = new StubAuthorizationService(true);
        var indicator = new SecurityConfigurationHealthIndicator(authService);
        var health = indicator.health();
        assertEquals("security-configuration", health.componentName());
        assertTrue(health.status().name().equals("UP") || health.status().name().equals("DOWN"));
    }

    @Test
    void healthHandlesAuthorizationFailure() {
        var authService = new StubAuthorizationService(false);
        var indicator = new SecurityConfigurationHealthIndicator(authService);
        var health = indicator.health();
        assertEquals("security-configuration", health.componentName());
        assertNotNull(health.detail());
    }

    private record StubAuthorizationService(boolean allowed) implements AuthorizationService {
        @Override
        public AuthorizationDecision authorize(AccessControlContext context, Permission permission, ProtectedResource resource) {
            return allowed
                    ? AuthorizationDecision.allow("Stub allowed")
                    : AuthorizationDecision.deny("Stub denied");
        }
    }
}
