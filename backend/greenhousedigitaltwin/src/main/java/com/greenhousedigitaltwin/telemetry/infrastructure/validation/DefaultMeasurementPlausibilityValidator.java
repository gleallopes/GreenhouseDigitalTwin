package com.greenhousedigitaltwin.telemetry.infrastructure.validation;

import com.greenhousedigitaltwin.telemetry.application.port.MeasurementPlausibilityValidator;
import com.greenhousedigitaltwin.telemetry.domain.MeasurementType;
import org.springframework.stereotype.Component;

@Component
public final class DefaultMeasurementPlausibilityValidator
        implements MeasurementPlausibilityValidator {

    @Override
    public boolean isPlausible(
            MeasurementType measurementType,
            double value
    ) {
        if (measurementType == null || !Double.isFinite(value)) {
            return false;
        }

        return switch (measurementType) {
            case TEMPERATURE -> value >= -40 && value <= 85;
            case HUMIDITY -> value >= 0 && value <= 100;
            case LUMINOSITY -> value >= 0 && value <= 100_000;
        };
    }
}