package ai.datalithix.kanon.common.runtime;

public record PayloadTransferPolicy(
        long maxInlineBytes,
        boolean objectStorageRequired,
        boolean directTransferPreferred,
        boolean checksumRequired
) {
    public PayloadTransferPolicy {
        if (maxInlineBytes < 0) {
            throw new IllegalArgumentException("maxInlineBytes must be zero or greater");
        }
    }
}
