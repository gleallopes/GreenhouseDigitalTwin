package com.greenhousedigitaltwin.telemetry.domain;

import java.util.Objects;

// Message of incoming MQTT message
public record MessageId(String value) {
    public MessageId {
        Objects.requireNonNull(value, "Message ID must not be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("Message ID must not be blank");
        }
    }
}
