package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.common.DataResidency;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.compliance.ComplianceClassification;
import ai.datalithix.kanon.common.compliance.DataClassification;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.common.security.AccessControlContext;
import ai.datalithix.kanon.common.security.AccessPurpose;
import ai.datalithix.kanon.common.security.BreakGlassGrant;
import ai.datalithix.kanon.common.security.Permission;
import ai.datalithix.kanon.common.security.SecurityDimensionSet;
import ai.datalithix.kanon.common.security.SecurityRole;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityFoundationTest {
    private static final String TENANT_A = "tenant-a";
    private static final String TENANT_B = "tenant-b";
    private static final String USER_ALICE = "alice";
    private static final String USER_BOB = "bob";
    private static final String ADMIN_USER = "admin";

    // ─── Cross-Tenant Isolation Tests ───────────────────────────────

    @Test
    void deniesAccessWhenTenantMismatch() {
        var auth = new DefaultAuthorizationService();
        var ctx = context(TENANT_A, USER_ALICE, Set.of(SecurityRole.TENANT_ADMIN), Set.of(Permission.ANNOTATION_VIEW));
        var resource = new ProtectedResource("annotation", "ann-1",
                new SecurityDimensionSet(TENANT_B, null, null, null, null, null, null,
                        null, ComplianceClassification.NONE, DataResidency.UNKNOWN, null, null, null, null, AccessPurpose.ADMINISTRATION));

        var result = auth.authorize(ctx, Permission.ANNOTATION_VIEW, resource);

        assertFalse(result.allowed());
        assertEquals("Tenant scope mismatch", result.reason());
    }

    @Test
    void allowsPlatformAdminAcrossTenants() {
        var auth = new DefaultAuthorizationService();
        var ctx = context(TENANT_A, USER_ALICE, Set.of(SecurityRole.PLATFORM_ADMIN), Set.of(Permission.EVIDENCE_VIEW));
        var resource = new ProtectedResource("evidence", "evt-1",
                new SecurityDimensionSet(TENANT_B, null, null, null, null, null, null,
                        null, ComplianceClassification.NONE, DataResidency.UNKNOWN, null, null, null, null, AccessPurpose.AUDIT));

        var result = auth.authorize(ctx, Permission.EVIDENCE_VIEW, resource);

        assertTrue(result.allowed());
    }

    @Test
    void deniesBreakGlassAccessAcrossTenants() {
        var repo = new InMemoryBreakGlassGrantRepository();
        var ledger = new CapturingEvidenceLedger();
        var audit = new CapturingAuditPublisher();
        var service = new DefaultBreakGlassService(repo, ledger, audit);

        repo.save(new BreakGlassGrant("bg-1", TENANT_A, USER_ALICE, "emergency", "admin",
                Instant.now().minusSeconds(10), Instant.now().plusSeconds(3600),
                Set.of(Permission.EVIDENCE_VIEW), "evt-1",
                new AuditMetadata(Instant.now(), "admin", Instant.now(), "admin", 1)));

        var foundTenantA = service.findActiveGrant(TENANT_A, USER_ALICE);
        var foundTenantB = service.findActiveGrant(TENANT_B, USER_ALICE);

        assertTrue(foundTenantA.isPresent());
        assertTrue(foundTenantB.isEmpty());
    }

    // ─── Unauthorized Access Tests ──────────────────────────────────

    @Test
    void deniesWhenMissingPermission() {
        var auth = new DefaultAuthorizationService();
        var ctx = context(TENANT_A, USER_ALICE, Set.of(SecurityRole.VIEWER), Set.of());
        var resource = new ProtectedResource("annotation", "ann-1", TENANT_A, null, null, null, null, null);

        var result = auth.authorize(ctx, Permission.ANNOTATION_EDIT, resource);

        assertFalse(result.allowed());
        assertEquals("Missing permission " + Permission.ANNOTATION_EDIT, result.reason());
    }

    @Test
    void allowsWhenPermissionPresent() {
        var auth = new DefaultAuthorizationService();
        var ctx = context(TENANT_A, USER_ALICE, Set.of(SecurityRole.REVIEWER_ANNOTATOR), Set.of(Permission.ANNOTATION_EDIT));
        var resource = new ProtectedResource("annotation", "ann-1", TENANT_A, null, null, null, null, null);

        var result = auth.authorize(ctx, Permission.ANNOTATION_EDIT, resource);

        assertTrue(result.allowed());
    }

    @Test
    void deniesWhenContextIsNull() {
        var auth = new DefaultAuthorizationService();
        var resource = new ProtectedResource("annotation", "ann-1", TENANT_A, null, null, null, null, null);

        var result = auth.authorize(null, Permission.ANNOTATION_VIEW, resource);

        assertFalse(result.allowed());
        assertEquals("Missing access context", result.reason());
    }

    @Test
    void deniesWhenResourceIsNull() {
        var auth = new DefaultAuthorizationService();
        var ctx = context(TENANT_A, USER_ALICE, Set.of(SecurityRole.TENANT_ADMIN), Set.of(Permission.ANNOTATION_VIEW));

        var result = auth.authorize(ctx, Permission.ANNOTATION_VIEW, null);

        assertFalse(result.allowed());
        assertEquals("Missing protected resource", result.reason());
    }

    // ─── Domain Scope Tests ─────────────────────────────────────────

    @Test
    void deniesWhenDomainScopeMismatch() {
        var auth = new DefaultAuthorizationService();
        var ctx = new AccessControlContext(TENANT_A, USER_ALICE, Set.of(SecurityRole.DOMAIN_MANAGER),
                Set.of(Permission.ANNOTATION_VIEW), Set.of(DomainType.ACCOUNTING), null, null, AccessPurpose.REVIEW);
        var resource = new ProtectedResource("annotation", "ann-1", TENANT_A, DomainType.HR,
                null, null, null, null);

        var result = auth.authorize(ctx, Permission.ANNOTATION_VIEW, resource);

        assertFalse(result.allowed());
        assertEquals("Domain scope mismatch", result.reason());
    }

    @Test
    void allowsWhenDomainScopeMatches() {
        var auth = new DefaultAuthorizationService();
        var ctx = new AccessControlContext(TENANT_A, USER_ALICE, Set.of(SecurityRole.DOMAIN_MANAGER),
                Set.of(Permission.ANNOTATION_VIEW), Set.of(DomainType.ACCOUNTING), null, null, AccessPurpose.REVIEW);
        var resource = new ProtectedResource("annotation", "ann-1", TENANT_A, DomainType.ACCOUNTING,
                null, null, null, null);

        var result = auth.authorize(ctx, Permission.ANNOTATION_VIEW, resource);

        assertTrue(result.allowed());
    }

    // ─── Classification Scope Tests ────────────────────────────────

    @Test
    void deniesWhenClassificationTooHigh() {
        var auth = new DefaultAuthorizationService();
        var ctx = new AccessControlContext(TENANT_A, USER_ALICE, Set.of(SecurityRole.REVIEWER_ANNOTATOR),
                Set.of(Permission.ANNOTATION_VIEW), null, null, Set.of(DataClassification.INTERNAL), AccessPurpose.REVIEW);
        var resource = new ProtectedResource("annotation", "ann-1", TENANT_A, null, null, null, null, DataClassification.RESTRICTED);

        var result = auth.authorize(ctx, Permission.ANNOTATION_VIEW, resource);

        assertFalse(result.allowed());
        assertEquals("Classification scope mismatch", result.reason());
    }

    // ─── Reviewer Assignment Tests ──────────────────────────────────

    @Test
    void deniesWhenReviewerNotAssigned() {
        var auth = new DefaultAuthorizationService();
        var ctx = new AccessControlContext(TENANT_A, USER_ALICE, Set.of(SecurityRole.REVIEWER_ANNOTATOR),
                Set.of(Permission.REVIEW_PERFORM), null, Set.of("case-1"), null, AccessPurpose.REVIEW);
        var resource = new ProtectedResource("annotation", "ann-1", TENANT_A, null, "case-2", null, null, null);

        var result = auth.authorize(ctx, Permission.REVIEW_PERFORM, resource);

        assertFalse(result.allowed());
        assertEquals("Assignment case mismatch", result.reason());
    }

    @Test
    void allowsWhenReviewerIsAssignedToCase() {
        var auth = new DefaultAuthorizationService();
        var ctx = new AccessControlContext(TENANT_A, USER_ALICE, Set.of(SecurityRole.REVIEWER_ANNOTATOR),
                Set.of(Permission.REVIEW_PERFORM), null, Set.of("case-1"), null, AccessPurpose.REVIEW);
        var resource = new ProtectedResource("annotation", "ann-1", TENANT_A, null, "case-1", null, null, null);

        var result = auth.authorize(ctx, Permission.REVIEW_PERFORM, resource);

        assertTrue(result.allowed());
    }

    @Test
    void deniesWhenUserNotAssignedToTask() {
        var auth = new DefaultAuthorizationService();
        var ctx = new AccessControlContext(TENANT_A, USER_ALICE, Set.of(SecurityRole.REVIEWER_ANNOTATOR),
                Set.of(Permission.REVIEW_PERFORM), null, Set.of(), null, AccessPurpose.REVIEW);
        var resource = new ProtectedResource("annotation", "ann-1", TENANT_A, null, null, null, "bob", null);

        var result = auth.authorize(ctx, Permission.REVIEW_PERFORM, resource);

        assertFalse(result.allowed());
        assertEquals("Assignment user mismatch", result.reason());
    }

    // ─── Redaction Service Tests ────────────────────────────────────

    @Test
    void redactsWhenMissingRevealPermission() {
        var redaction = new DefaultRedactionService();
        var ctx = context(TENANT_A, USER_ALICE, Set.of(SecurityRole.VIEWER), Set.of());
        var policy = new RedactionPolicy(Set.of(DataClassification.CONFIDENTIAL), Set.of(Permission.EVIDENCE_VIEW), null);

        var result = redaction.redact("sensitive content", ctx, policy);

        assertEquals("[REDACTED]", result);
    }

    @Test
    void revealsWhenHasPermission() {
        var redaction = new DefaultRedactionService();
        var ctx = context(TENANT_A, USER_ALICE, Set.of(SecurityRole.AUDITOR), Set.of(Permission.EVIDENCE_VIEW));
        var policy = new RedactionPolicy(Set.of(DataClassification.CONFIDENTIAL), Set.of(Permission.EVIDENCE_VIEW), null);

        var result = redaction.redact("sensitive content", ctx, policy);

        assertEquals("sensitive content", result);
    }

    @Test
    void usesCustomReplacementWhenProvided() {
        var redaction = new DefaultRedactionService();
        var ctx = context(TENANT_A, USER_ALICE, Set.of(SecurityRole.VIEWER), Set.of());
        var policy = new RedactionPolicy(Set.of(DataClassification.CONFIDENTIAL), Set.of(Permission.EVIDENCE_VIEW), "***");

        var result = redaction.redact("sensitive content", ctx, policy);

        assertEquals("***", result);
    }

    @Test
    void returnsNullForNullValue() {
        var redaction = new DefaultRedactionService();
        var ctx = context(TENANT_A, USER_ALICE, Set.of(SecurityRole.VIEWER), Set.of());
        var policy = new RedactionPolicy(Set.of(DataClassification.CONFIDENTIAL), Set.of(Permission.EVIDENCE_VIEW), null);

        var result = redaction.redact(null, ctx, policy);

        assertEquals(null, result);
    }

    @Test
    void returnsEmptyForBlankValue() {
        var redaction = new DefaultRedactionService();
        var ctx = context(TENANT_A, USER_ALICE, Set.of(SecurityRole.VIEWER), Set.of());
        var policy = new RedactionPolicy(Set.of(DataClassification.CONFIDENTIAL), Set.of(Permission.EVIDENCE_VIEW), null);

        var result = redaction.redact("   ", ctx, policy);

        assertEquals("   ", result);
    }

    @Test
    void redactsWhenPolicyIsNull() {
        var redaction = new DefaultRedactionService();
        var ctx = context(TENANT_A, USER_ALICE, Set.of(SecurityRole.PLATFORM_ADMIN), Set.of(Permission.EVIDENCE_VIEW));

        var result = redaction.redact("sensitive content", ctx, null);

        assertEquals("[REDACTED]", result);
    }

    // ─── Break-Glass Service Lifecycle Tests ────────────────────────

    @Test
    void breakGlassRequestCreateAndFindActive() {
        var repo = new InMemoryBreakGlassGrantRepository();
        var ledger = new CapturingEvidenceLedger();
        var audit = new CapturingAuditPublisher();
        var service = new DefaultBreakGlassService(repo, ledger, audit);

        service.request(TENANT_A, USER_ALICE, "Need access for review",
                Set.of("EVIDENCE_VIEW", "SOURCE_VIEW"), Instant.now().plusSeconds(3600), ADMIN_USER);

        var active = service.findActiveGrant(TENANT_A, USER_ALICE);
        assertTrue(active.isPresent());
        assertEquals(TENANT_A, active.get().tenantId());
        assertEquals(USER_ALICE, active.get().userId());
        assertEquals(2, active.get().permissions().size());
        assertTrue(active.get().permissions().contains(Permission.EVIDENCE_VIEW));
        assertTrue(active.get().permissions().contains(Permission.SOURCE_VIEW));
        assertEquals(1, ledger.events.size());
        assertEquals("BREAK_GLASS_REQUESTED", ledger.events.getFirst().eventType());
    }

    @Test
    void breakGlassApproveSetsApprover() {
        var repo = new InMemoryBreakGlassGrantRepository();
        var ledger = new CapturingEvidenceLedger();
        var audit = new CapturingAuditPublisher();
        var service = new DefaultBreakGlassService(repo, ledger, audit);

        var grant = service.request(TENANT_A, USER_ALICE, "Need access",
                Set.of("EVIDENCE_VIEW"), Instant.now().plusSeconds(3600), USER_ALICE);
        var approved = service.approve(grant.grantId(), TENANT_A, ADMIN_USER);

        assertEquals(ADMIN_USER, approved.approvedBy());
        assertEquals(2, ledger.events.size());
        assertEquals("BREAK_GLASS_APPROVED", ledger.events.get(1).eventType());
    }

    @Test
    void breakGlassDenyExpiresGrant() {
        var repo = new InMemoryBreakGlassGrantRepository();
        var ledger = new CapturingEvidenceLedger();
        var audit = new CapturingAuditPublisher();
        var service = new DefaultBreakGlassService(repo, ledger, audit);

        var grant = service.request(TENANT_A, USER_ALICE, "Need access",
                Set.of("EVIDENCE_VIEW"), Instant.now().plusSeconds(3600), USER_ALICE);
        service.deny(grant.grantId(), TENANT_A, ADMIN_USER, "Not authorized");

        var active = service.findActiveGrant(TENANT_A, USER_ALICE);
        assertTrue(active.isEmpty());
        assertEquals(2, ledger.events.size());
        assertEquals("BREAK_GLASS_DENIED", ledger.events.get(1).eventType());
    }

    @Test
    void breakGlassRevokeExpiresGrant() {
        var repo = new InMemoryBreakGlassGrantRepository();
        var ledger = new CapturingEvidenceLedger();
        var audit = new CapturingAuditPublisher();
        var service = new DefaultBreakGlassService(repo, ledger, audit);

        var grant = service.request(TENANT_A, USER_ALICE, "Need access",
                Set.of("EVIDENCE_VIEW"), Instant.now().plusSeconds(3600), USER_ALICE);
        service.approve(grant.grantId(), TENANT_A, ADMIN_USER);
        var revoked = service.revoke(grant.grantId(), TENANT_A, ADMIN_USER, "No longer needed");

        assertTrue(revoked.expiresAt().isBefore(Instant.now().plusSeconds(1)));
        var active = service.findActiveGrant(TENANT_A, USER_ALICE);
        assertTrue(active.isEmpty());
    }

    @Test
    void breakGlassThrowsWhenDuplicateActiveGrant() {
        var repo = new InMemoryBreakGlassGrantRepository();
        var ledger = new CapturingEvidenceLedger();
        var audit = new CapturingAuditPublisher();
        var service = new DefaultBreakGlassService(repo, ledger, audit);

        service.request(TENANT_A, USER_ALICE, "First request",
                Set.of("EVIDENCE_VIEW"), Instant.now().plusSeconds(3600), USER_ALICE);

        assertThrows(IllegalStateException.class, () ->
                service.request(TENANT_A, USER_ALICE, "Second request",
                        Set.of("EVIDENCE_VIEW"), Instant.now().plusSeconds(3600), USER_ALICE));
    }

    @Test
    void breakGlassThrowsWhenApprovingAlreadyProcessed() {
        var repo = new InMemoryBreakGlassGrantRepository();
        var ledger = new CapturingEvidenceLedger();
        var audit = new CapturingAuditPublisher();
        var service = new DefaultBreakGlassService(repo, ledger, audit);

        var grant = service.request(TENANT_A, USER_ALICE, "Need access",
                Set.of("EVIDENCE_VIEW"), Instant.now().plusSeconds(3600), USER_ALICE);
        service.deny(grant.grantId(), TENANT_A, ADMIN_USER, "Denied");

        assertThrows(IllegalStateException.class, () ->
                service.approve(grant.grantId(), TENANT_A, ADMIN_USER));
    }

    // ─── Helpers ────────────────────────────────────────────────────

    private static AccessControlContext context(String tenantId, String userId,
                                                 Set<SecurityRole> roles, Set<Permission> permissions) {
        return new AccessControlContext(tenantId, userId, roles, permissions,
                null, null, null, AccessPurpose.ADMINISTRATION);
    }

    private static class CapturingEvidenceLedger implements EvidenceLedger {
        final List<EvidenceEvent> events = new ArrayList<>();

        @Override
        public void append(EvidenceEvent event) {
            events.add(event);
        }
    }

    private static class CapturingAuditPublisher implements SecurityAuditEventPublisher {
        final List<ai.datalithix.kanon.common.security.SecurityAuditEvent> events = new ArrayList<>();

        @Override
        public void publish(ai.datalithix.kanon.common.security.SecurityAuditEvent event) {
            events.add(event);
        }
    }
}
