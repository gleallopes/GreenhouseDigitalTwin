package com.greenhousedigitaltwin.telemetry.application;

import java.time.Instant;

public record IngestTelemetryCommand (
        String measurementId,
        String messageId,
        String deviceId,
        String sensorId,
        String measurementType,
        Double value,
        String unit,
        Instant measuredAt
){
}
