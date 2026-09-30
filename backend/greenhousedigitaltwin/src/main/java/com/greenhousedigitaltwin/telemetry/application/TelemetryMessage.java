package com.greenhousedigitaltwin.telemetry.application;

import java.time.Instant;

public record TelemetryMessage(
        String messageId,
        String deviceId,
        String sensorId,
        String measurementType,
        double value,
        String unit,
        Instant measuredAt
) {
}
