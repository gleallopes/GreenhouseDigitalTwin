package com.greenhousedigitaltwin.telemetry.application.port;

import com.greenhousedigitaltwin.telemetry.application.IngestTelemetryCommand;
import com.greenhousedigitaltwin.telemetry.application.TelemetryIngestionOutcome;

public interface IngestTelemetryUseCase {

    TelemetryIngestionOutcome ingest(
            IngestTelemetryCommand command
    );
}
