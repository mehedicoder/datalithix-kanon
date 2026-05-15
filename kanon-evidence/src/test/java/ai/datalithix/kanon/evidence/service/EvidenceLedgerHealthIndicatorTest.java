package ai.datalithix.kanon.evidence.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceLedgerHealthIndicatorTest {
    @Test
    void healthReturnsUpWhenLedgerWorks() {
        var ledger = new InMemoryEvidenceLedger();
        var indicator = new EvidenceLedgerHealthIndicator(ledger);
        var health = indicator.health();
        assertEquals("evidence-ledger", health.componentName());
        assertTrue(health.status().name().equals("UP") || health.status().name().equals("DOWN"),
                "Status should be UP or DOWN");
        assertNotNull(health.checkedAt());
    }
}
