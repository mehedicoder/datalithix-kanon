package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningConfig;
import ai.datalithix.kanon.activelearning.model.SelectedRecord;
import ai.datalithix.kanon.activelearning.model.SelectionStrategyType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UncertaintySamplingSelector implements RecordSelector {
    @Override
    public SelectionStrategyType supportedStrategy() {
        return SelectionStrategyType.UNCERTAINTY_SAMPLING;
    }

    @Override
    public List<SelectedRecord> selectRecords(List<RecordCandidate> candidates, ActiveLearningConfig config) {
        List<RecordCandidate> belowThreshold = candidates.stream()
                .filter(c -> c.confidence() < config.minConfidenceThreshold())
                .sorted(Comparator.comparingDouble(RecordCandidate::confidence))
                .toList();
        int limit = Math.min(belowThreshold.size(), config.maxRecordsPerCycle());
        List<SelectedRecord> result = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            RecordCandidate c = belowThreshold.get(i);
            double uncertainty = 1.0 - c.confidence();
            result.add(new SelectedRecord(
                    c.recordId(), uncertainty,
                    "Uncertainty sampling: confidence " + String.format("%.3f", c.confidence())
                            + " below threshold " + config.minConfidenceThreshold(),
                    java.util.Map.of("confidence", c.confidence(), "uncertainty", uncertainty)
            ));
        }
        return result;
    }
}
