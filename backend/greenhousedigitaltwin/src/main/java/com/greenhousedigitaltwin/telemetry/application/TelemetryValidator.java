package com.greenhousedigitaltwin.telemetry.application;

import com.greenhousedigitaltwin.telemetry.domain.MeasurementType;
import com.greenhousedigitaltwin.telemetry.domain.Unit;

public final class TelemetryValidator {
    public TelemetryValidationResult validate(TelemetryMessage message){

//      Structure Validation
        if (message == null){
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.STRUCTURAL_ERROR
            );
        }

        if (message.messageId() == null || message.messageId().isBlank()){
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.STRUCTURAL_ERROR
            );
        }

        if (message.deviceId() == null || message.deviceId().isBlank()){
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.STRUCTURAL_ERROR
            );
        }

        if (message.sensorId() == null || message.sensorId().isBlank()){
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.STRUCTURAL_ERROR
            );
        }

        if (message.measurementType() == null || message.measurementType().isBlank()){
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.STRUCTURAL_ERROR
            );
        }

        if (message.unit() == null || message.unit().isBlank()){
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.STRUCTURAL_ERROR
            );
        }

        if (message.measuredAt() == null){
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.INVALID_TIMESTAMP
            );
        }

        if (!Double.isFinite(message.value())){
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.STRUCTURAL_ERROR
            );
        }

//        Semantic validation
        MeasurementType measurementType = parseMeasurementType(message.measurementType());

        if (measurementType == null){
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.SEMANTIC_ERROR
            );
        }

        Unit unit = parseUnit(message.unit());

        if (unit == null){
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.SEMANTIC_ERROR
            );
        }

        if (!measurementType.supports(unit)){
            return TelemetryValidationResult.rejected(
                    TelemetryRejectionReason.INCOMPATIBLE_MEASUREMENT_TYPE
            );
        }

        return TelemetryValidationResult.accepted();
    }

    private MeasurementType parseMeasurementType(String value){
        try {
            return MeasurementType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Unit parseUnit(String value){
        try {
            return Unit.valueOf(value.trim().toUpperCase());
        }catch (IllegalArgumentException e) {
            return null;
        }
    }
}
