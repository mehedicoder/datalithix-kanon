package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningConfig;
import ai.datalithix.kanon.activelearning.model.SelectedRecord;
import ai.datalithix.kanon.activelearning.model.SelectionStrategyType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class QueryByCommitteeSelector implements RecordSelector {
    @Override
    public SelectionStrategyType supportedStrategy() {
        return SelectionStrategyType.QUERY_BY_COMMITTEE;
    }

    @Override
    public List<SelectedRecord> selectRecords(List<RecordCandidate> candidates, ActiveLearningConfig config) {
        List<RecordCandidate> withDisagreement = candidates.stream()
                .filter(c -> c.predictionProbs() != null && c.predictionProbs().length > 0)
                .sorted(Comparator.comparingDouble(c -> -voteEntropy(c.predictionProbs())))
                .toList();
        int limit = Math.min(withDisagreement.size(), config.maxRecordsPerCycle());
        List<SelectedRecord> result = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            RecordCandidate c = withDisagreement.get(i);
            double entropy = voteEntropy(c.predictionProbs());
            result.add(new SelectedRecord(
                    c.recordId(), entropy,
                    "QBC: vote entropy " + String.format("%.3f", entropy),
                    java.util.Map.of("voteEntropy", entropy, "numClasses", c.predictionProbs().length)
            ));
        }
        return result;
    }

    private static double voteEntropy(double[] probs) {
        double entropy = 0;
        for (double p : probs) {
            if (p > 0) {
                entropy -= p * Math.log(p);
            }
        }
        return entropy;
    }
}
