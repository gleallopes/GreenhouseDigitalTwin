package com.greenhousedigitaltwin.telemetry.domain;

import com.greenhousedigitaltwin.greenhouse.domain.SensorId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class MeasurementTest {
    private final MeasurementId measurementId = new MeasurementId("MEAS-001");
    private final MessageId messageId = new MessageId("MSG-001");
    private final SensorId sensorId = new SensorId("TEMP-001");
    private final Instant measuredAt = Instant.parse("2026-09-24T18:30:15Z");
    private final Instant receivedAt = Instant.parse("2026-09-24T18:30:16Z");

    @Test
    void shouldCreateValidTemperatureMeasurement() {
        Measurement measurement = new Measurement(
                measurementId,
                messageId,
                sensorId,
                MeasurementType.TEMPERATURE,
                25.4,
                Unit.CELSIUS,
                measuredAt,
                receivedAt
        );

        assertEquals(25.4, measurement.getValue());
        assertEquals(MeasurementType.TEMPERATURE, measurement.getType());
        assertEquals(Unit.CELSIUS, measurement.getUnit());
        assertEquals(measuredAt, measurement.getMeasuredAt());
        assertEquals(receivedAt, measurement.getReceivedAt());
    }

    @Test
    void shouldRejectIncompatibleUnit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Measurement(
                        measurementId,
                        messageId,
                        sensorId,
                        MeasurementType.TEMPERATURE,
                        25.4,
                        Unit.LUX,
                        measuredAt,
                        receivedAt
                )
        );
    }

    @Test
    void shouldRejectNonFiniteValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Measurement(
                    measurementId,
                    messageId,
                    sensorId,
                    MeasurementType.TEMPERATURE,
                    Double.NaN,
                    Unit.CELSIUS,
                    measuredAt,
                    receivedAt
                )
        );
    }

}
