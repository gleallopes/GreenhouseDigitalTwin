package com.greenhousedigitaltwin.telemetry.application;

import java.util.Objects;

public record TelemetryValidationResult(
        boolean valid,
        TelemetryRejectionReason rejectionReason
) {
    public static TelemetryValidationResult accepted() {
        return new TelemetryValidationResult(true, null);
    }

    public static TelemetryValidationResult rejected(
            TelemetryRejectionReason reason) {

        return new TelemetryValidationResult(
                false,
                Objects.requireNonNull(reason)
        );
    }
}
