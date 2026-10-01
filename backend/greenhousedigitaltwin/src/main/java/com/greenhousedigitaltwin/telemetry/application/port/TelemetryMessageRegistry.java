package com.greenhousedigitaltwin.telemetry.application.port;

public interface TelemetryMessageRegistry {
    boolean exists(String messageId);
}
