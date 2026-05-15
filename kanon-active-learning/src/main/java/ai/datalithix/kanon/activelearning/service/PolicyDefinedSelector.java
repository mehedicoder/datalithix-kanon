package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningConfig;
import ai.datalithix.kanon.activelearning.model.SelectedRecord;
import ai.datalithix.kanon.activelearning.model.SelectionStrategyType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PolicyDefinedSelector implements RecordSelector {
    @Override
    public SelectionStrategyType supportedStrategy() {
        return SelectionStrategyType.POLICY_DEFINED;
    }

    @Override
    public List<SelectedRecord> selectRecords(List<RecordCandidate> candidates, ActiveLearningConfig config) {
        String policyType = config.strategyParams().getOrDefault("policyType", "confidence");
        int limit = Math.min(candidates.size(), config.maxRecordsPerCycle());
        List<SelectedRecord> result = new ArrayList<>();
        List<RecordCandidate> sorted;

        switch (policyType) {
            case "label":
                String targetLabel = config.strategyParams().getOrDefault("targetLabel", "");
                sorted = candidates.stream()
                        .filter(c -> targetLabel.isEmpty() || targetLabel.equals(c.predictedLabel()))
                        .sorted(Comparator.comparingDouble(c -> -c.confidence()))
                        .toList();
                break;
            case "error":
                sorted = candidates.stream()
                        .filter(c -> c.trueLabel() != null && !c.trueLabel().equals(c.predictedLabel()))
                        .sorted(Comparator.comparingDouble(RecordCandidate::confidence))
                        .toList();
                break;
            case "random":
                List<RecordCandidate> shuffled = new ArrayList<>(candidates);
                java.util.Collections.shuffle(shuffled);
                sorted = shuffled;
                break;
            default:
                sorted = candidates.stream()
                        .filter(c -> c.confidence() < config.minConfidenceThreshold())
                        .sorted(Comparator.comparingDouble(RecordCandidate::confidence))
                        .toList();
                break;
        }

        for (int i = 0; i < Math.min(limit, sorted.size()); i++) {
            RecordCandidate c = sorted.get(i);
            result.add(new SelectedRecord(
                    c.recordId(), 1.0 - c.confidence(),
                    "Policy-defined (" + policyType + "): confidence " + String.format("%.3f", c.confidence()),
                    java.util.Map.of("policyType", policyType, "confidence", c.confidence())
            ));
        }
        return result;
    }
}
