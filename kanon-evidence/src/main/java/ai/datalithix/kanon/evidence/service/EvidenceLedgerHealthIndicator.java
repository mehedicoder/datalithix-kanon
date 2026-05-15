package ai.datalithix.kanon.evidence.service;

import ai.datalithix.kanon.common.runtime.ComponentHealth;
import ai.datalithix.kanon.common.runtime.HealthIndicator;
import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EvidenceLedgerHealthIndicator implements HealthIndicator {
    private final EvidenceLedger ledger;

    public EvidenceLedgerHealthIndicator(EvidenceLedger ledger) {
        this.ledger = ledger;
    }

    @Override
    public ComponentHealth health() {
        try {
            var event = new EvidenceEvent(
                    UUID.randomUUID().toString(), "system", "health-check",
                    "HEALTH_CHECK", ActorType.SYSTEM, "health-checker",
                    null, null, null, null, null,
                    "Health check evidence write test", Instant.now());
            ledger.append(event);
            return ComponentHealth.up("evidence-ledger", "Evidence ledger write succeeded");
        } catch (Exception e) {
            return ComponentHealth.down("evidence-ledger",
                    "Evidence ledger write failed: " + e.getMessage());
        }
    }
}
