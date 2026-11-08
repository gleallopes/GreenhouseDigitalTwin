package com.greenhousedigitaltwin.telemetry.application;

import com.greenhousedigitaltwin.telemetry.application.port.IngestTelemetryUseCase;
import com.greenhousedigitaltwin.telemetry.application.port.PersistAcceptedTelemetryUseCase;
import com.greenhousedigitaltwin.telemetry.domain.Measurement;

import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class IngestTelemetryService
        implements IngestTelemetryUseCase {

    private final TelemetryMeasurementFactory factory;
    private final TelemetryValidator validator;
    private final PersistAcceptedTelemetryUseCase persistence;

    public IngestTelemetryService(
            TelemetryMeasurementFactory factory,
            TelemetryValidator validator,
            PersistAcceptedTelemetryUseCase persistence
    ) {
        this.factory = factory;
        this.validator = validator;
        this.persistence = persistence;
    }

    @Override
    public TelemetryIngestionOutcome ingest(
            IngestTelemetryCommand command
    ) {
        Objects.requireNonNull(command, "command");

        // Step 1: Convert the command into a validation message.
        TelemetryMessage message = toTelemetryMessage(command);

        // Step 2: Validate before constructing the domain entity.
        TelemetryValidationResult validationResult =
                validator.validate(message);

        if (!validationResult.valid()) {
            throw new TelemetryValidationException(
                    validationResult.rejectionReason()
            );
        }

        // Step 3: Construct a valid domain measurement.
        Measurement measurement = factory.create(command);

        // Step 4: Delegate transactional persistence.
        TelemetryPersistenceOutcome outcome =
                persistence.persist(measurement);

        // Step 5: Map the persistence result.
        return switch (outcome) {
            case PERSISTED ->
                    TelemetryIngestionOutcome.PERSISTED;

            case ALREADY_PROCESSED ->
                    TelemetryIngestionOutcome.ALREADY_PROCESSED;
        };
    }

    private TelemetryMessage toTelemetryMessage(
            IngestTelemetryCommand command
    ) {
        return new TelemetryMessage(
                command.messageId(),
                command.deviceId(),
                command.sensorId(),
                command.measurementType(),
                command.value(),
                command.unit(),
                command.measuredAt()
        );
    }
}