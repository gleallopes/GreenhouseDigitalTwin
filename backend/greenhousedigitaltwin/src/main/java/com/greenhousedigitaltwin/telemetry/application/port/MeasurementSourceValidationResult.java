package com.greenhousedigitaltwin.telemetry.application.port;

public enum MeasurementSourceValidationResult {
    VALID,
    UNKNOWN_DEVICE,
    UNKNOWN_SENSOR,
    SENSOR_DEVICE_MISMATCH,
    INCOMPATIBLE_MEASUREMENT_TYPE,
}
