package com.greenhousedigitaltwin.telemetry.application;

public class TelemetryValidationException
        extends RuntimeException {

    private final TelemetryRejectionReason reason;

    public TelemetryValidationException(
            TelemetryRejectionReason reason
    ) {
        super("Telemetry rejected: " + reason);
        this.reason = reason;
    }

    public TelemetryRejectionReason reason() {
        return reason;
    }
}