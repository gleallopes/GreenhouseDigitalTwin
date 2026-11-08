package com.greenhousedigitaltwin.telemetry.application;

import com.greenhousedigitaltwin.greenhouse.domain.SensorId;
import com.greenhousedigitaltwin.telemetry.application.port.MeasurementPlausibilityValidator;
import com.greenhousedigitaltwin.telemetry.application.port.MeasurementSourceRegistry;
import com.greenhousedigitaltwin.telemetry.domain.MeasurementType;
import com.greenhousedigitaltwin.telemetry.domain.Unit;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public final class TelemetryValidator {

    private final MeasurementSourceRegistry sourceRegistry;
    private final MeasurementPlausibilityValidator plausibilityValidator;

    public TelemetryValidator(
            MeasurementSourceRegistry sourceRegistry,
            MeasurementPlausibilityValidator plausibilityValidator
    ) {
        this.sourceRegistry = sourceRegistry;
        this.plausibilityValidator = plausibilityValidator;
    }

    public TelemetryValidationResult validate(TelemetryMessage message) {
        // Structural validation
        if (message == null
                || message.value() == null
                || isBlank(message.messageId())
                || isBlank(message.deviceId())
                || isBlank(message.sensorId())
                || isBlank(message.measurementType())
                || isBlank(message.unit())) {
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.STRUCTURAL_ERROR
            );
        }

        if (message.measuredAt() == null) {
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.INVALID_TIMESTAMP
            );
        }

        if (!Double.isFinite(message.value())) {
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.STRUCTURAL_ERROR
            );
        }

        // Semantic validation
        MeasurementType measurementType = parseMeasurementType(message.measurementType());
        if (measurementType == null) {
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.SEMANTIC_ERROR
            );
        }

        Unit unit = parseUnit(message.unit());
        if (unit == null) {
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.SEMANTIC_ERROR
            );
        }

        if (!measurementType.supports(unit)) {
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.INCOMPATIBLE_MEASUREMENT_TYPE
            );
        }

        // Verify the device, sensor, and measurement type relationship.
        var sourceResult = sourceRegistry.validateSource(
                message.deviceId(),
                new SensorId(message.sensorId()),
                measurementType
        );

        switch (sourceResult) {
            case UNKNOWN_DEVICE -> {
                return TelemetryValidationResult.rejected(
                        TelemetryRejectionReason.UNKNOWN_DEVICE
                );
            }
            case UNKNOWN_SENSOR -> {
                return TelemetryValidationResult.rejected(
                        TelemetryRejectionReason.UNKNOWN_SENSOR
                );
            }
            case SENSOR_DEVICE_MISMATCH -> {
                return TelemetryValidationResult.rejected(
                        TelemetryRejectionReason.SENSOR_DEVICE_MISMATCH
                );
            }
            case INCOMPATIBLE_MEASUREMENT_TYPE -> {
                return TelemetryValidationResult.rejected(
                        TelemetryRejectionReason.INCOMPATIBLE_MEASUREMENT_TYPE
                );
            }
            // Other outcomes (such as a valid source) proceed to plausibility validation.
            default -> { }
        }

        // Plausibility is distinct from the greenhouse's preferred growing range.
        if (!plausibilityValidator.isPlausible(measurementType, message.value())) {
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.PHYSICAL_VALUE_OUT_OF_RANGE
            );
        }

        // Duplicate/identity handling belongs to the atomic persistence layer (DT-008).
        return TelemetryValidationResult.accepted();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private MeasurementType parseMeasurementType(String value) {
        try {
            return MeasurementType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Unit parseUnit(String value) {
        try {
            return Unit.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
