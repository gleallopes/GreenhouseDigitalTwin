package com.greenhousedigitaltwin.telemetry.application;

public final class TelemetryValidator {
    public TelemetryValidationResult validate(TelemetryMessage message){

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


        return TelemetryValidationResult.accepted();
    }
}
