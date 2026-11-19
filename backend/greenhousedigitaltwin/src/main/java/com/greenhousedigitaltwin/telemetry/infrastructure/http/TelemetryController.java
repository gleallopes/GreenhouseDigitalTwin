package com.greenhousedigitaltwin.telemetry.infrastructure.http;

import com.greenhousedigitaltwin.telemetry.application.TelemetryIngestionOutcome;
import com.greenhousedigitaltwin.telemetry.application.port.IngestTelemetryUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TelemetryController {

    private final IngestTelemetryUseCase ingestion;

    public TelemetryController(IngestTelemetryUseCase ingestion) {
        this.ingestion = ingestion;
    }

    @PostMapping(path = "/api/v1/telemetry", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TelemetryResponse> ingest(@Valid @RequestBody TelemetryRequest request) {
        TelemetryIngestionOutcome outcome = ingestion.ingest(request.toCommand());
        HttpStatus status = switch (outcome) {
            case PERSISTED -> HttpStatus.CREATED;
            case ALREADY_PROCESSED -> HttpStatus.OK;
        };
        return ResponseEntity.status(status).body(new TelemetryResponse(outcome));
    }
}
