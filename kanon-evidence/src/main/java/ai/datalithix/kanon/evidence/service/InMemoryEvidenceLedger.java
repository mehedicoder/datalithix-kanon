package ai.datalithix.kanon.evidence.service;

import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InMemoryEvidenceLedger implements EvidenceLedger, EvidenceQueryService {
    private final List<EvidenceEvent> events = new ArrayList<>();

    @Override
    public synchronized void append(EvidenceEvent event) {
        events.add(event);
    }

    public synchronized List<EvidenceEvent> snapshot() {
        return List.copyOf(events);
    }

    @Override
    public synchronized List<EvidenceEvent> findRecent(String tenantId, int limit) {
        return events.stream()
                .filter(event -> tenantId.equals(event.tenantId()))
                .sorted(Comparator.comparing(EvidenceEvent::occurredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(Math.max(0, limit))
                .toList();
    }

    @Override
    public synchronized List<EvidenceEvent> findByCaseId(String tenantId, String caseId, int limit) {
        return events.stream()
                .filter(event -> tenantId.equals(event.tenantId()))
                .filter(event -> caseId.equals(event.caseId()))
                .sorted(Comparator.comparing(EvidenceEvent::occurredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(Math.max(0, limit))
                .toList();
    }
}
