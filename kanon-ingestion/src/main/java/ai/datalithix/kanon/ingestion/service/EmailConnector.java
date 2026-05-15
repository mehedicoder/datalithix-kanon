package ai.datalithix.kanon.ingestion.service;

import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import ai.datalithix.kanon.ingestion.model.EmailSourceTraceDetails;
import ai.datalithix.kanon.ingestion.model.IngestionRequest;
import ai.datalithix.kanon.ingestion.model.SourcePayload;
import ai.datalithix.kanon.ingestion.model.SourceTraceDetails;
import ai.datalithix.kanon.ingestion.model.V1ConnectorType;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kanon.connectors.email-enabled", havingValue = "true", matchIfMissing = true)
public class EmailConnector extends AbstractV1DataSourceConnector {
    public EmailConnector() {
        super("v1-email", V1ConnectorType.EMAIL, SourceCategory.COMMUNICATION, SourceType.EMAIL_INBOX,
                Set.of(SourceType.EMAIL_INBOX, SourceType.SHARED_MAILBOX, SourceType.FORWARDED_EMAIL), defaultPolicy(true, true, true));
    }

    @Override
    protected SourceTraceDetails details(IngestionRequest request) {
        String receivedAt = attribute(request, "received_at");
        return new EmailSourceTraceDetails(
                attribute(request, "mailbox"),
                attribute(request, "message_id"),
                attribute(request, "thread_id"),
                attribute(request, "from_address"),
                csv(attribute(request, "to_addresses")),
                csv(attribute(request, "cc_addresses")),
                attribute(request, "subject"),
                receivedAt == null || receivedAt.isBlank() ? null : Instant.parse(receivedAt),
                intAttribute(request, "attachment_count")
        );
    }

    private static List<String> csv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
