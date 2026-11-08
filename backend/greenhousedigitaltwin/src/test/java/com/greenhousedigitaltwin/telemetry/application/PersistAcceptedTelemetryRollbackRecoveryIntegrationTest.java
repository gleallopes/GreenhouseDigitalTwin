package com.greenhousedigitaltwin.telemetry.application;

import com.greenhousedigitaltwin.telemetry.domain.Measurement;
import com.greenhousedigitaltwin.twin.application.UpdateTwinStateUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.greenhousedigitaltwin.telemetry.CreateMeasurementHelper.createMeasurement;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@Testcontainers
public class PersistAcceptedTelemetryRollbackRecoveryIntegrationTest {

    @MockitoBean
    private UpdateTwinStateUseCase updateTwinStateUseCase;

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
    void shouldRecoverWhenConcurrentTransactionRollsBack()
            throws Exception {

        Measurement measurement =
                createMeasurement("MEAS-500", "MSG-500");

        CountDownLatch firstInsideTwin = new CountDownLatch(1);
        CountDownLatch allowFirstToFail = new CountDownLatch(1);

        AtomicInteger calls = new AtomicInteger();

        Mockito.doAnswer(invocation -> {
            if (calls.incrementAndGet() == 1) {
                firstInsideTwin.countDown();

                if (!allowFirstToFail.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Timed out waiting to trigger rollback"
                    );
                }

                throw new IllegalStateException(
                        "Simulated Twin update failure"
                );
            }

            return true;
        }).when(updateTwinStateUseCase).updateFrom(any());

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<TelemetryPersistenceOutcome> first =
                    executor.submit(() -> service.persist(measurement));

            assertTrue(
                    firstInsideTwin.await(10, TimeUnit.SECONDS)
            );

            CountDownLatch secondStarted = new CountDownLatch(1);

            Future<TelemetryPersistenceOutcome> second =
                    executor.submit(() -> {
                        secondStarted.countDown();
                        return service.persist(measurement);
                    });

            assertTrue(
                    secondStarted.await(10, TimeUnit.SECONDS)
            );

            // Allow the first transaction to roll back.
            allowFirstToFail.countDown();

            ExecutionException failure = assertThrows(
                    ExecutionException.class,
                    () -> first.get(10, TimeUnit.SECONDS)
            );

            assertInstanceOf(
                    IllegalStateException.class,
                    failure.getCause()
            );

            assertEquals(
                    TelemetryPersistenceOutcome.PERSISTED,
                    second.get(10, TimeUnit.SECONDS)
            );

            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM measurement
                    WHERE message_id = ?
                    """,
                    Integer.class,
                    "MSG-500"
            );

            assertEquals(1, count);

        } finally {
            allowFirstToFail.countDown();
            executor.shutdownNow();
        }
    }
}
