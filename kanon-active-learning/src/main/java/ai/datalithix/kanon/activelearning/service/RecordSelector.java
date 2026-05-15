package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningConfig;
import ai.datalithix.kanon.activelearning.model.SelectedRecord;
import ai.datalithix.kanon.activelearning.model.SelectionStrategyType;
import java.util.List;

public interface RecordSelector {
    SelectionStrategyType supportedStrategy();
    List<SelectedRecord> selectRecords(
            List<RecordCandidate> candidates,
            ActiveLearningConfig config
    );

    record RecordCandidate(
            String recordId,
            double confidence,
            String predictedLabel,
            String trueLabel,
            double[] featureVector,
            double[] predictionProbs
    ) {}
}
