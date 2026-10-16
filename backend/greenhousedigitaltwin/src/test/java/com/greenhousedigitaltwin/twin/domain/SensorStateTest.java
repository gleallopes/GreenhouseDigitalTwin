package com.greenhousedigitaltwin.twin.domain;

import com.greenhousedigitaltwin.greenhouse.domain.SensorId;
import com.greenhousedigitaltwin.telemetry.domain.MeasurementType;
import com.greenhousedigitaltwin.telemetry.domain.Unit;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SensorStateTest {

    private SensorState state(String timestamp){
        return new SensorState(
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE,
                25.4,
                Unit.CELSIUS,
                Instant.parse(timestamp),
                Instant.parse("2026-10-16T12:00:00Z")
        );
    }

    @Test
    void shouldRecognizeNewerObservations(){
        SensorState current = state("2026-10-16T10:00:00Z");

        SensorState incoming = state("2026-10-16T10:05:00Z");

        assertThat(incoming.isNewerThan(current)).isTrue();
    }

    @Test
    void shouldRejectOlderObservationsAsNewer(){
        SensorState current = state("2026-10-16T10:05:00Z");

        SensorState incoming = state("2026-10-16T10:02:00Z");

        assertThat(incoming.isNewerThan(current)).isFalse();
    }

    @Test
    void shouldNotReplaceEqualTimestamp(){
        SensorState current = state("2026-10-16T10:05:00Z");
        SensorState incoming = state("2026-10-16T10:05:00Z");

        assertThat(incoming.isNewerThan(current)).isFalse();
    }

    @Test
    void shouldRejectComparisonBetweenDifferentSensors(){
        SensorState current = state("2026-10-16T10:05:00Z");

        SensorState incoming = new SensorState(
                new SensorId("HUM-001"),
                MeasurementType.HUMIDITY,
                70.0,
                Unit.PERCENT,
                Instant.parse("2026-10-16T10:05:00Z"),
                Instant.parse("2026-10-16T12:00:00Z")
        );

        assertThatThrownBy(() -> incoming.isNewerThan(current))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
