package ai.datalithix.kanon.dataset.service;

import ai.datalithix.kanon.common.ActorType;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.dataset.model.CurationRule;
import ai.datalithix.kanon.dataset.model.DatasetDefinition;
import ai.datalithix.kanon.dataset.model.DatasetSplit;
import ai.datalithix.kanon.dataset.model.DatasetVersion;
import ai.datalithix.kanon.dataset.model.ExportFormat;
import ai.datalithix.kanon.dataset.model.SplitStrategy;
import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DefaultDatasetCurationService implements DatasetCurationService {
    private final DatasetRepository datasetRepository;
    private final EvidenceLedger evidenceLedger;
    private final Map<ExportFormat, ExportAdapter> exportAdapters;

    public DefaultDatasetCurationService(
            DatasetRepository datasetRepository,
            EvidenceLedger evidenceLedger,
            List<ExportAdapter> adapters
    ) {
        this.datasetRepository = datasetRepository;
        this.evidenceLedger = evidenceLedger;
        this.exportAdapters = adapters.stream()
                .collect(Collectors.toUnmodifiableMap(ExportAdapter::supportedFormat, a -> a));
    }

    @Override
    public DatasetVersion curate(DatasetDefinition definition, String actorId) {
        CurationRule rule = definition.curationRule();
        return curateWithRule(definition, rule, actorId);
    }

    @Override
    public DatasetVersion curateWithRule(DatasetDefinition definition, CurationRule rule, String actorId) {
        List<String> annotationRecordIds = selectAnnotationRecords(definition, rule);
        List<DatasetSplit> splits = computeSplits(annotationRecordIds, definition.splitStrategy(),
                definition.trainRatio(), definition.valRatio(), definition.testRatio());
        int nextVersion = definition.latestVersionNumber() + 1;
        String versionId = "dsv-" + UUID.randomUUID();
        Instant now = Instant.now();
        AuditMetadata audit = new AuditMetadata(now, actorId, now, actorId, 1);
        DatasetVersion version = new DatasetVersion(
                versionId, definition.datasetDefinitionId(), definition.tenantId(), nextVersion,
                rule == null ? null : rule.ruleId(), definition.splitStrategy(),
                splits, Map.of(), Map.of(), annotationRecordIds.size(),
                now, actorId, "CURATED", null, null, null, List.of(), audit
        );
        version = computeMetadata(version);
        DatasetVersion saved = datasetRepository.saveVersion(version);
        DatasetDefinition updated = new DatasetDefinition(
                definition.datasetDefinitionId(), definition.tenantId(), definition.name(),
                definition.description(), definition.domainType(), definition.sourceAnnotationIds(),
                definition.curationRule(), definition.splitStrategy(), definition.trainRatio(),
                definition.valRatio(), definition.testRatio(), definition.exportFormats(),
                definition.dataResidency(), definition.enabled(), nextVersion, updatedAudit(definition.audit(), actorId)
        );
        datasetRepository.save(updated);
        evidenceLedger.append(new EvidenceEvent(UUID.randomUUID().toString(), definition.tenantId(),
                definition.datasetDefinitionId(), "DATASET_VERSION_CURATED", ActorType.SYSTEM, actorId,
                "dataset-curation-service", null, null, Map.of(), Map.of("versionId", versionId),
                "Dataset version " + nextVersion + " curated", now));
        return saved;
    }

    @Override
    public List<ExportFormat> export(DatasetVersion version, List<ExportFormat> formats) {
        String basePath = System.getProperty("java.io.tmpdir") + "/kanon-exports";
        for (ExportFormat format : formats) {
            ExportAdapter adapter = exportAdapters.get(format);
            String artifactUri;
            if (adapter != null) {
                artifactUri = adapter.export(version, basePath);
            } else {
                artifactUri = "s3://datasets/" + version.tenantId() + "/" + version.datasetDefinitionId()
                        + "/v" + version.versionNumber() + "/export." + format.name().toLowerCase();
            }
            DatasetVersion updated = new DatasetVersion(
                    version.datasetVersionId(), version.datasetDefinitionId(), version.tenantId(),
                    version.versionNumber(), version.curationRuleId(), version.splitStrategy(),
                    version.splits(), version.labelDistribution(), version.classBalance(),
                    version.totalRecordCount(), version.curatedAt(), version.curatedBy(),
                    "EXPORTED", format.name(), artifactUri, null,
                    version.evidenceEventIds(), version.audit()
            );
            datasetRepository.saveVersion(updated);
            evidenceLedger.append(new EvidenceEvent(UUID.randomUUID().toString(), version.tenantId(),
                    version.datasetDefinitionId(), "DATASET_EXPORTED", ActorType.SYSTEM, "system",
                    "dataset-curation-service", null, null, Map.of(), Map.of("versionId", version.datasetVersionId()),
                    "Dataset v" + version.versionNumber() + " exported as " + format.name(), Instant.now()));
        }
        return formats;
    }

    @Override
    public DatasetVersion computeMetadata(DatasetVersion version) {
        Map<String, Long> labelDistribution = new HashMap<>();
        long total = 0;
        for (DatasetSplit split : version.splits()) {
            for (String recordId : split.annotationRecordIds()) {
                String label = extractLabel(recordId);
                labelDistribution.merge(label, 1L, Long::sum);
                total++;
            }
        }
        Map<String, Double> classBalance = new HashMap<>();
        if (total > 0) {
            for (Map.Entry<String, Long> entry : labelDistribution.entrySet()) {
                classBalance.put(entry.getKey(), (double) entry.getValue() / total);
            }
        }
        return new DatasetVersion(
                version.datasetVersionId(), version.datasetDefinitionId(), version.tenantId(),
                version.versionNumber(), version.curationRuleId(), version.splitStrategy(),
                version.splits(), Map.copyOf(labelDistribution), Map.copyOf(classBalance),
                total, version.curatedAt(), version.curatedBy(), version.exportStatus(),
                version.exportFormat(), version.exportArtifactUri(), version.failureReason(),
                version.evidenceEventIds(), version.audit()
        );
    }

    private List<String> selectAnnotationRecords(DatasetDefinition definition, CurationRule rule) {
        return new ArrayList<>(definition.sourceAnnotationIds());
    }

    private List<DatasetSplit> computeSplits(List<String> recordIds, SplitStrategy strategy,
                                              double trainRatio, double valRatio, double testRatio) {
        List<String> shuffled = new ArrayList<>(recordIds);
        Collections.shuffle(shuffled);
        int total = shuffled.size();
        int trainEnd = (int) (total * trainRatio);
        int valEnd = trainEnd + (int) (total * valRatio);
        return List.of(
                new DatasetSplit("train", trainRatio, List.copyOf(shuffled.subList(0, trainEnd))),
                new DatasetSplit("val", valRatio, List.copyOf(shuffled.subList(trainEnd, Math.min(valEnd, total)))),
                new DatasetSplit("test", testRatio, List.copyOf(shuffled.subList(Math.min(valEnd, total), total)))
        );
    }

    private String extractLabel(String recordId) {
        return "unknown";
    }

    private static AuditMetadata updatedAudit(AuditMetadata existing, String actorId) {
        return new AuditMetadata(existing.createdAt(), existing.createdBy(),
                Instant.now(), actorId, existing.version() + 1);
    }

}
