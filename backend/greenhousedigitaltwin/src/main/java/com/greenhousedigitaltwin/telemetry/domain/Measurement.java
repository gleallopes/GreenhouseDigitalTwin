package com.greenhousedigitaltwin.telemetry.domain;

import com.greenhousedigitaltwin.greenhouse.domain.SensorId;

import java.time.Instant;
import java.util.Objects;

public record Measurement(MeasurementId id, MessageId messageId, SensorId sensorId, MeasurementType type, double value,
                          Unit unit, Instant measuredAt, Instant receivedAt) {

    public Measurement(MeasurementId id, MessageId messageId,
                       SensorId sensorId, MeasurementType type,
                       double value, Unit unit,
                       Instant measuredAt, Instant receivedAt) {
        this.id = Objects.requireNonNull(id, "Measurement ID must not be null");
        this.messageId = Objects.requireNonNull(messageId, "Message ID must not be null");
        this.sensorId = Objects.requireNonNull(sensorId, "Sensor ID must not be null");
        this.type = Objects.requireNonNull(type, "Measurement type must not be null");
        this.unit = Objects.requireNonNull(unit, "Unit must not be null");
        this.measuredAt = Objects.requireNonNull(measuredAt, "Measured-at timestamp must not be null");
        this.receivedAt = Objects.requireNonNull(receivedAt, "Received-at timestamp must not be null");

        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Measurement value must be finite");
        }

        if (type.supports(unit)) {
            throw new IllegalArgumentException(
                    "Unit " + unit + " is not valid for measurement type " + type
            );
        }

        this.value = value;
    }

    public boolean sameEventAs(Measurement other) {
        if (other == null) {
            return false;
        }

        return id.equals(other.id())
                && messageId.equals(other.messageId())
                && sensorId.equals(other.sensorId())
                && type.equals(other.type())
                && Double.compare(value, other.value()) == 0
                && unit.equals(other.unit())
                && measuredAt.equals(other.measuredAt());
    }
}
