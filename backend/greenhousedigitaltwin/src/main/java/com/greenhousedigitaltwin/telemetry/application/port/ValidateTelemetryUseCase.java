package com.greenhousedigitaltwin.telemetry.application.port;

import com.greenhousedigitaltwin.telemetry.domain.Measurement;

public interface ValidateTelemetryUseCase {
    void validate(Measurement measurement);
}
