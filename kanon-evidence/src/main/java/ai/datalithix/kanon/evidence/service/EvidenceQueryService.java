package ai.datalithix.kanon.evidence.service;

import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import java.util.List;

public interface EvidenceQueryService {
    List<EvidenceEvent> findRecent(String tenantId, int limit);

    List<EvidenceEvent> findByCaseId(String tenantId, String caseId, int limit);
}
