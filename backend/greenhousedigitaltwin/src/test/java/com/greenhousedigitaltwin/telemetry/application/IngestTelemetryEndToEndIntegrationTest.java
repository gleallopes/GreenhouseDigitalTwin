package com.greenhousedigitaltwin.telemetry.application;

import com.greenhousedigitaltwin.telemetry.application.port.IngestTelemetryUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class IngestTelemetryEndToEndIntegrationTest {

    @Autowired
    private IngestTelemetryUseCase ingestion;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM twin_state");
        jdbcTemplate.update("DELETE FROM measurement");
        jdbcTemplate.update("DELETE FROM sensor");
        jdbcTemplate.update("DELETE FROM device");
        jdbcTemplate.update("DELETE FROM greenhouse");
        jdbcTemplate.update(
                """
                INSERT INTO greenhouse (id, name)
                VALUES (?, ?)
                ON CONFLICT (id) DO NOTHING
                """,
                "GH-E2E-001", "Integration Test Greenhouse"
        );

        jdbcTemplate.update(
                """
                INSERT INTO device (id, greenhouse_id, name)
                VALUES (?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """,
                "ARD-E2E-001", "GH-E2E-001", "Test Arduino"
        );

        jdbcTemplate.update(
                """
                INSERT INTO sensor (id, device_id, measurement_type, unit)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """,
                "TEMP-E2E-001", "ARD-E2E-001",
                "TEMPERATURE", "CELSIUS"
        );
    }

    @Test
    void shouldPersistValidTelemetryAndUpdateTwinState(){
        IngestTelemetryCommand command = createValidCommand();

        TelemetryIngestionOutcome outcome = ingestion.ingest(command);

        assertEquals(
                TelemetryIngestionOutcome.PERSISTED,
                outcome
        );

        Integer countDBUpdate = jdbcTemplate.queryForObject(
                """
               SELECT COUNT(*)
               FROM measurement
               WHERE message_id = ?
                 AND sensor_id = ?
                 AND measurement_type = ?
                 AND value = ?
                 AND unit = ?
                 AND measured_at = ?
               """,
                Integer.class,
                "MSG-E2E-001",
                "TEMP-E2E-001",
                "TEMPERATURE",
                25.4,
                "CELSIUS",
                java.sql.Timestamp.from(
                        Instant.parse("2026-11-09T12:00:00Z")
                )
        );

        Integer countTwin = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM twin_state
                WHERE sensor_id = ?
                    AND value = ?
                    AND measured_at = ?
                """,
                Integer.class,
                "TEMP-E2E-001",
                25.4,
                java.sql.Timestamp.from(
                        Instant.parse("2026-11-09T12:00:00Z")
                )
        );
        assertEquals(1, countDBUpdate);
        assertEquals(1, countTwin);

    }

    @Test
    void shouldAcknowledgeIdenticalRetryWithoutDuplicatingMeasurement(){
        IngestTelemetryCommand command = createValidCommand();

        TelemetryIngestionOutcome outcomeFirst = ingestion.ingest(command);

        assertEquals(
                TelemetryIngestionOutcome.PERSISTED,
                outcomeFirst
        );

        TelemetryIngestionOutcome outcomeSecond = ingestion.ingest(command);

        assertEquals(
                TelemetryIngestionOutcome.ALREADY_PROCESSED,
                outcomeSecond
        );

        Integer countDBUpdate = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM measurement
                WHERE message_id = ?
                """,
                Integer.class,
                command.messageId()
        );

        Integer countTwin = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM twin_state
                WHERE sensor_id = ?
                    AND value = ?
                    AND measured_at = ?
                """,
                Integer.class,
                "TEMP-E2E-001",
                25.4,
                java.sql.Timestamp.from(
                        Instant.parse("2026-11-09T12:00:00Z")
                )
        );
        assertEquals(1, countDBUpdate);
        assertEquals(1, countTwin);

    }

    @Test
    void shouldPreserveNewestTwinStateWhenOlderTelemetryArrives() {

        // Arrange: newer measurement
        IngestTelemetryCommand newer = new IngestTelemetryCommand(
                "MEAS-E2E-002",
                "MSG-E2E-002",
                "ARD-E2E-001",
                "TEMP-E2E-001",
                "TEMPERATURE",
                27.0,
                "CELSIUS",
                Instant.parse("2026-11-09T12:05:00Z")
        );

        // Arrange: older measurement
        IngestTelemetryCommand older = new IngestTelemetryCommand(
                "MEAS-E2E-003",
                "MSG-E2E-003",
                "ARD-E2E-001",
                "TEMP-E2E-001",
                "TEMPERATURE",
                24.0,
                "CELSIUS",
                Instant.parse("2026-11-09T12:03:00Z")
        );

        // Act: intentionally ingest in reverse chronological order
        TelemetryIngestionOutcome first = ingestion.ingest(newer);
        TelemetryIngestionOutcome second = ingestion.ingest(older);

        // Assert: both valid measurements are persisted
        assertEquals(TelemetryIngestionOutcome.PERSISTED, first);
        assertEquals(TelemetryIngestionOutcome.PERSISTED, second);

        Integer historyCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM measurement
                WHERE message_id IN (?, ?)
                """,
                Integer.class,
                newer.messageId(),
                older.messageId()
        );

        assertEquals(2, historyCount);

        // Assert: current state still reflects the newer measurement
        Integer currentStateCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM twin_state
                WHERE sensor_id = ?
                  AND value = ?
                  AND measured_at = ?
                """,
                Integer.class,
                newer.sensorId(),
                newer.value(),
                Timestamp.from(newer.measuredAt())
        );

        Instant currentMeasuredAt = jdbcTemplate.queryForObject(
                """
                SELECT measured_at
                FROM twin_state
                WHERE sensor_id = ?
                AND value = ?
                """,
                Instant.class,
                newer.sensorId(),
                newer.value()
        );

        assertEquals(1, currentStateCount);
        assertEquals(newer.measuredAt(), currentMeasuredAt);
    }

    @Test
    void shouldPreserveCurrentStateWhenTimestampsAreEqual() {

        IngestTelemetryCommand first = createValidCommand();

        IngestTelemetryCommand second = new IngestTelemetryCommand(
                "MEAS-E2E-004",
                "MSG-E2E-004",
                "ARD-E2E-001",
                "TEMP-E2E-001",
                "TEMPERATURE",
                26.0,
                "CELSIUS",
                first.measuredAt()
        );

        TelemetryIngestionOutcome firstOutcome =
                ingestion.ingest(first);

        TelemetryIngestionOutcome secondOutcome =
                ingestion.ingest(second);

        assertEquals(
                TelemetryIngestionOutcome.PERSISTED,
                firstOutcome
        );

        assertEquals(
                TelemetryIngestionOutcome.PERSISTED,
                secondOutcome
        );

        Integer historyCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM measurement
                WHERE message_id IN (?, ?)
                """,
                Integer.class,
                first.messageId(),
                second.messageId()
        );

        assertEquals(2, historyCount);

        Integer currentStateCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM twin_state
                WHERE sensor_id = ?
                  AND value = ?
                  AND measured_at = ?
                """,
                Integer.class,
                first.sensorId(),
                first.value(),
                Timestamp.from(first.measuredAt())
        );

        assertEquals(1, currentStateCount);
    }

    @Test
    void shouldRejectUnknownSensorWithoutPersistingTelemetry() {

        IngestTelemetryCommand command = new IngestTelemetryCommand(
                "MEAS-E2E-005",
                "MSG-E2E-005",
                "ARD-E2E-001",
                "TEMP-UNKNOWN",
                "TEMPERATURE",
                25.4,
                "CELSIUS",
                Instant.parse("2026-11-09T12:10:00Z")
        );

        TelemetryRejectionReason reason = assertThrows(
                TelemetryValidationException.class,
                () -> ingestion.ingest(command)
        ).reason();

        Integer measurementCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM measurement
                WHERE message_id = ?
                """,
                Integer.class,
                command.messageId()
        );

        Integer twinStateCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM twin_state
                """,
                Integer.class
        );

        assertEquals(0, measurementCount);
        assertEquals(0, twinStateCount);
        assertEquals(TelemetryRejectionReason.UNKNOWN_SENSOR, reason);
    }

    @Test
    void shouldRejectUnknownDeviceWithoutPersistingTelemetry() {
        IngestTelemetryCommand command = new IngestTelemetryCommand(
                "MEAS-E2E-001",
                "MSG-E2E-001",
                "ARD-UNKNOWN",
                "TEMP-E2E-001",
                "TEMPERATURE",
                25.4,
                "CELSIUS",
                Instant.parse("2026-11-09T12:00:00Z")
        );

        TelemetryRejectionReason reason = assertThrows(
                TelemetryValidationException.class,
                () -> ingestion.ingest(command)
        ).reason();

        Integer measurementCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM measurement
                WHERE message_id = ?
                """,
                Integer.class,
                command.messageId()
        );

        Integer twinStateCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM twin_state
                """,
                Integer.class
        );

        assertEquals(0, measurementCount);
        assertEquals(0, twinStateCount);
        assertEquals(TelemetryRejectionReason.UNKNOWN_DEVICE, reason);
    }

    @Test
    void shouldRejectIncompatibleUnitWithoutPersistingTelemetry() {
        IngestTelemetryCommand command = new IngestTelemetryCommand(
                "MEAS-E2E-001",
                "MSG-E2E-001",
                "ARD-E2E-001",
                "TEMP-E2E-001",
                "TEMPERATURE",
                25.4,
                "LUX",
                Instant.parse("2026-11-09T12:00:00Z")
        );

        TelemetryRejectionReason reason = assertThrows(
                TelemetryValidationException.class,
                () -> ingestion.ingest(command)
        ).reason();

        Integer measurementCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM measurement
                WHERE message_id = ?
                """,
                Integer.class,
                command.messageId()
        );

        Integer twinStateCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM twin_state
                """,
                Integer.class
        );

        assertEquals(0, measurementCount);
        assertEquals(0, twinStateCount);
        assertEquals(TelemetryRejectionReason.INCOMPATIBLE_MEASUREMENT_TYPE, reason);
    }

    @Test
    void shouldRejectImplausibleValueWithoutPersistingTelemetry() {
        IngestTelemetryCommand command = new IngestTelemetryCommand(
                "MEAS-E2E-001",
                "MSG-E2E-001",
                "ARD-E2E-001",
                "TEMP-E2E-001",
                "TEMPERATURE",
                90.0,
                "CELSIUS",
                Instant.parse("2026-11-09T12:00:00Z")
        );

        TelemetryRejectionReason reason = assertThrows(
                TelemetryValidationException.class,
                () -> ingestion.ingest(command)
        ).reason();

        Integer measurementCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM measurement
                WHERE message_id = ?
                """,
                Integer.class,
                command.messageId()
        );

        Integer twinStateCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM twin_state
                """,
                Integer.class
        );

        assertEquals(0, measurementCount);
        assertEquals(0, twinStateCount);
        assertEquals(TelemetryRejectionReason.PHYSICAL_VALUE_OUT_OF_RANGE, reason);

    }

    @Test
    void shouldRejectSensorDeviceMismatch() {

        jdbcTemplate.update(
                """
                INSERT INTO device (id, greenhouse_id, name)
                VALUES (?, ?, ?)
                """,
                "ARD-E2E-002",
                "GH-E2E-001",
                "Second Arduino"
        );

        IngestTelemetryCommand command = new IngestTelemetryCommand(
                "MEAS-E2E-006",
                "MSG-E2E-006",
                "ARD-E2E-002",
                "TEMP-E2E-001",
                "TEMPERATURE",
                25.4,
                "CELSIUS",
                Instant.parse("2026-11-09T12:15:00Z")
        );

        TelemetryValidationException exception = assertThrows(
                TelemetryValidationException.class,
                () -> ingestion.ingest(command)
        );

        assertEquals(
                TelemetryRejectionReason.SENSOR_DEVICE_MISMATCH,
                exception.reason()
        );

        Integer measurementCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM measurement",
                Integer.class
        );

        Integer twinStateCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM twin_state",
                Integer.class
        );

        assertEquals(0, measurementCount);
        assertEquals(0, twinStateCount);
    }

    private IngestTelemetryCommand createValidCommand(){
        return new IngestTelemetryCommand(
                "MEAS-E2E-001",
                "MSG-E2E-001",
                "ARD-E2E-001",
                "TEMP-E2E-001",
                "TEMPERATURE",
                25.4,
                "CELSIUS",
                Instant.parse("2026-11-09T12:00:00Z")
        );
    }
}
