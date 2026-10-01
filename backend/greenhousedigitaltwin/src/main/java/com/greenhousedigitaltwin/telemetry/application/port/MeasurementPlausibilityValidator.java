package com.greenhousedigitaltwin.telemetry.application.port;

import com.greenhousedigitaltwin.telemetry.domain.MeasurementType;

public interface MeasurementPlausibilityValidator {
    boolean isPlausible(
            MeasurementType measurementType,
            double value
    );
}
