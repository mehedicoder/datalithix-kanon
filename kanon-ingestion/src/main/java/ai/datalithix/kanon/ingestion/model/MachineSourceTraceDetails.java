package ai.datalithix.kanon.ingestion.model;

import java.math.BigDecimal;
import java.time.Instant;

public record MachineSourceTraceDetails(
        String sourceDeviceId,
        String missionId,
        Instant captureTimestamp,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal altitude,
        String telemetryRef
) implements SourceTraceDetails {}
