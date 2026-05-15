package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningCycle;
import ai.datalithix.kanon.activelearning.model.CycleStatus;
import java.util.List;
import java.util.Optional;

public interface ActiveLearningCycleRepository {
    ActiveLearningCycle save(ActiveLearningCycle cycle);
    Optional<ActiveLearningCycle> findById(String tenantId, String cycleId);
    List<ActiveLearningCycle> findByTenant(String tenantId);
    List<ActiveLearningCycle> findByModel(String tenantId, String modelEntryId);
    List<ActiveLearningCycle> findByStatus(String tenantId, CycleStatus status);
    List<ActiveLearningCycle> findAllByStatus(CycleStatus status);
    Optional<ActiveLearningCycle> findLatestByModel(String tenantId, String modelEntryId);
}
