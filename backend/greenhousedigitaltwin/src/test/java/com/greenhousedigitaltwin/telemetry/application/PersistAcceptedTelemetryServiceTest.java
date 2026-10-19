package com.greenhousedigitaltwin.telemetry.application;

import com.greenhousedigitaltwin.telemetry.application.port.MeasurementRepository;
import com.greenhousedigitaltwin.telemetry.domain.Measurement;
import com.greenhousedigitaltwin.twin.application.UpdateTwinStateUseCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.*;

public class PersistAcceptedTelemetryServiceTest {

    @Test
    void shouldPersistMeasurementAndUpdateTwinState(){
        MeasurementRepository measurementRepository = mock(MeasurementRepository.class);

        UpdateTwinStateUseCase updateTwinStateUseCase = mock(UpdateTwinStateUseCase.class);

        PersistAcceptedTelemetryService service =
                new PersistAcceptedTelemetryService(measurementRepository, updateTwinStateUseCase);

        Measurement measurement = mock(Measurement.class);

        service.persist(measurement);

        var order = inOrder(
                measurementRepository,
                updateTwinStateUseCase
        );

        order.verify(measurementRepository).save(measurement);
        order.verify(updateTwinStateUseCase).updateFrom(measurement);
    }

    @Test
    void shouldPropagateTwinUpdateFailure(){
        MeasurementRepository measurementRepository = mock(MeasurementRepository.class);
        UpdateTwinStateUseCase updateTwinStateUseCase = mock(UpdateTwinStateUseCase.class);

        PersistAcceptedTelemetryService service =
                new PersistAcceptedTelemetryService(measurementRepository, updateTwinStateUseCase);

        Measurement measurement = mock(Measurement.class);

        when(updateTwinStateUseCase.updateFrom(measurement))
                .thenThrow(new IllegalStateException("Twin State persistence failed"));

        assertThatThrownBy(() -> service.persist(measurement))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldAcceptHistoricalMeasurementWhenTwinIsNotUpdated(){
        MeasurementRepository measurementRepository = mock(MeasurementRepository.class);
        UpdateTwinStateUseCase updateTwinStateUseCase = mock(UpdateTwinStateUseCase.class);

        PersistAcceptedTelemetryService service =
                new PersistAcceptedTelemetryService(measurementRepository, updateTwinStateUseCase);

        Measurement measurement = mock(Measurement.class);

        when(updateTwinStateUseCase.updateFrom(measurement))
                .thenReturn(false);

        service.persist(measurement);

        verify(measurementRepository).save(measurement);
        verify(updateTwinStateUseCase).updateFrom(measurement);
    }
}
