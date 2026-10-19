package com.greenhousedigitaltwin.twin.domain;

import com.greenhousedigitaltwin.greenhouse.domain.SensorId;
import com.greenhousedigitaltwin.telemetry.domain.MeasurementType;
import com.greenhousedigitaltwin.telemetry.domain.Unit;

import java.time.Instant;
import java.util.Objects;

public record SensorState (
        SensorId sensorId,
        MeasurementType measurementType,
        double value,
        Unit unit,
        Instant measuredAt,
        Instant receivedAt
){
    public SensorState {
        Objects.requireNonNull(sensorId);
        Objects.requireNonNull(measurementType);
        Objects.requireNonNull(unit);
        Objects.requireNonNull(measuredAt);
        Objects.requireNonNull(receivedAt);

        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Sensor state value must be finite"
            );
        }
    }

    public boolean isNewerThan(SensorState current){
        Objects.requireNonNull(current);

        if (!sensorId.equals(current.sensorId())) {
            throw new IllegalArgumentException("Cannot compare states from different sensors");
        }

        return measuredAt.isAfter(current.measuredAt());
    }
}
