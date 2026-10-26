package com.greenhousedigitaltwin.telemetry.application;

import com.greenhousedigitaltwin.telemetry.application.port.MeasurementInsertResult;
import com.greenhousedigitaltwin.telemetry.application.port.MeasurementRepository;
import com.greenhousedigitaltwin.telemetry.domain.Measurement;
import com.greenhousedigitaltwin.twin.application.UpdateTwinStateUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.greenhousedigitaltwin.telemetry.CreateMeasurementHelper.createMeasurement;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PersistAcceptedTelemetryServiceTest {

    @Mock
    MeasurementRepository measurementRepository;

    @InjectMocks
    PersistAcceptedTelemetryService service;

    @Mock
    UpdateTwinStateUseCase updateTwinStateUseCase;


    @Test
    void shouldUpdateTwinWhenMeasurementIsInserted() {
        Measurement measurement = createMeasurement("MEAS-200", "MSG-200");

        when(measurementRepository.insertIfAbsent(measurement))
                .thenReturn(MeasurementInsertResult.INSERTED);

        TelemetryPersistenceOutcome outcome =
                service.persist(measurement);

        assertThat(outcome)
                .isEqualTo(TelemetryPersistenceOutcome.PERSISTED);

        InOrder order = inOrder(
                measurementRepository,
                updateTwinStateUseCase
        );

        order.verify(measurementRepository)
                .insertIfAbsent(measurement);

        order.verify(updateTwinStateUseCase)
                .updateFrom(measurement);
    }

    @Test
    void shouldSkipTwinUpdateWhenMessageAlreadyExists() {
        Measurement measurement = createMeasurement("MEAS-200", "MSG-200");

        when(measurementRepository.insertIfAbsent(measurement))
                .thenReturn(MeasurementInsertResult.ALREADY_EXISTS);

        TelemetryPersistenceOutcome outcome =
                service.persist(measurement);

        assertThat(outcome)
                .isEqualTo(TelemetryPersistenceOutcome.ALREADY_PROCESSED);

        verify(measurementRepository)
                .insertIfAbsent(measurement);

        verifyNoInteractions(updateTwinStateUseCase);
    }

    @Test
    void shouldPropagateTwinUpdateFailure() {
        Measurement measurement = createMeasurement("MEAS-200", "MSG-200");

        when(measurementRepository.insertIfAbsent(measurement))
                .thenReturn(MeasurementInsertResult.INSERTED);

        when(updateTwinStateUseCase.updateFrom(measurement))
                .thenThrow(new IllegalStateException("Twin update failed"));

        assertThatThrownBy(() -> service.persist(measurement))
                .isInstanceOf(IllegalStateException.class);
    }
}
