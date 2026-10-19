package com.greenhousedigitaltwin.twin.application;

import com.greenhousedigitaltwin.greenhouse.domain.SensorId;
import com.greenhousedigitaltwin.telemetry.domain.*;
import com.greenhousedigitaltwin.twin.application.port.TwinStateRepository;
import com.greenhousedigitaltwin.twin.domain.SensorState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UpdateTwinStateServiceTest {

    private TwinStateRepository repository;
    private UpdateTwinStateService service;

    @BeforeEach
    void setUp(){
        repository = mock(TwinStateRepository.class);
        service = new UpdateTwinStateService(repository);
    }

    private Measurement measurement(){
        return new Measurement(
                new MeasurementId("MEAS-001"),
                new MessageId("MSG-001"),
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE,
                25.4,
                Unit.CELSIUS,
                Instant.parse("2026-10-16T10:00:00Z"),
                Instant.parse("2026-10-16T10:00:01Z")
        );
    }

    @Test
    void shouldConvertMeasurementIntoSensorState(){
        when(repository.updateIfNewer(any(SensorState.class)))
                .thenReturn(true);

        boolean updated = service.updateFrom(measurement());

        assertThat(updated).isTrue();

        verify(repository).updateIfNewer(
                argThat(state ->
                        state.sensorId().equals(new SensorId("TEMP-001"))
                        && state.measurementType() == MeasurementType.TEMPERATURE
                        && state.value() == 25.4
                        && state.unit() == Unit.CELSIUS
                        && state.measuredAt().equals(Instant.parse("2026-10-16T10:00:00Z"))
                        )
        );
    }

    @Test
    void shouldReturnFalseWhenRepositoryDoesNotUpdateState(){
        when(repository.updateIfNewer(any(SensorState.class))).thenReturn(false);

        boolean updated = service.updateFrom(measurement());

        assertThat(updated).isFalse();

        verify(repository, times(1)).updateIfNewer(any(SensorState.class));
    }

    @Test
    void shouldPropagatePersistenceFailure(){
        when(repository.updateIfNewer(any(SensorState.class)))
                .thenThrow(new IllegalStateException("Database unavailable"));

        assertThatThrownBy(() -> service.updateFrom(measurement()))
                .isInstanceOf(IllegalStateException.class);
    }

}
