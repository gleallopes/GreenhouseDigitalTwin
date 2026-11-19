package com.greenhousedigitaltwin.telemetry.infrastructure.http;

import com.greenhousedigitaltwin.telemetry.application.IngestTelemetryCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record TelemetryRequest(
        @NotBlank @Size(max = 100) String measurementId,
        @NotBlank @Size(max = 100) String messageId,
        @NotBlank @Size(max = 50) String deviceId,
        @NotBlank @Size(max = 50) String sensorId,
        @NotBlank String measurementType,
        @NotNull Double value,
        @NotBlank String unit,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,6})?(?:Z|[+-]\\d{2}:\\d{2})")
        String measuredAt
) {
    IngestTelemetryCommand toCommand() {
        return new IngestTelemetryCommand(
                measurementId, messageId, deviceId, sensorId,
                measurementType, value, unit, OffsetDateTime.parse(measuredAt).toInstant()
        );
    }

    @Override
    public String toString() {
        return "TelemetryRequest";
    }
}
