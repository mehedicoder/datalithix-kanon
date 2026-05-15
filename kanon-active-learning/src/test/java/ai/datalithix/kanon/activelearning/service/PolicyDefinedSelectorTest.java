package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningConfig;
import ai.datalithix.kanon.activelearning.model.SelectionStrategyType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PolicyDefinedSelectorTest {
    private final PolicyDefinedSelector selector = new PolicyDefinedSelector();

    @Test
    void supportsPolicyDefined() {
        assertEquals(SelectionStrategyType.POLICY_DEFINED, selector.supportedStrategy());
    }

    @Test
    void selectsByConfidenceDefaultPolicy() {
        var config = new ActiveLearningConfig(SelectionStrategyType.POLICY_DEFINED, 0.7, 3, 100,
                false, null, true, Map.of());
        var candidates = List.of(
                new RecordSelector.RecordCandidate("r1", 0.95, "A", "A", new double[]{}, new double[]{}),
                new RecordSelector.RecordCandidate("r2", 0.45, "B", "B", new double[]{}, new double[]{}),
                new RecordSelector.RecordCandidate("r3", 0.60, "A", "B", new double[]{}, new double[]{})
        );
        var selected = selector.selectRecords(candidates, config);
        assertEquals(2, selected.size());
    }

    @Test
    void selectsByErrorPolicy() {
        var config = new ActiveLearningConfig(SelectionStrategyType.POLICY_DEFINED, 0.0, 10, 100,
                false, null, true, Map.of("policyType", "error"));
        var candidates = List.of(
                new RecordSelector.RecordCandidate("r1", 0.95, "A", "A", new double[]{}, new double[]{}),
                new RecordSelector.RecordCandidate("r2", 0.80, "B", "C", new double[]{}, new double[]{}),
                new RecordSelector.RecordCandidate("r3", 0.60, "A", "B", new double[]{}, new double[]{})
        );
        var selected = selector.selectRecords(candidates, config);
        assertEquals(2, selected.size());
        assertTrue(selected.stream().allMatch(s -> s.selectionReason().contains("error")));
    }

    @Test
    void selectsByLabelPolicy() {
        var config = new ActiveLearningConfig(SelectionStrategyType.POLICY_DEFINED, 0.0, 10, 100,
                false, null, true, Map.of("policyType", "label", "targetLabel", "B"));
        var candidates = List.of(
                new RecordSelector.RecordCandidate("r1", 0.95, "A", "A", new double[]{}, new double[]{}),
                new RecordSelector.RecordCandidate("r2", 0.80, "B", "B", new double[]{}, new double[]{}),
                new RecordSelector.RecordCandidate("r3", 0.60, "C", "C", new double[]{}, new double[]{})
        );
        var selected = selector.selectRecords(candidates, config);
        assertEquals(1, selected.size());
    }
}
