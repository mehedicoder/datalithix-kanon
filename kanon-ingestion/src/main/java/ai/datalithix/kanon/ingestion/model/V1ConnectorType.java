package ai.datalithix.kanon.ingestion.model;

import ai.datalithix.kanon.common.SourceCategory;
import ai.datalithix.kanon.common.SourceType;
import java.util.Optional;

public enum V1ConnectorType {
    UPLOAD(SourceCategory.DOCUMENT),
    EMAIL(SourceCategory.COMMUNICATION),
    MANUAL_ENTRY(SourceCategory.INTERACTIVE),
    REST_WEBHOOK(SourceCategory.API),
    DATABASE_IMPORT(SourceCategory.STORAGE),
    OBJECT_STORAGE(SourceCategory.STORAGE),
    CUSTOM(null);

    private final SourceCategory sourceCategory;

    V1ConnectorType(SourceCategory sourceCategory) {
        this.sourceCategory = sourceCategory;
    }

    public SourceCategory sourceCategory() {
        return sourceCategory;
    }

    public static Optional<V1ConnectorType> from(SourceCategory sourceCategory, SourceType sourceType) {
        if (sourceCategory == null || sourceType == null) {
            return Optional.empty();
        }
        return switch (sourceType) {
            case FILE_UPLOAD -> Optional.of(UPLOAD);
            case EMAIL_INBOX, SHARED_MAILBOX, FORWARDED_EMAIL -> Optional.of(EMAIL);
            case MANUAL_ENTRY -> Optional.of(MANUAL_ENTRY);
            case REST_API, WEBHOOK -> Optional.of(REST_WEBHOOK);
            case DATABASE_IMPORT -> Optional.of(DATABASE_IMPORT);
            case OBJECT_STORAGE -> Optional.of(OBJECT_STORAGE);
            default -> Optional.empty();
        };
    }
}
