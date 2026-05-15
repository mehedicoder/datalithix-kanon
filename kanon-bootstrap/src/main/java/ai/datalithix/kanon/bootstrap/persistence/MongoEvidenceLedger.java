package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.evidence.model.EvidenceEvent;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import ai.datalithix.kanon.evidence.service.EvidenceQueryService;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import static ai.datalithix.kanon.common.ActorType.valueOf;

@Component
@Primary
@Profile("!test")
public class MongoEvidenceLedger implements EvidenceLedger, EvidenceQueryService {
    private static final String COLLECTION = "evidence_events";

    private final MongoTemplate mongoTemplate;

    public MongoEvidenceLedger(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    void ensureIndexes() {
        mongoTemplate.indexOps(COLLECTION)
                .ensureIndex(new Index()
                        .on("tenant_id", Sort.Direction.ASC)
                        .on("case_id", Sort.Direction.ASC)
                        .on("occurred_at", Sort.Direction.DESC));
        mongoTemplate.indexOps(COLLECTION)
                .ensureIndex(new Index()
                        .on("tenant_id", Sort.Direction.ASC)
                        .on("event_type", Sort.Direction.ASC)
                        .on("occurred_at", Sort.Direction.DESC));
        mongoTemplate.indexOps(COLLECTION)
                .ensureIndex(new Index()
                        .on("tenant_id", Sort.Direction.ASC)
                        .on("actor_type", Sort.Direction.ASC)
                        .on("occurred_at", Sort.Direction.DESC));
        mongoTemplate.indexOps(COLLECTION)
                .ensureIndex(new Index()
                        .on("event_id", Sort.Direction.ASC)
                        .unique());
    }

    @Override
    public void append(EvidenceEvent event) {
        Document document = new Document()
                .append("event_id", event.eventId())
                .append("tenant_id", event.tenantId())
                .append("case_id", event.caseId())
                .append("event_type", event.eventType())
                .append("actor_type", event.actorType() == null ? null : event.actorType().name())
                .append("actor_id", event.actorId())
                .append("agent_key", event.agentKey())
                .append("model_profile", event.modelProfile())
                .append("policy_version", event.policyVersion())
                .append("before_state", event.beforeState())
                .append("after_state", event.afterState())
                .append("rationale", event.rationale())
                .append("occurred_at", event.occurredAt());
        mongoTemplate.insert(document, COLLECTION);
    }

    @Override
    public List<EvidenceEvent> findRecent(String tenantId, int limit) {
        Query query = Query.query(Criteria.where("tenant_id").is(tenantId))
                .with(Sort.by(Sort.Direction.DESC, "occurred_at"))
                .limit(Math.max(0, limit));
        return mongoTemplate.find(query, Document.class, COLLECTION).stream()
                .map(this::map)
                .toList();
    }

    @Override
    public List<EvidenceEvent> findByCaseId(String tenantId, String caseId, int limit) {
        Query query = Query.query(Criteria.where("tenant_id").is(tenantId).and("case_id").is(caseId))
                .with(Sort.by(Sort.Direction.DESC, "occurred_at"))
                .limit(Math.max(0, limit));
        return mongoTemplate.find(query, Document.class, COLLECTION).stream()
                .map(this::map)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private EvidenceEvent map(Document document) {
        String actorType = document.getString("actor_type");
        return new EvidenceEvent(
                document.getString("event_id"),
                document.getString("tenant_id"),
                document.getString("case_id"),
                document.getString("event_type"),
                actorType == null ? null : valueOf(actorType),
                document.getString("actor_id"),
                document.getString("agent_key"),
                document.getString("model_profile"),
                document.getString("policy_version"),
                (Map<String, Object>) document.get("before_state", Map.class),
                (Map<String, Object>) document.get("after_state", Map.class),
                document.getString("rationale"),
                document.get("occurred_at", Instant.class)
        );
    }
}
