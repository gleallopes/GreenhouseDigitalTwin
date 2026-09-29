package com.greenhousedigitaltwin.greenhouse.domain;

import java.util.Objects;

public record SensorId(String value) {
    public SensorId {
        Objects.requireNonNull(value, "Sensor id cannot be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("Sensor id cannot be blank");
        }
    }
}
