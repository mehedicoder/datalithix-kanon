package ai.datalithix.kanon.evidence.service;

import ai.datalithix.kanon.evidence.model.EvidenceEvent;

public interface EvidenceLedger {
    void append(EvidenceEvent event);
}
