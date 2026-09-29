package com.greenhousedigitaltwin.telemetry.domain;

import java.util.Objects;

// Identity of domain Measurement
public record MeasurementId(String value) {
    public MeasurementId {
        Objects.requireNonNull(value, "Measurement ID must not be null");

        if (value.isBlank()) throw new IllegalArgumentException("Measurement ID must not be blank");
    }
}
