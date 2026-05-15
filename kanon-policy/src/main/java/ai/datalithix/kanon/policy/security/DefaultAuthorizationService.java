package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.security.AccessControlContext;
import ai.datalithix.kanon.common.security.Permission;
import ai.datalithix.kanon.common.security.SecurityRole;
import org.springframework.stereotype.Component;

@Component
public class DefaultAuthorizationService implements AuthorizationService {
    @Override
    public AuthorizationDecision authorize(AccessControlContext context, Permission permission, ProtectedResource resource) {
        if (context == null) {
            return AuthorizationDecision.deny("Missing access context");
        }
        if (resource == null) {
            return AuthorizationDecision.deny("Missing protected resource");
        }
        if (!context.hasPermission(permission)) {
            return AuthorizationDecision.deny("Missing permission " + permission);
        }
        var dimensions = resource.dimensions();
        if (dimensions.tenantId() != null && !dimensions.tenantId().equals(context.tenantId())
                && !context.hasRole(SecurityRole.PLATFORM_ADMIN)) {
            return AuthorizationDecision.deny("Tenant scope mismatch");
        }
        if (dimensions.domainType() != null && context.domainScope() != null
                && !context.domainScope().isEmpty()
                && !context.domainScope().contains(dimensions.domainType())) {
            return AuthorizationDecision.deny("Domain scope mismatch");
        }
        if (dimensions.dataClassification() != null && context.allowedClassifications() != null
                && !context.allowedClassifications().isEmpty()
                && !context.allowedClassifications().contains(dimensions.dataClassification())) {
            return AuthorizationDecision.deny("Classification scope mismatch");
        }
        if (dimensions.assignedUserId() != null
                && context.hasRole(SecurityRole.REVIEWER_ANNOTATOR)
                && !dimensions.assignedUserId().equals(context.userId())) {
            return AuthorizationDecision.deny("Assignment user mismatch");
        }
        if (dimensions.caseId() != null
                && context.assignedCaseIds() != null
                && !context.assignedCaseIds().isEmpty()
                && !context.assignedCaseIds().contains(dimensions.caseId())) {
            return AuthorizationDecision.deny("Assignment case mismatch");
        }
        return AuthorizationDecision.allow("Allowed by role, permission, tenant, domain, and classification scope");
    }
}
