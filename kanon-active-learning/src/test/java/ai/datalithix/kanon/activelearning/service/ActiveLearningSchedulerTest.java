package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningConfig;
import ai.datalithix.kanon.activelearning.model.ActiveLearningCycle;
import ai.datalithix.kanon.activelearning.model.CycleStatus;
import ai.datalithix.kanon.activelearning.model.SelectionStrategyType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActiveLearningSchedulerTest {
    private final InMemoryActiveLearningCycleRepository cycleRepo = new InMemoryActiveLearningCycleRepository();
    private final CapturingOrchestrator orchestrator = new CapturingOrchestrator();
    private final ActiveLearningScheduler scheduler = new ActiveLearningScheduler(cycleRepo, orchestrator);

    @Test
    void autoTriggersNewCycleForPromotedCyclesWithAutoTrigger() {
        var cycle = createCycle("cycle-1", CycleStatus.PROMOTED, true);
        cycleRepo.save(cycle);

        scheduler.autoTriggerCompletedCycles();

        assertEquals(1, orchestrator.startedCycles.size());
    }

    @Test
    void doesNotTriggerForCyclesWithoutAutoTrigger() {
        var cycle = createCycle("cycle-1", CycleStatus.PROMOTED, false);
        cycleRepo.save(cycle);

        scheduler.autoTriggerCompletedCycles();

        assertTrue(orchestrator.startedCycles.isEmpty());
    }

    @Test
    void doesNotTriggerWhenOrchestratorBlocks() {
        var cycle = createCycle("cycle-1", CycleStatus.PROMOTED, true);
        cycleRepo.save(cycle);
        orchestrator.canTriggerResult = false;

        scheduler.autoTriggerCompletedCycles();

        assertTrue(orchestrator.startedCycles.isEmpty());
    }

    @Test
    void handlesRejectedAndFailedCycles() {
        cycleRepo.save(createCycle("c1", CycleStatus.REJECTED, true));
        cycleRepo.save(createCycle("c2", CycleStatus.FAILED, true));

        scheduler.autoTriggerCompletedCycles();

        assertEquals(2, orchestrator.startedCycles.size());
    }

    @Test
    void handlesExceptionDuringStartCycle() {
        var cycle = createCycle("cycle-1", CycleStatus.PROMOTED, true);
        cycleRepo.save(cycle);
        orchestrator.throwOnStart = true;

        assertDoesNotThrow(() -> scheduler.autoTriggerCompletedCycles());
    }

    private static ActiveLearningCycle createCycle(String id, CycleStatus status, boolean autoTrigger) {
        var audit = new AuditMetadata(Instant.now(), "creator", Instant.now(), "creator", 1);
        return new ActiveLearningCycle(id, "tenant-1", "me-1", "mv-1",
                SelectionStrategyType.UNCERTAINTY_SAMPLING, status, 10, 5,
                "dsv-1", "dsv-2", "tj-1", "er-1", null,
                Instant.now(), status == CycleStatus.PROMOTED ? Instant.now() : null,
                null, autoTrigger, List.of(), audit);
    }

    private static class CapturingOrchestrator extends ActiveLearningOrchestrator {
        final List<String> startedCycles = new ArrayList<>();
        boolean canTriggerResult = true;
        boolean throwOnStart;

        CapturingOrchestrator() {
            super(null, null, java.util.Collections.emptyList());
        }

        @Override
        public ActiveLearningCycle startCycle(String tenantId, String modelEntryId, String modelVersionId,
                                               String sourceDatasetVersionId, ActiveLearningConfig config,
                                               List<RecordSelector.RecordCandidate> candidates, String actorId) {
            if (throwOnStart) throw new RuntimeException("Simulated failure");
            startedCycles.add(modelEntryId);
            var audit = new AuditMetadata(Instant.now(), actorId, Instant.now(), actorId, 1);
            return new ActiveLearningCycle("new-" + modelEntryId, tenantId, modelEntryId, modelVersionId,
                    SelectionStrategyType.UNCERTAINTY_SAMPLING, CycleStatus.AWAITING_REVIEW, 0, 0,
                    sourceDatasetVersionId, null, null, null, null,
                    Instant.now(), null, null, true, List.of(), audit);
        }

        @Override
        public boolean canTriggerCycle(String tenantId, String modelEntryId) {
            return canTriggerResult;
        }
    }
}
