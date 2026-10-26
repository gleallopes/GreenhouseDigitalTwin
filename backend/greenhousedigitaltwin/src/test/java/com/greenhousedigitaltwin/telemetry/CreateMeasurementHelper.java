package com.greenhousedigitaltwin.telemetry;

import com.greenhousedigitaltwin.greenhouse.domain.SensorId;
import com.greenhousedigitaltwin.telemetry.domain.*;

import java.time.Instant;

public class CreateMeasurementHelper {

    public static Measurement createMeasurement(String meas, String msg) {
        return new Measurement(
                new MeasurementId(meas),
                new MessageId(msg),
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE,
                25.4,
                Unit.CELSIUS,
                Instant.parse("2026-09-30T15:00:00Z"),
                Instant.parse("2026-09-30T15:00:00Z")
        );
    }

    public static Measurement createMeasurementWithValue(String meas, String msg, double value) {
        return new Measurement(
                new MeasurementId(meas),
                new MessageId(msg),
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE,
                value,
                Unit.CELSIUS,
                Instant.parse("2026-09-30T15:00:00Z"),
                Instant.parse("2026-09-30T15:00:00Z")
        );
    }
}
