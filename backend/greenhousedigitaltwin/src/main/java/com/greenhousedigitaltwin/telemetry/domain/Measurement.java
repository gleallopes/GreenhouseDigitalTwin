package com.greenhousedigitaltwin.telemetry.domain;

import com.greenhousedigitaltwin.greenhouse.domain.SensorId;

import java.time.Instant;
import java.util.Objects;

public class Measurement {

    private final MeasurementId id;
    private final MessageId messageId;
    private final SensorId sensorId;
    private final MeasurementType type;
    private final double value;
    private final Unit unit;
    private final Instant measuredAt;
    private final Instant receivedAt;

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

        if (!type.supports(unit)) {
            throw new IllegalArgumentException(
                    "Unit " + unit + " is not valid for measurement type " + type
            );
        }

        this.value = value;
    }

    public MeasurementId getId() {
        return id;
    }

    public MessageId getMessageId() {
        return messageId;
    }

    public SensorId getSensorId() {
        return sensorId;
    }

    public MeasurementType getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    public Unit getUnit() {
        return unit;
    }

    public Instant getMeasuredAt() {
        return measuredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
