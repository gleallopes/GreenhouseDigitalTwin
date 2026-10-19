package com.greenhousedigitaltwin.twin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "twin_state")
public class TwinStateEntity {
    @Id
    @Column(name = "sensor_id", nullable = false)
    private String sensorId;

    @Column(name = "measurement_type", nullable = false)
    private String measurementType;

    @Column(name = "value", nullable = false)
    private double value;

    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "measuredAt", nullable = false)
    private Instant measuredAt;

    @Column(name = "receivedAt", nullable = false)
    private Instant receivedAt;

    protected TwinStateEntity() {}

    public String getSensorId() {
        return sensorId;
    }

    public String getMeasurementType() {
        return measurementType;
    }

    public double getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public Instant getMeasuredAt() {
        return measuredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
