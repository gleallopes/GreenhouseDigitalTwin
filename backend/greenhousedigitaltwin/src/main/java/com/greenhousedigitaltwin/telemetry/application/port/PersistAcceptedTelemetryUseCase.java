package com.greenhousedigitaltwin.telemetry.application.port;

import com.greenhousedigitaltwin.telemetry.application.TelemetryPersistenceOutcome;
import com.greenhousedigitaltwin.telemetry.domain.Measurement;

public interface PersistAcceptedTelemetryUseCase {
    TelemetryPersistenceOutcome persist(
            Measurement measurement
    );
}
