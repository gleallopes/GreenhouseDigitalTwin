package com.greenhousedigitaltwin.telemetry.infrastructure.persistence;

import com.greenhousedigitaltwin.telemetry.application.port.MeasurementInsertResult;
import com.greenhousedigitaltwin.telemetry.application.port.MeasurementRepository;
import com.greenhousedigitaltwin.telemetry.domain.Measurement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import static com.greenhousedigitaltwin.telemetry.domain.MeasurementTest.createMeasurement;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
@SpringBootTest
public class PostgresMeasurementIdempotencyIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:17");

    @Autowired
    private MeasurementRepository measurementRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.update("DELETE FROM twin_state");
        jdbcTemplate.update("DELETE FROM measurement");
        jdbcTemplate.update("DELETE FROM sensor");
        jdbcTemplate.update("DELETE FROM device");
        jdbcTemplate.update("DELETE FROM greenhouse");

        jdbcTemplate.update(
                "INSERT INTO greenhouse (id, name) VALUES (?, ?)",
                "GH-001",
                "Apartment Mint Greenhouse"
        );

        jdbcTemplate.update(
                """
                INSERT INTO device (id, greenhouse_id, name)
                VALUES (?, ?, ?)
                """,
                "ARD-001",
                "GH-001",
                "Arduino Controller"
        );

        jdbcTemplate.update(
                """
                INSERT INTO sensor (id, device_id, measurement_type, unit)
                VALUES (?, ?, ?, ?)
                """,
                "TEMP-001",
                "ARD-001",
                "TEMPERATURE",
                "CELSIUS"
        );
    }

    @Test
    void shouldInsertNewMeasurement() {
        Measurement measurement = createMeasurement("MEAS-300", "MSG-300");

        MeasurementInsertResult result = measurementRepository.insertIfAbsent(measurement);

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM measurement
                WHERE message_id = ?
                """,
                Integer.class,
                "MSG-300"
        );

        assertThat(result).isEqualTo(MeasurementInsertResult.INSERTED);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldNotInsertDuplicateMessage() {
        Measurement measurement = createMeasurement("MEAS-301", "MSG-301");
        Measurement duplicate = createMeasurement("MEAS-302", "MSG-301");

        MeasurementInsertResult firstResult = measurementRepository.insertIfAbsent(measurement);
        MeasurementInsertResult secondResult = measurementRepository.insertIfAbsent(duplicate);


        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM measurement
                        WHERE message_id = ?
                        """,
                Integer.class,
                "MSG-301"
        );

        assertThat(firstResult).isEqualTo(MeasurementInsertResult.INSERTED);
        assertThat(secondResult).isEqualTo(MeasurementInsertResult.ALREADY_EXISTS);
        assertThat(count).isEqualTo(1);

    }

    @Test
    void shouldRejectDuplicateMeasurementIdWithDifferentMessageId() {
        Measurement first =
                createMeasurement("MEAS-303", "MSG-303");

        Measurement conflicting =
                createMeasurement("MEAS-303", "MSG-304");

        measurementRepository.insertIfAbsent(first);

        assertThatThrownBy(
                () -> measurementRepository.insertIfAbsent(conflicting)
        ).isInstanceOf(DataIntegrityViolationException.class);

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM measurement
                WHERE measurement_id = ?
                """,
                Integer.class,
                "MEAS-303"
        );

        assertThat(count).isEqualTo(1);
    }
}
