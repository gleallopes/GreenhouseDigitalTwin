package com.greenhousedigitaltwin.telemetry.application;

import com.greenhousedigitaltwin.telemetry.application.port.PersistAcceptedTelemetryUseCase;
import com.greenhousedigitaltwin.telemetry.domain.Measurement;
import com.greenhousedigitaltwin.telemetry.domain.MeasurementId;
import com.greenhousedigitaltwin.telemetry.domain.MessageId;
import com.greenhousedigitaltwin.greenhouse.domain.SensorId;
import com.greenhousedigitaltwin.telemetry.domain.MeasurementType;
import com.greenhousedigitaltwin.telemetry.domain.Unit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * DT-009: Tests application orchestration without starting Spring or PostgreSQL.
 * Adjust domain imports if your project's packages differ.
 */
@ExtendWith(MockitoExtension.class)
class IngestTelemetryServiceTest {

    @Mock
    private TelemetryMeasurementFactory factory;

    @Mock
    private TelemetryValidator validator;

    @Mock
    private PersistAcceptedTelemetryUseCase persistence;

    @InjectMocks
    private IngestTelemetryService service;

    @Test
    void shouldIngestValidTelemetry() {
        IngestTelemetryCommand command = createValidCommand();
        Measurement measurement = createValidMeasurement();

        when(validator.validate(any(TelemetryMessage.class)))
                .thenReturn(TelemetryValidationResult.accepted());

        when(factory.create(command))
                .thenReturn(measurement);

        when(persistence.persist(measurement))
                .thenReturn(TelemetryPersistenceOutcome.PERSISTED);

        TelemetryIngestionOutcome result = service.ingest(command);

        assertEquals(TelemetryIngestionOutcome.PERSISTED, result);

        verify(validator).validate(any(TelemetryMessage.class));
        verify(factory).create(command);
        verify(persistence).persist(measurement);
    }

    @Test
    void shouldNotPersistInvalidTelemetry() {
        IngestTelemetryCommand command = createValidCommand();

        when(validator.validate(any(TelemetryMessage.class)))
                .thenReturn(
                        TelemetryValidationResult.rejected(
                                TelemetryRejectionReason.STRUCTURAL_ERROR
                        )
                );

        TelemetryValidationException exception =
                assertThrows(
                        TelemetryValidationException.class,
                        () -> service.ingest(command)
                );

        assertEquals(
                TelemetryRejectionReason.STRUCTURAL_ERROR,
                exception.reason()
        );

        verify(validator).validate(any(TelemetryMessage.class));

        verifyNoInteractions(factory, persistence);
    }

    @Test
    void shouldAcknowledgeAlreadyProcessedTelemetry() {
        IngestTelemetryCommand command = createValidCommand();
        Measurement measurement = createValidMeasurement();

        when(validator.validate(any(TelemetryMessage.class)))
                .thenReturn(TelemetryValidationResult.accepted());

        when(factory.create(command))
                .thenReturn(measurement);

        when(persistence.persist(measurement))
                .thenReturn(TelemetryPersistenceOutcome.ALREADY_PROCESSED);

        TelemetryIngestionOutcome result = service.ingest(command);

        assertEquals(
                TelemetryIngestionOutcome.ALREADY_PROCESSED,
                result
        );

        verify(validator).validate(any(TelemetryMessage.class));
        verify(factory).create(command);
        verify(persistence).persist(measurement);
    }

    private IngestTelemetryCommand createValidCommand() {
        return new IngestTelemetryCommand(
                "MEAS-901",   // measurementId
                "MSG-901",    // messageId
                "ARD-001",    // deviceId (NEW)
                "TEMP-001",   // sensorId
                "TEMPERATURE",
                25.4,
                "CELSIUS",
                Instant.parse("2026-11-06T14:00:00Z")
        );
    }

    private Measurement createValidMeasurement() {
        return new Measurement(
                new MeasurementId("MEAS-901"),
                new MessageId("MSG-901"),
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE,
                25.4,
                Unit.CELSIUS,
                Instant.parse("2026-11-06T14:00:00Z"),
                Instant.parse("2026-11-06T14:00:01Z")
        );
    }
}
