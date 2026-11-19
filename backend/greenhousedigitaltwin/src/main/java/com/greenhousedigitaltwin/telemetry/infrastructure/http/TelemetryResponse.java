package com.greenhousedigitaltwin.telemetry.infrastructure.http;

import com.greenhousedigitaltwin.telemetry.application.TelemetryIngestionOutcome;

public record TelemetryResponse(TelemetryIngestionOutcome outcome) {
}
