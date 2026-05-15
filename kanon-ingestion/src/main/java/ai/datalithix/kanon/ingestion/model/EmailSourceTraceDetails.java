package ai.datalithix.kanon.ingestion.model;

import java.time.Instant;
import java.util.List;

public record EmailSourceTraceDetails(
        String mailbox,
        String messageId,
        String threadId,
        String fromAddress,
        List<String> toAddresses,
        List<String> ccAddresses,
        String subject,
        Instant receivedAt,
        int attachmentCount
) implements SourceTraceDetails {
    public EmailSourceTraceDetails {
        toAddresses = toAddresses == null ? List.of() : List.copyOf(toAddresses);
        ccAddresses = ccAddresses == null ? List.of() : List.copyOf(ccAddresses);
    }
}
