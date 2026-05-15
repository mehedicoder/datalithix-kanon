package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningConfig;
import ai.datalithix.kanon.activelearning.model.SelectedRecord;
import ai.datalithix.kanon.activelearning.model.SelectionStrategyType;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UncertaintySamplingSelectorTest {
    private final UncertaintySamplingSelector selector = new UncertaintySamplingSelector();

    @Test
    void supportsUncertaintySampling() {
        assertEquals(SelectionStrategyType.UNCERTAINTY_SAMPLING, selector.supportedStrategy());
    }

    @Test
    void selectsRecordsBelowConfidenceThreshold() {
        var config = new ActiveLearningConfig(SelectionStrategyType.UNCERTAINTY_SAMPLING, 0.8, 10, 100,
                false, null, true, null);
        var candidates = List.of(
                new RecordSelector.RecordCandidate("r1", 0.95, "A", "A", new double[]{}, new double[]{}),
                new RecordSelector.RecordCandidate("r2", 0.45, "B", "B", new double[]{}, new double[]{}),
                new RecordSelector.RecordCandidate("r3", 0.60, "A", "B", new double[]{}, new double[]{})
        );
        List<SelectedRecord> selected = selector.selectRecords(candidates, config);
        assertEquals(2, selected.size());
        assertTrue(selected.stream().allMatch(s -> s.score() > 0));
    }

    @Test
    void respectsMaxRecordsPerCycle() {
        var config = new ActiveLearningConfig(SelectionStrategyType.UNCERTAINTY_SAMPLING, 0.9, 2, 100,
                false, null, true, null);
        var candidates = List.of(
                new RecordSelector.RecordCandidate("r1", 0.1, "A", "A", new double[]{}, new double[]{}),
                new RecordSelector.RecordCandidate("r2", 0.2, "B", "B", new double[]{}, new double[]{}),
                new RecordSelector.RecordCandidate("r3", 0.3, "A", "B", new double[]{}, new double[]{})
        );
        List<SelectedRecord> selected = selector.selectRecords(candidates, config);
        assertEquals(2, selected.size());
    }

    @Test
    void returnsEmptyWhenAllAboveThreshold() {
        var config = new ActiveLearningConfig(SelectionStrategyType.UNCERTAINTY_SAMPLING, 0.3, 10, 100,
                false, null, true, null);
        var candidates = List.of(
                new RecordSelector.RecordCandidate("r1", 0.95, "A", "A", new double[]{}, new double[]{})
        );
        List<SelectedRecord> selected = selector.selectRecords(candidates, config);
        assertTrue(selected.isEmpty());
    }
}
