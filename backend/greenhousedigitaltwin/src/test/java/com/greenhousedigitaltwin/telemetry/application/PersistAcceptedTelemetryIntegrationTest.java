package com.greenhousedigitaltwin.telemetry.application;



import com.greenhousedigitaltwin.telemetry.domain.*;
import com.greenhousedigitaltwin.twin.application.UpdateTwinStateUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.greenhousedigitaltwin.telemetry.CreateMeasurementHelper.createMeasurement;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
public class PersistAcceptedTelemetryIntegrationTest {

    @Autowired
    private PersistAcceptedTelemetryService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private UpdateTwinStateUseCase updateTwinStateUseCase;

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
    void shouldRollbackMeasurementWhenTwinUpdateFails() {
        Measurement measurement =
                createMeasurement("MEAS-100", "MSG-100");

        when(updateTwinStateUseCase.updateFrom(any(Measurement.class)))
                .thenThrow(new IllegalStateException(
                        "Twin persistence failed"
                ));

        assertThatThrownBy(() -> service.persist(measurement))
                .isInstanceOf(IllegalStateException.class);

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM measurement
                WHERE message_id = ?
                """,
                Integer.class,
                "MSG-100"
        );

        assertThat(count).isZero();
    }

    @Test
    void shouldCommitMeasurementWhenTwinUpdateReturnsFalse() {
        Measurement measurement =
                createMeasurement("MEAS-101", "MSG-101");

        when(updateTwinStateUseCase.updateFrom(any(Measurement.class)))
                .thenReturn(false);

        service.persist(measurement);

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM measurement
                WHERE message_id = ?
                """,
                Integer.class,
                "MSG-101"
        );

        assertThat(count).isEqualTo(1);
    }

}
