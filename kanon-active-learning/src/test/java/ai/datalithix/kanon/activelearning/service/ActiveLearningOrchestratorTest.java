package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningConfig;
import ai.datalithix.kanon.activelearning.model.ActiveLearningCycle;
import ai.datalithix.kanon.activelearning.model.CycleStatus;
import ai.datalithix.kanon.activelearning.model.SelectionStrategyType;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActiveLearningOrchestratorTest {
    private final InMemoryActiveLearningCycleRepository repository = new InMemoryActiveLearningCycleRepository();
    private final CapturingEvidenceLedger ledger = new CapturingEvidenceLedger();
    private final ActiveLearningOrchestrator orchestrator = new ActiveLearningOrchestrator(
            repository, ledger,
            List.of(new UncertaintySamplingSelector(), new DiversitySamplingSelector(),
                    new QueryByCommitteeSelector(), new PolicyDefinedSelector())
    );

    @Test
    void startsCycleInAwaitingReviewStatus() {
        var config = new ActiveLearningConfig(SelectionStrategyType.UNCERTAINTY_SAMPLING, 0.8, 10, 100,
                false, null, true, null);
        var candidates = List.of(
                new RecordSelector.RecordCandidate("r1", 0.95, "A", "A", new double[]{}, new double[]{}),
                new RecordSelector.RecordCandidate("r2", 0.45, "B", "B", new double[]{}, new double[]{})
        );
        ActiveLearningCycle cycle = orchestrator.startCycle(
                "tenant-1", "model-1", "mv-1", "dsv-1", config, candidates, "user-1");
        assertEquals("tenant-1", cycle.tenantId());
        assertEquals(CycleStatus.AWAITING_REVIEW, cycle.status());
        assertNotNull(cycle.cycleId());
        assertNotNull(cycle.startedAt());
    }

    @Test
    void transitionsThroughValidStatuses() {
        var config = new ActiveLearningConfig(SelectionStrategyType.UNCERTAINTY_SAMPLING, 0.9, 10, 100,
                false, null, true, null);
        var candidates = List.of(
                new RecordSelector.RecordCandidate("r1", 0.3, "A", "A", new double[]{}, new double[]{})
        );
        ActiveLearningCycle cycle = orchestrator.startCycle(
                "tenant-1", "model-1", "mv-1", "dsv-1", config, candidates, "user-1");

        cycle = orchestrator.updateStatus("tenant-1", cycle.cycleId(), CycleStatus.DATASET_UPDATED, "dsv-2", "user-1");
        assertEquals(CycleStatus.DATASET_UPDATED, cycle.status());
        assertEquals("dsv-2", cycle.targetDatasetVersionId());

        cycle = orchestrator.updateStatus("tenant-1", cycle.cycleId(), CycleStatus.RETRAINING, null, "user-1");
        assertEquals(CycleStatus.RETRAINING, cycle.status());

        cycle = orchestrator.updateStatus("tenant-1", cycle.cycleId(), CycleStatus.EVALUATING, null, "user-1");
        assertEquals(CycleStatus.EVALUATING, cycle.status());

        cycle = orchestrator.updateStatus("tenant-1", cycle.cycleId(), CycleStatus.PROMOTED, null, "user-1");
        assertEquals(CycleStatus.PROMOTED, cycle.status());
        assertNotNull(cycle.completedAt());
    }

    @Test
    void rejectsInvalidTransition() {
        var config = new ActiveLearningConfig(SelectionStrategyType.UNCERTAINTY_SAMPLING, 0.9, 10, 100,
                false, null, true, null);
        var candidates = List.of(
                new RecordSelector.RecordCandidate("r1", 0.3, "A", "A", new double[]{}, new double[]{})
        );
        ActiveLearningCycle cycle = orchestrator.startCycle(
                "tenant-1", "model-1", "mv-1", "dsv-1", config, candidates, "user-1");

        assertThrows(IllegalStateException.class,
                () -> orchestrator.updateStatus("tenant-1", cycle.cycleId(), CycleStatus.PROMOTED, null, "user-1"));
    }

    @Test
    void rejectsCycleWhenInProgress() {
        var config = new ActiveLearningConfig(SelectionStrategyType.UNCERTAINTY_SAMPLING, 0.9, 10, 100,
                false, null, true, null);
        var candidates = List.of(
                new RecordSelector.RecordCandidate("r1", 0.3, "A", "A", new double[]{}, new double[]{})
        );
        orchestrator.startCycle("tenant-1", "model-1", "mv-1", "dsv-1", config, candidates, "user-1");

        assertFalse(orchestrator.canTriggerCycle("tenant-1", "model-1"));
    }

    @Test
    void allowsCycleWhenPreviousCompleted() {
        var config = new ActiveLearningConfig(SelectionStrategyType.UNCERTAINTY_SAMPLING, 0.9, 10, 100,
                false, null, true, null);
        var candidates = List.of(
                new RecordSelector.RecordCandidate("r1", 0.3, "A", "A", new double[]{}, new double[]{})
        );
        ActiveLearningCycle cycle = orchestrator.startCycle(
                "tenant-1", "model-1", "mv-1", "dsv-1", config, candidates, "user-1");
        orchestrator.cancelCycle("tenant-1", cycle.cycleId(), "user-1");

        assertTrue(orchestrator.canTriggerCycle("tenant-1", "model-1"));
    }

    @Test
    void recordsReviewProgress() {
        var config = new ActiveLearningConfig(SelectionStrategyType.UNCERTAINTY_SAMPLING, 0.9, 10, 100,
                false, null, true, null);
        var candidates = List.of(
                new RecordSelector.RecordCandidate("r1", 0.3, "A", "A", new double[]{}, new double[]{})
        );
        ActiveLearningCycle cycle = orchestrator.startCycle(
                "tenant-1", "model-1", "mv-1", "dsv-1", config, candidates, "user-1");

        cycle = orchestrator.recordReviewProgress("tenant-1", cycle.cycleId(), 1, "reviewer-1");
        assertEquals(CycleStatus.DATASET_UPDATED, cycle.status());
        assertEquals(1, cycle.passedReviewCount());
    }

    @Test
    void createsEvidenceOnCycleStart() {
        var config = new ActiveLearningConfig(SelectionStrategyType.UNCERTAINTY_SAMPLING, 0.8, 10, 100,
                false, null, true, null);
        var candidates = List.of(
                new RecordSelector.RecordCandidate("r1", 0.3, "A", "A", new double[]{}, new double[]{})
        );
        orchestrator.startCycle("tenant-1", "model-1", "mv-1", "dsv-1", config, candidates, "user-1");
        assertEquals(1, ledger.events().size());
        assertTrue(((EvidenceEvent) ledger.events().getFirst()).eventType().contains("ACTIVE_LEARNING"));
    }

    private static class CapturingEvidenceLedger implements EvidenceLedger {
        private final List<Object> events = new java.util.ArrayList<>();
        @Override public void append(EvidenceEvent event) { events.add(event); }
        List<Object> events() { return events; }
    }
}
