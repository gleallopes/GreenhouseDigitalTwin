package com.greenhousedigitaltwin.telemetry.infrastructure.persistence;


import com.greenhousedigitaltwin.greenhouse.domain.SensorId;
import com.greenhousedigitaltwin.telemetry.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
class PostgresMeasurementRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private SpringDataMeasurementRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PostgresMeasurementRepository measurementRepository;

    @Autowired
    private PostgresTelemetryMessageRegistry messageRegistry;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        jdbcTemplate.update("DELETE FROM sensor");
        jdbcTemplate.update("DELETE FROM device");
        jdbcTemplate.update("DELETE FROM greenhouse");

        jdbcTemplate.update(
                """
                INSERT INTO greenhouse (id, name)
                VALUES (?, ?)
                """, "GH-001", "Apartment Mint Greenhouse"
        );

        jdbcTemplate.update("""
        INSERT INTO device (id, greenhouse_id, name)
        VALUES (?, ?, ?)
        """, "ARD-001", "GH-001","Arduino Controller"
        );

        jdbcTemplate.update("""
        INSERT INTO sensor (id, device_id, measurement_type, unit)
        VALUES (?, ?, ?, ?)
        """, "TEMP-001", "ARD-001", "TEMPERATURE", "CELSIUS"
        );
    }

    @Test
    void shouldStartPostgresSQLAndApplyFlywayMigration(){
        assertThat(repository.count()).isZero();
    }

    @Test
    void shouldPersistAcceptedMeasurement(){
//        Arrange
        Measurement measurement = createMeasurement("MEAS-001","MSG-001");

//        Act
        measurementRepository.save(measurement);

        MeasurementEntity persisted = repository
                .findByMessageId("MSG-001")
                .orElseThrow();

        assertThat(persisted.getMeasurementId()).isEqualTo("MEAS-001");
        assertThat(persisted.getMessageId()).isEqualTo("MSG-001");
        assertThat(persisted.getSensorId()).isEqualTo("TEMP-001");
        assertThat(persisted.getMeasurementType()).isEqualTo("TEMPERATURE");
        assertThat(persisted.getUnit()).isEqualTo("CELSIUS");
        assertThat(persisted.getValue()).isEqualTo(25.4);
        assertThat(persisted.getUnit()).isEqualTo("CELSIUS");
    }

    @Test
    void shouldDetectPreviouslyPersistedMeasurement(){
        Measurement measurement = createMeasurement("MEAS-002","MSG-002");

        measurementRepository.save(measurement);

        boolean exists = messageRegistry.exists("MSG-002");
        assertThat(exists).isTrue();
        assertThat(messageRegistry.exists("UNKNOWN-MSG")).isFalse();

    }

    @Test
    void shouldRejectDuplicateMeasurementId() {
        Measurement first =
                createMeasurement("MEAS-010", "MSG-010");

        Measurement second =
                createMeasurement("MEAS-010", "MSG-011");

        measurementRepository.save(first);

        assertThatThrownBy(() -> {
            measurementRepository.save(second);
            repository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectDuplicateMessageId(){
        Measurement firstMeasurement = createMeasurement("MEAS-003","MSG-003");
        Measurement duplicateMeasurement = createMeasurement("MEAS-004","MSG-003");

        measurementRepository.save(firstMeasurement);

        assertThatThrownBy(() -> {
            measurementRepository.save(duplicateMeasurement);
            repository.flush();

        }).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(repository.count()).isEqualTo(1);

    }

    private Measurement createMeasurement(String measurementId, String messageId) {

        return new Measurement(
                new MeasurementId(measurementId),
                new MessageId(messageId),
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE,
                25.4,
                Unit.CELSIUS,
                Instant.parse("2026-09-24T18:30:15Z"),
                Instant.parse("2026-09-24T18:30:16Z")
        );
    }


}
