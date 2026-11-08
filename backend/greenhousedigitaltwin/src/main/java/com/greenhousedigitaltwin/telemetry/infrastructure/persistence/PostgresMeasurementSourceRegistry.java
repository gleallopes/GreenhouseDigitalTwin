
package com.greenhousedigitaltwin.telemetry.infrastructure.persistence;

import com.greenhousedigitaltwin.greenhouse.domain.SensorId;
import com.greenhousedigitaltwin.telemetry.application.port.MeasurementSourceRegistry;
import com.greenhousedigitaltwin.telemetry.application.port.MeasurementSourceValidationResult;
import com.greenhousedigitaltwin.telemetry.domain.MeasurementType;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class PostgresMeasurementSourceRegistry
        implements MeasurementSourceRegistry {

    private final JdbcTemplate jdbcTemplate;

    public PostgresMeasurementSourceRegistry(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MeasurementSourceValidationResult validateSource(
            String deviceId,
            SensorId sensorId,
            MeasurementType measurementType
    ) {
        Objects.requireNonNull(deviceId, "deviceId");
        Objects.requireNonNull(sensorId, "sensorId");
        Objects.requireNonNull(measurementType, "measurementType");

        // 1. Verify the device exists.
        Integer deviceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM device WHERE id = ?",
                Integer.class,
                deviceId
        );

        if (deviceCount == null || deviceCount == 0) {
            return MeasurementSourceValidationResult.UNKNOWN_DEVICE;
        }

        // 2. Find the registered sensor.
        List<SensorRecord> sensors = jdbcTemplate.query(
                """
                SELECT device_id, measurement_type
                FROM sensor
                WHERE id = ?
                """,
                (rs, rowNum) -> new SensorRecord(
                        rs.getString("device_id"),
                        rs.getString("measurement_type")
                ),
                sensorId.value()
        );

        if (sensors.isEmpty()) {
            return MeasurementSourceValidationResult.UNKNOWN_SENSOR;
        }

        SensorRecord sensor = sensors.get(0);

        // 3. Verify sensor ownership.
        if (!sensor.deviceId().equals(deviceId)) {
            return MeasurementSourceValidationResult.SENSOR_DEVICE_MISMATCH;
        }

        // 4. Verify the registered measurement type.
        if (!sensor.measurementType().equals(measurementType.name())) {
            return MeasurementSourceValidationResult.INCOMPATIBLE_MEASUREMENT_TYPE;
        }

        return MeasurementSourceValidationResult.VALID;
    }

    private record SensorRecord(
            String deviceId,
            String measurementType
    ) {}
}
