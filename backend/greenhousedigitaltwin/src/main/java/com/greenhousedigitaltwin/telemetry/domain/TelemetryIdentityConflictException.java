package com.greenhousedigitaltwin.telemetry.domain;

public class TelemetryIdentityConflictException extends RuntimeException {
    public TelemetryIdentityConflictException(String message) {
        super(message);
    }
}
