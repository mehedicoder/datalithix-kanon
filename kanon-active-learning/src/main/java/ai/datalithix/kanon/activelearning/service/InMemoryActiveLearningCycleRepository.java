package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningCycle;
import ai.datalithix.kanon.activelearning.model.CycleStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryActiveLearningCycleRepository implements ActiveLearningCycleRepository {
    private final Map<String, ActiveLearningCycle> cycles = new ConcurrentHashMap<>();

    @Override
    public ActiveLearningCycle save(ActiveLearningCycle cycle) {
        cycles.put(cycle.cycleId(), cycle);
        return cycle;
    }

    @Override
    public Optional<ActiveLearningCycle> findById(String tenantId, String cycleId) {
        return Optional.ofNullable(cycles.get(cycleId))
                .filter(c -> c.tenantId().equals(tenantId));
    }

    @Override
    public List<ActiveLearningCycle> findByTenant(String tenantId) {
        return cycles.values().stream()
                .filter(c -> c.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(c -> c.startedAt(), Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public List<ActiveLearningCycle> findByModel(String tenantId, String modelEntryId) {
        return cycles.values().stream()
                .filter(c -> c.tenantId().equals(tenantId))
                .filter(c -> c.modelEntryId().equals(modelEntryId))
                .sorted(Comparator.comparing(c -> c.startedAt(), Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public List<ActiveLearningCycle> findByStatus(String tenantId, CycleStatus status) {
        return cycles.values().stream()
                .filter(c -> c.tenantId().equals(tenantId))
                .filter(c -> c.status() == status)
                .sorted(Comparator.comparing(c -> c.startedAt(), Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public List<ActiveLearningCycle> findAllByStatus(CycleStatus status) {
        return cycles.values().stream()
                .filter(c -> c.status() == status)
                .sorted(Comparator.comparing(c -> c.startedAt(), Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public Optional<ActiveLearningCycle> findLatestByModel(String tenantId, String modelEntryId) {
        return findByModel(tenantId, modelEntryId).stream().findFirst();
    }
}
