package com.greenhousedigitaltwin.telemetry.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "measurement",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_measurement_message_id",
                        columnNames = "message_id"
                )
        }
)
public class MeasurementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (name="message_id", nullable = false, updatable = false)
    private String messageId;

    @Column(name = "sensor_id", nullable = false, updatable = false)
    private String sensorId;

    @Column(name = "measurement_type", nullable = false, updatable = false)
    private String measurementType;

    @Column(name = "value", nullable = false, updatable = false)
    private double value;

    @Column(name = "unit", nullable = false, updatable = false)
    private String unit;

    @Column(name = "measured_at", nullable = false, updatable = false)
    private Instant measuredAt;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected MeasurementEntity() {
//        Required by JPA
    }

    public MeasurementEntity(String message, String sensorId, String measurementType, double value, String unit, Instant measuredAt, Instant receivedAt) {
        this.messageId = message;
        this.sensorId = sensorId;
        this.measurementType = measurementType;
        this.value = value;
        this.unit = unit;
        this.measuredAt = measuredAt;
        this.receivedAt = receivedAt;
    }

    public long getId() {
        return id;
    }

    public String getMessageId() {
        return messageId;
    }

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
