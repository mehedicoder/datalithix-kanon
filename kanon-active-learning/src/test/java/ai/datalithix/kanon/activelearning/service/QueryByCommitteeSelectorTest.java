package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningConfig;
import ai.datalithix.kanon.activelearning.model.SelectedRecord;
import ai.datalithix.kanon.activelearning.model.SelectionStrategyType;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryByCommitteeSelectorTest {
    private final QueryByCommitteeSelector selector = new QueryByCommitteeSelector();

    @Test
    void supportsQueryByCommittee() {
        assertEquals(SelectionStrategyType.QUERY_BY_COMMITTEE, selector.supportedStrategy());
    }

    @Test
    void selectsRecordsWithHighestVoteEntropy() {
        var config = new ActiveLearningConfig(SelectionStrategyType.QUERY_BY_COMMITTEE, 0.0, 2, 100,
                false, null, true, null);
        var candidates = List.of(
                new RecordSelector.RecordCandidate("r1", 0.9, "A", "A", new double[]{}, new double[]{0.95, 0.05}),
                new RecordSelector.RecordCandidate("r2", 0.6, "B", "B", new double[]{}, new double[]{0.50, 0.50}),
                new RecordSelector.RecordCandidate("r3", 0.8, "A", "A", new double[]{}, new double[]{0.80, 0.20})
        );
        List<SelectedRecord> selected = selector.selectRecords(candidates, config);
        assertEquals(2, selected.size());
        assertEquals("r2", selected.get(0).recordId());
    }

    @Test
    void skipsCandidatesWithoutPredictionProbs() {
        var config = new ActiveLearningConfig(SelectionStrategyType.QUERY_BY_COMMITTEE, 0.0, 10, 100,
                false, null, true, null);
        var candidates = List.of(
                new RecordSelector.RecordCandidate("r1", 0.9, "A", "A", new double[]{}, null)
        );
        assertTrue(selector.selectRecords(candidates, config).isEmpty());
    }
}
