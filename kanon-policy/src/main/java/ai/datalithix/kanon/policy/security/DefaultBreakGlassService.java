package ai.datalithix.kanon.policy.security;

import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.PageSpec;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.model.SortDirection;
import ai.datalithix.kanon.common.security.BreakGlassGrant;
import ai.datalithix.kanon.common.security.Permission;
import ai.datalithix.kanon.common.security.SecurityAuditEvent;
import ai.datalithix.kanon.common.security.SecurityEventType;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DefaultBreakGlassService implements BreakGlassService {
    private final BreakGlassGrantRepository grantRepository;
    private final EvidenceLedger evidenceLedger;
    private final SecurityAuditEventPublisher auditPublisher;

    public DefaultBreakGlassService(
            BreakGlassGrantRepository grantRepository,
            EvidenceLedger evidenceLedger,
            SecurityAuditEventPublisher auditPublisher
    ) {
        this.grantRepository = grantRepository;
        this.evidenceLedger = evidenceLedger;
        this.auditPublisher = auditPublisher;
    }

    @Override
    public BreakGlassGrant request(String tenantId, String userId, String reason,
                                    Set<String> requestedPermissionKeys, Instant expiresAt, String actorId) {
        if (grantRepository.findActiveGrant(tenantId, userId, Instant.now()).isPresent()) {
            throw new IllegalStateException("User already has an active break-glass grant");
        }
        Instant now = Instant.now();
        String grantId = "bg-" + UUID.randomUUID();
        Set<Permission> permissions = requestedPermissionKeys.stream()
                .map(Permission::valueOf)
                .collect(Collectors.toSet());
        var grant = new BreakGlassGrant(
                grantId, tenantId, userId, reason, null, now, expiresAt,
                permissions, null,
                new AuditMetadata(now, actorId, now, actorId, 1)
        );
        BreakGlassGrant saved = grantRepository.save(grant);
        evidenceLedger.append(new EvidenceEvent(
                UUID.randomUUID().toString(), tenantId, grantId,
                "BREAK_GLASS_REQUESTED", ActorType.HUMAN, actorId,
                null, null, null,
                Map.of(), Map.of("userId", userId, "permissions", requestedPermissionKeys.toString()),
                reason, now
        ));
        auditPublisher.publish(new SecurityAuditEvent(
                UUID.randomUUID().toString(), tenantId, actorId,
                SecurityEventType.BREAK_GLASS_REQUESTED,
                null, "PENDING", reason, now, Map.of("grantId", grantId, "targetUserId", userId)
        ));
        return saved;
    }

    @Override
    public BreakGlassGrant approve(String grantId, String tenantId, String approvedBy) {
        var grant = findById(tenantId, grantId)
                .orElseThrow(() -> new IllegalArgumentException("Grant not found: " + grantId));
        if (grant.approvedBy() != null) {
            throw new IllegalStateException("Grant already " + (grant.expiresAt().isBefore(Instant.now()) ? "expired" : "processed"));
        }
        Instant now = Instant.now();
        var approved = new BreakGlassGrant(
                grant.grantId(), grant.tenantId(), grant.userId(), grant.reason(),
                approvedBy, grant.startsAt(), grant.expiresAt(),
                grant.permissions(), grant.evidenceEventId(),
                new AuditMetadata(grant.audit().createdAt(), grant.audit().createdBy(), now, approvedBy,
                        grant.audit().version() + 1)
        );
        BreakGlassGrant saved = grantRepository.save(approved);
        evidenceLedger.append(new EvidenceEvent(
                UUID.randomUUID().toString(), tenantId, grantId,
                "BREAK_GLASS_APPROVED", ActorType.HUMAN, approvedBy,
                null, null, null,
                Map.of("userId", grant.userId()), Map.of("approvedBy", approvedBy),
                "Break-glass grant approved for " + grant.userId(), now
        ));
        auditPublisher.publish(new SecurityAuditEvent(
                UUID.randomUUID().toString(), tenantId, approvedBy,
                SecurityEventType.BREAK_GLASS_APPROVED,
                null, "APPROVED", "Grant approved", now,
                Map.of("grantId", grantId, "targetUserId", grant.userId())
        ));
        return saved;
    }

    @Override
    public BreakGlassGrant deny(String grantId, String tenantId, String deniedBy, String reason) {
        var grant = findById(tenantId, grantId)
                .orElseThrow(() -> new IllegalArgumentException("Grant not found: " + grantId));
        if (grant.approvedBy() != null) {
            throw new IllegalStateException("Grant already processed");
        }
        Instant now = Instant.now();
        var denied = new BreakGlassGrant(
                grant.grantId(), grant.tenantId(), grant.userId(), grant.reason(),
                "DENIED_BY:" + deniedBy, grant.startsAt(), now,
                grant.permissions(), grant.evidenceEventId(),
                new AuditMetadata(grant.audit().createdAt(), grant.audit().createdBy(), now, deniedBy,
                        grant.audit().version() + 1)
        );
        grantRepository.save(denied);
        evidenceLedger.append(new EvidenceEvent(
                UUID.randomUUID().toString(), tenantId, grantId,
                "BREAK_GLASS_DENIED", ActorType.HUMAN, deniedBy,
                null, null, null,
                Map.of("userId", grant.userId()), Map.of("deniedBy", deniedBy),
                reason != null ? reason : "Break-glass grant denied", now
        ));
        auditPublisher.publish(new SecurityAuditEvent(
                UUID.randomUUID().toString(), tenantId, deniedBy,
                SecurityEventType.BREAK_GLASS_DENIED,
                null, "DENIED", reason, now,
                Map.of("grantId", grantId, "targetUserId", grant.userId())
        ));
        return denied;
    }

    @Override
    public BreakGlassGrant revoke(String grantId, String tenantId, String revokedBy, String reason) {
        var grant = findById(tenantId, grantId)
                .orElseThrow(() -> new IllegalArgumentException("Grant not found: " + grantId));
        if (grant.approvedBy() == null || grant.approvedBy().startsWith("DENIED_BY:")) {
            throw new IllegalStateException("Only approved grants can be revoked");
        }
        Instant now = Instant.now();
        var expired = new BreakGlassGrant(
                grant.grantId(), grant.tenantId(), grant.userId(), grant.reason(),
                grant.approvedBy(), grant.startsAt(), now,
                grant.permissions(), grant.evidenceEventId(),
                new AuditMetadata(grant.audit().createdAt(), grant.audit().createdBy(), now, revokedBy,
                        grant.audit().version() + 1)
        );
        grantRepository.save(expired);
        evidenceLedger.append(new EvidenceEvent(
                UUID.randomUUID().toString(), tenantId, grantId,
                "BREAK_GLASS_REVOKED", ActorType.HUMAN, revokedBy,
                null, null, null,
                Map.of("userId", grant.userId()), Map.of("revokedBy", revokedBy),
                reason != null ? reason : "Break-glass grant revoked", now
        ));
        return expired;
    }

    @Override
    public Optional<BreakGlassGrant> findActiveGrant(String tenantId, String userId) {
        return grantRepository.findActiveGrant(tenantId, userId, Instant.now());
    }

    @Override
    public Optional<BreakGlassGrant> findById(String tenantId, String grantId) {
        return grantRepository.findPage(new QuerySpec(tenantId, new PageSpec(0, 100, null, SortDirection.ASC), null, null))
                .items().stream()
                .filter(g -> g.grantId().equals(grantId))
                .findFirst();
    }

    @Override
    public PageResult<BreakGlassGrant> findPage(QuerySpec query) {
        return grantRepository.findPage(query);
    }
}
