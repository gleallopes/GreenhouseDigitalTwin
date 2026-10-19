package com.greenhousedigitaltwin.twin.infrastructure.persistence;

import com.greenhousedigitaltwin.greenhouse.domain.SensorId;
import com.greenhousedigitaltwin.telemetry.domain.MeasurementType;
import com.greenhousedigitaltwin.telemetry.domain.Unit;
import com.greenhousedigitaltwin.twin.domain.SensorState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.*;

@Testcontainers
@SpringBootTest
public class PostgresTwinStateRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =  new PostgreSQLContainer("postgres:17");

    @Autowired
    private PostgresTwinStateRepository repository;

    @Autowired
    private SpringDataTwinStateRepository springRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup(){
        jdbcTemplate.update("DELETE FROM twin_state");
        jdbcTemplate.update("DELETE FROM measurement");
        jdbcTemplate.update("DELETE FROM sensor");
        jdbcTemplate.update("DELETE FROM device");
        jdbcTemplate.update("DELETE FROM greenhouse");

        jdbcTemplate.update("""
            INSERT INTO greenhouse (id, name)
            VALUES (?, ?)
            """, "GH-001", "Apartment Mint Greenhouse");

        jdbcTemplate.update("""
            INSERT INTO device (id, greenhouse_id, name)
            VALUES (?, ?, ?)
            """, "ARD-001", "GH-001", "Arduino Controller");

        jdbcTemplate.update("""
            INSERT INTO sensor (id, device_id, measurement_type, unit)
            VALUES (?, ?, ?, ?)
            """, "TEMP-001", "ARD-001", "TEMPERATURE", "CELSIUS");
    }

    @Test
    void shouldInsertInitialSensorState(){
        SensorState incoming = state(24.0, "2026-10-19T10:00:00Z");

        boolean updated = repository.updateIfNewer(incoming);

        assertThat(updated).isTrue();

        TwinStateEntity persisted = springRepository
                .findById("TEMP-001")
                .orElseThrow();

        assertThat(persisted.getValue()).isEqualTo(24.0);
        assertThat(persisted.getMeasuredAt()).isEqualTo(incoming.measuredAt());
    }

    @Test
    void shouldUpdateStateWhenMeasurementIsNewer(){
        repository.updateIfNewer(state(24.0, "2026-10-19T10:00:00Z"));

        boolean updated = repository.updateIfNewer(state(26.0, "2026-10-19T10:05:00Z"));

        assertThat(updated).isTrue();

        TwinStateEntity persisted = springRepository
                .findById("TEMP-001")
                .orElseThrow();

        assertThat(persisted.getValue()).isEqualTo(26.0);
        assertThat(persisted.getMeasuredAt()).isEqualTo(Instant.parse("2026-10-19T10:05:00Z"));

        assertThat(springRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldNotOverwriteStateWithOlderMeasurement(){
        repository.updateIfNewer(state(26.0, "2026-10-19T10:05:00Z"));

        boolean updated = repository.updateIfNewer(state(25.0, "2026-10-19T10:02:00Z"));

        assertThat(updated).isFalse();

        TwinStateEntity persisted = springRepository
                .findById("TEMP-001")
                .orElseThrow();

        assertThat(persisted.getValue()).isEqualTo(26.0);

        assertThat(persisted.getMeasuredAt()).isEqualTo(Instant.parse("2026-10-19T10:05:00Z"));
    }

    @Test
    void shouldNotOverwriteEqualTimestamp(){
        repository.updateIfNewer(state(26.0, "2026-10-19T10:05:00Z"));

        boolean updated = repository.updateIfNewer(
                state(99.0, "2026-10-19T10:05:00Z")
        );

        assertThat(updated).isFalse();

        TwinStateEntity persisted = springRepository
                .findById("TEMP-001")
                .orElseThrow();

        assertThat(persisted.getValue()).isEqualTo(26.0);
    }

    @Test
    void shouldPreserveNewerStateUnderConcurrentUpdates() throws Exception {
        SensorState newer = state(27.0, "2026-10-19T10:07:00Z");
        SensorState older = state(25.0, "2026-10-19T10:03:00Z");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<Boolean> newerTask = () -> {
                ready.countDown();
                start.await();
                return repository.updateIfNewer(newer);
            };
            Callable<Boolean> olderTask = () -> {
                ready.countDown();
                start.await();
                return repository.updateIfNewer(older);
            };

            Future<Boolean> newerResult = executor.submit(newerTask);
            Future<Boolean> olderResult = executor.submit(olderTask);

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            newerResult.get(10, TimeUnit.SECONDS);
            olderResult.get(10, TimeUnit.SECONDS);

            TwinStateEntity persisted = springRepository
                    .findById("TEMP-001")
                    .orElseThrow();

            assertThat(persisted.getValue()).isEqualTo(27.0);

            assertThat(persisted.getMeasuredAt()).isEqualTo(Instant.parse("2026-10-19T10:07:00Z"));

            assertThat(springRepository.count()).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private SensorState state(double value, String measuredAt){
        return new SensorState (
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE,
                value,
                Unit.CELSIUS,
                Instant.parse(measuredAt),
                Instant.parse("2026-10-19T12:00:00Z")

        );
    }


}
