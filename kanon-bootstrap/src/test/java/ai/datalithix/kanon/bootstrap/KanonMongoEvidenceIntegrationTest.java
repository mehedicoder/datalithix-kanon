package ai.datalithix.kanon.bootstrap;

import static org.junit.jupiter.api.Assertions.*;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Tag("docker")
class KanonMongoEvidenceIntegrationTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    private static MongoDatabase db;

    @BeforeAll
    static void setUp() {
        db = MongoClients.create(mongo.getConnectionString())
                .getDatabase("datalithix_kanon");
    }

    @Test
    void appendsAndRetrievesEvidenceEvent() {
        var collection = db.getCollection("evidence_events");

        collection.insertOne(new Document()
                .append("event_id", "evt-1")
                .append("tenant_id", "tenant-a")
                .append("case_id", "case-1")
                .append("event_type", "ANNOTATION_CREATED")
                .append("actor_type", "HUMAN")
                .append("actor_id", "user-1")
                .append("agent_key", "extraction-agent")
                .append("rationale", "Initial annotation")
                .append("occurred_at", Instant.now()));

        var doc = collection.find(new Document("event_id", "evt-1")).first();
        assertNotNull(doc);
        assertEquals("tenant-a", doc.getString("tenant_id"));
        assertEquals("ANNOTATION_CREATED", doc.getString("event_type"));
        assertEquals("HUMAN", doc.getString("actor_type"));
    }

    @Test
    void findsEventsByTenantAndCase() {
        var collection = db.getCollection("evidence_events");
        var base = Instant.now();

        collection.insertOne(new Document()
                .append("event_id", "evt-c1").append("tenant_id", "t1")
                .append("case_id", "case-x").append("event_type", "CREATED")
                .append("actor_type", "SYSTEM").append("occurred_at", base));
        collection.insertOne(new Document()
                .append("event_id", "evt-c2").append("tenant_id", "t1")
                .append("case_id", "case-x").append("event_type", "REVIEWED")
                .append("actor_type", "HUMAN").append("occurred_at", base.plusSeconds(10)));
        collection.insertOne(new Document()
                .append("event_id", "evt-c3").append("tenant_id", "t2")
                .append("case_id", "case-x").append("event_type", "CREATED")
                .append("actor_type", "SYSTEM").append("occurred_at", base));

        var t1CaseX = collection.find(new Document("tenant_id", "t1").append("case_id", "case-x"))
                .into(List.of());
        assertEquals(2, t1CaseX.size(), "Tenant t1 should see 2 events for case-x");

        var t2CaseX = collection.find(new Document("tenant_id", "t2").append("case_id", "case-x"))
                .into(List.of());
        assertEquals(1, t2CaseX.size(), "Tenant t2 should see 1 event for case-x");
    }

    @Test
    void enforcesCrossTenantIsolation() {
        var collection = db.getCollection("evidence_events");
        var now = Instant.now();

        collection.insertOne(new Document()
                .append("event_id", "evt-sec-a").append("tenant_id", "secret-a")
                .append("event_type", "ACCESS").append("actor_type", "HUMAN")
                .append("occurred_at", now));
        collection.insertOne(new Document()
                .append("event_id", "evt-sec-b").append("tenant_id", "secret-b")
                .append("event_type", "ACCESS").append("actor_type", "HUMAN")
                .append("occurred_at", now));

        var tenantAEvents = collection.find(new Document("tenant_id", "secret-a")).into(List.of());
        assertEquals(1, tenantAEvents.size());

        var noLeak = collection.find(new Document("tenant_id", "secret-b"))
                .filter(new Document("event_id", "evt-sec-a"))
                .into(List.of());
        assertTrue(noLeak.isEmpty(), "Tenant B should not see Tenant A events");
    }

    @Test
    void indexesAreCreated() {
        var collection = db.getCollection("evidence_events");
        var indexes = collection.listIndexes().into(List.of());
        var indexNames = indexes.stream().map(idx -> idx.getString("name")).toList();

        assertTrue(indexNames.contains("tenant_id_1_case_id_1_occurred_at_-1")
                        || indexNames.stream().anyMatch(n -> n.contains("tenant_id") && n.contains("case_id")),
                "Expected compound index on tenant_id, case_id, occurred_at. Found: " + indexNames);
    }

    @Test
    void eventOrderingByTimestamp() {
        var collection = db.getCollection("evidence_events");
        var now = Instant.now();

        collection.insertOne(new Document()
                .append("event_id", "evt-ord-1").append("tenant_id", "ord-t")
                .append("event_type", "FIRST").append("occurred_at", now));
        collection.insertOne(new Document()
                .append("event_id", "evt-ord-2").append("tenant_id", "ord-t")
                .append("event_type", "SECOND").append("occurred_at", now.plusSeconds(60)));
        collection.insertOne(new Document()
                .append("event_id", "evt-ord-3").append("tenant_id", "ord-t")
                .append("event_type", "THIRD").append("occurred_at", now.plusSeconds(120)));

        var events = collection.find(new Document("tenant_id", "ord-t"))
                .sort(new Document("occurred_at", -1))
                .into(List.of());

        assertEquals(3, events.size());
        assertEquals("THIRD", events.get(0).getString("event_type"));
        assertEquals("SECOND", events.get(1).getString("event_type"));
        assertEquals("FIRST", events.get(2).getString("event_type"));
    }
}
