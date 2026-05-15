package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningConfig;
import ai.datalithix.kanon.activelearning.model.SelectedRecord;
import ai.datalithix.kanon.activelearning.model.SelectionStrategyType;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiversitySamplingSelectorTest {
    private final DiversitySamplingSelector selector = new DiversitySamplingSelector();

    @Test
    void supportsDiversitySampling() {
        assertEquals(SelectionStrategyType.DIVERSITY_SAMPLING, selector.supportedStrategy());
    }

    @Test
    void selectsRecordsFurthestFromCentroid() {
        var config = new ActiveLearningConfig(SelectionStrategyType.DIVERSITY_SAMPLING, 0.0, 2, 100,
                false, null, true, null);
        var candidates = List.of(
                new RecordSelector.RecordCandidate("r1", 0.9, "A", "A", new double[]{0.0, 0.0}, new double[]{}),
                new RecordSelector.RecordCandidate("r2", 0.9, "B", "B", new double[]{1.0, 1.0}, new double[]{}),
                new RecordSelector.RecordCandidate("r3", 0.9, "A", "A", new double[]{0.5, 0.5}, new double[]{})
        );
        List<SelectedRecord> selected = selector.selectRecords(candidates, config);
        assertEquals(2, selected.size());
        assertTrue(selected.stream().anyMatch(s -> s.recordId().equals("r1")));
        assertTrue(selected.stream().anyMatch(s -> s.recordId().equals("r2")));
        assertTrue(selected.stream().noneMatch(s -> s.recordId().equals("r3")));
    }

    @Test
    void returnsEmptyForNoCandidates() {
        var config = new ActiveLearningConfig(SelectionStrategyType.DIVERSITY_SAMPLING, 0.0, 10, 100,
                false, null, true, null);
        assertTrue(selector.selectRecords(List.of(), config).isEmpty());
    }
}
