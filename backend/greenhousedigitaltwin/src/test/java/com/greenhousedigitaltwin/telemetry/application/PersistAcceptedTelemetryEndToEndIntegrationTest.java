package com.greenhousedigitaltwin.telemetry.application;

import com.greenhousedigitaltwin.greenhouse.domain.SensorId;
import com.greenhousedigitaltwin.telemetry.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;

import static com.greenhousedigitaltwin.telemetry.CreateMeasurementHelper.createMeasurement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
public class PersistAcceptedTelemetryEndToEndIntegrationTest {
    @Autowired
    private PersistAcceptedTelemetryService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
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
    void shouldPersistMeasurementAndUpdateTwinStateAtomically() {
        Measurement measurement =
                createMeasurement("MEAS-200", "MSG-200");

        service.persist(measurement);

        Integer measurementCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM measurement
                WHERE message_id = ?
                """,
                Integer.class,
                "MSG-200"
        );

        Double currentTemperature = jdbcTemplate.queryForObject(
                """
                SELECT value
                FROM twin_state
                WHERE sensor_id = ?
                """,
                Double.class,
                "TEMP-001"
        );

        assertThat(measurementCount).isEqualTo(1);
        assertThat(currentTemperature).isEqualTo(25.4);
    }

    @Test
    void shouldPreserveOlderMeasurementWithoutRegressingTwinState() {

        Measurement newer = new Measurement(
                new MeasurementId("MEAS-201"),
                new MessageId("MSG-201"),
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE,
                27.0,
                Unit.CELSIUS,
                Instant.parse("2026-10-19T10:07:00Z"),
                Instant.parse("2026-10-19T10:07:01Z")
        );

        Measurement older = new Measurement(
                new MeasurementId("MEAS-202"),
                new MessageId("MSG-202"),
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE,
                25.0,
                Unit.CELSIUS,
                Instant.parse("2026-10-19T10:03:00Z"),
                Instant.parse("2026-10-19T10:08:00Z")
        );

        service.persist(newer);
        service.persist(older);

        Integer historyCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM measurement
                WHERE sensor_id = ?
                """,
                Integer.class,
                "TEMP-001"
        );

        Double currentTemperature = jdbcTemplate.queryForObject(
                """
                SELECT value
                FROM twin_state
                WHERE sensor_id = ?
                """,
                Double.class,
                "TEMP-001"
        );

        assertThat(historyCount).isEqualTo(2);
        assertThat(currentTemperature).isEqualTo(27.0);
    }

    @Test
    void shouldProcessConcurrentDuplicateMessagesOnlyOnce()
            throws Exception {

        Measurement measurement =
                createMeasurement("MEAS-500", "MSG-500");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Callable<TelemetryPersistenceOutcome> task = () -> {
                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Timed out waiting to start"
                    );
                }

                return service.persist(measurement);
            };

            Future<TelemetryPersistenceOutcome> first =
                    executor.submit(task);

            Future<TelemetryPersistenceOutcome> second =
                    executor.submit(task);

            assertThat(ready.await(10, TimeUnit.SECONDS))
                    .isTrue();

            start.countDown();

            TelemetryPersistenceOutcome firstResult =
                    first.get(20, TimeUnit.SECONDS);

            TelemetryPersistenceOutcome secondResult =
                    second.get(20, TimeUnit.SECONDS);

            assertThat(List.of(firstResult, secondResult))
                    .containsExactlyInAnyOrder(
                            TelemetryPersistenceOutcome.PERSISTED,
                            TelemetryPersistenceOutcome.ALREADY_PROCESSED
                    );

            Integer measurementCount =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM measurement
                            WHERE message_id = ?
                            """,
                            Integer.class,
                            "MSG-500"
                    );

            Integer twinStateCount =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM twin_state
                            WHERE sensor_id = ?
                            """,
                            Integer.class,
                            "TEMP-001"
                    );

            Double currentTemperature =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT value
                            FROM twin_state
                            WHERE sensor_id = ?
                            """,
                            Double.class,
                            "TEMP-001"
                    );

            assertThat(measurementCount).isEqualTo(1);
            assertThat(twinStateCount).isEqualTo(1);
            assertThat(currentTemperature).isEqualTo(25.4);

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void shouldProcessDifferentMessagesAsIdentityConflict()
    throws Exception {

        Measurement first =
                createMeasurement("MEAS-500", "MSG-500");

        Measurement second =
                createMeasurement("MEAS-500", "MSG-501");

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<TelemetryPersistenceOutcome> firstTask = () -> {
                ready.countDown();
                start.await();
                return service.persist(first);
            };

            Callable<TelemetryPersistenceOutcome> secondTask = () -> {
                ready.countDown();
                start.await();
                return service.persist(second);
            };

            Future<TelemetryPersistenceOutcome> firstFuture =
                    executor.submit(firstTask);

            Future<TelemetryPersistenceOutcome> secondFuture =
                    executor.submit(secondTask);

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            int persisted = 0;
            int conflicts = 0;

            for (Future<TelemetryPersistenceOutcome> future :
                    List.of(firstFuture, secondFuture)) {
                try {
                    TelemetryPersistenceOutcome outcome =
                            future.get(10, TimeUnit.SECONDS);

                    if (outcome == TelemetryPersistenceOutcome.PERSISTED) {
                        persisted++;
                    }

                } catch (ExecutionException exception) {

                    assertInstanceOf(
                            InvalidDataAccessApiUsageException.class,
                            exception.getCause()
                    );

                    conflicts++;
                }
            }

            assertEquals(1, persisted);
            assertEquals(1, conflicts);

            Integer measurementCount =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM measurement
                            WHERE measurement_id = ?
                            """,
                            Integer.class,
                            "MEAS-500"
                    );

            Integer twinStateCount =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM twin_state
                            WHERE sensor_id = ?
                            """,
                            Integer.class,
                            "TEMP-001"
                    );

            Double currentTemperature =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT value
                            FROM twin_state
                            WHERE sensor_id = ?
                            """,
                            Double.class,
                            "TEMP-001"
                    );

            assertThat(measurementCount).isEqualTo(1);
            assertThat(twinStateCount).isEqualTo(1);
            assertThat(currentTemperature).isEqualTo(25.4);

        } finally {
            start.countDown();
            executor.shutdownNow();
        }

    }
}
