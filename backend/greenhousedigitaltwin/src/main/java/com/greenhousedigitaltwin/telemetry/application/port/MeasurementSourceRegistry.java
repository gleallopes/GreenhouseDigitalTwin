package com.greenhousedigitaltwin.telemetry.application.port;

import com.greenhousedigitaltwin.greenhouse.domain.SensorId;
import com.greenhousedigitaltwin.telemetry.domain.MeasurementType;

public interface MeasurementSourceRegistry {
    MeasurementSourceValidationResult validateSource(
            String deviceId,
            SensorId sensorId,
            MeasurementType measurementType
    );
}
