package com.greenhousedigitaltwin.telemetry.application;

public enum TelemetryRejectionReason {
    STRUCTURAL_ERROR,
    SEMANTIC_ERROR,
    UNKNOWN_DEVICE,
    UNKNOWN_SENSOR,
    SENSOR_DEVICE_MISMATCH,
    INCOMPATIBLE_MEASUREMENT_TYPE,
    DUPLICATE_MESSAGE,
    PHYSICAL_VALUE_OUT_OF_RANGE,
    INVALID_TIMESTAMP
}
