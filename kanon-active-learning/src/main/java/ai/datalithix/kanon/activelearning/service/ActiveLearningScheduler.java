package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningConfig;
import ai.datalithix.kanon.activelearning.model.ActiveLearningCycle;
import ai.datalithix.kanon.activelearning.model.CycleStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ActiveLearningScheduler {
    private static final Logger log = LoggerFactory.getLogger(ActiveLearningScheduler.class);

    private final ActiveLearningCycleRepository cycleRepository;
    private final ActiveLearningOrchestrator orchestrator;

    public ActiveLearningScheduler(
            ActiveLearningCycleRepository cycleRepository,
            ActiveLearningOrchestrator orchestrator
    ) {
        this.cycleRepository = cycleRepository;
        this.orchestrator = orchestrator;
    }

    @Scheduled(fixedDelayString = "${kanon.active-learning.scheduler.interval-ms:60000}")
    public void autoTriggerCompletedCycles() {
        List<ActiveLearningCycle> terminal = new ArrayList<>();
        terminal.addAll(cycleRepository.findAllByStatus(CycleStatus.PROMOTED));
        terminal.addAll(cycleRepository.findAllByStatus(CycleStatus.REJECTED));
        terminal.addAll(cycleRepository.findAllByStatus(CycleStatus.FAILED));

        for (ActiveLearningCycle cycle : terminal) {
            if (!cycle.autoTrigger()) continue;
            if (!orchestrator.canTriggerCycle(cycle.tenantId(), cycle.modelEntryId())) continue;

            try {
                ActiveLearningConfig config = new ActiveLearningConfig(
                        cycle.strategyType(), 0.85, 100, 50,
                        true, cycle.cronExpression(), true, Map.of());
                orchestrator.startCycle(
                        cycle.tenantId(), cycle.modelEntryId(), cycle.modelVersionId(),
                        cycle.sourceDatasetVersionId(), config, List.of(), "scheduler");
                log.info("Auto-triggered new active learning cycle for model {} in tenant {}",
                        cycle.modelEntryId(), cycle.tenantId());
            } catch (Exception e) {
                log.error("Failed to auto-trigger cycle for model {} in tenant {}",
                        cycle.modelEntryId(), cycle.tenantId(), e);
            }
        }
    }
}
