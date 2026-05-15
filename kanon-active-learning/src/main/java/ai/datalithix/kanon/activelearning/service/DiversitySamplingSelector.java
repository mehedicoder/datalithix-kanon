package ai.datalithix.kanon.activelearning.service;

import ai.datalithix.kanon.activelearning.model.ActiveLearningConfig;
import ai.datalithix.kanon.activelearning.model.SelectedRecord;
import ai.datalithix.kanon.activelearning.model.SelectionStrategyType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DiversitySamplingSelector implements RecordSelector {
    @Override
    public SelectionStrategyType supportedStrategy() {
        return SelectionStrategyType.DIVERSITY_SAMPLING;
    }

    @Override
    public List<SelectedRecord> selectRecords(List<RecordCandidate> candidates, ActiveLearningConfig config) {
        if (candidates.isEmpty()) return List.of();
        List<RecordCandidate> sorted = new ArrayList<>(candidates);
        List<SelectedRecord> result = new ArrayList<>();
        double[] centroid = computeCentroid(sorted);
        sorted.sort(Comparator.comparingDouble(c -> -euclideanDistance(c.featureVector(), centroid)));
        int limit = Math.min(sorted.size(), config.maxRecordsPerCycle());
        for (int i = 0; i < limit; i++) {
            RecordCandidate c = sorted.get(i);
            double distance = euclideanDistance(c.featureVector(), centroid);
            result.add(new SelectedRecord(
                    c.recordId(), distance,
                    "Diversity sampling: distance " + String.format("%.3f", distance) + " from centroid",
                    java.util.Map.of("distanceFromCentroid", distance)
            ));
        }
        return result;
    }

    private static double[] computeCentroid(List<RecordCandidate> candidates) {
        if (candidates.isEmpty()) return new double[0];
        int dims = candidates.getFirst().featureVector().length;
        double[] centroid = new double[dims];
        for (RecordCandidate c : candidates) {
            double[] vec = c.featureVector();
            for (int i = 0; i < Math.min(dims, vec.length); i++) {
                centroid[i] += vec[i];
            }
        }
        for (int i = 0; i < dims; i++) {
            centroid[i] /= candidates.size();
        }
        return centroid;
    }

    private static double euclideanDistance(double[] a, double[] b) {
        int len = Math.min(a.length, b.length);
        double sum = 0;
        for (int i = 0; i < len; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}
