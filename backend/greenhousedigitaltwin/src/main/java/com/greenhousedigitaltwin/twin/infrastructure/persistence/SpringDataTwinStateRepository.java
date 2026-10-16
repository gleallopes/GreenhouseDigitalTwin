package com.greenhousedigitaltwin.twin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

interface SpringDataTwinStateRepository
        extends JpaRepository<TwinStateEntity, String> {

    @Modifying
    @Query(value = """
        INSERT INTO twin_state (
            sensor_id,
            measurement_type,
            value,
            unit,
            measured_at,
            received_at
        )
        VALUES (
            :sensorId,
            :measurementType,
            :value,
            :unit,
            :measuredAt,
            :receivedAt
        )
        ON CONFLICT (sensor_id)
        DO UPDATE SET
            measurement_type = EXCLUDED.measurement_type,
            value = EXCLUDED.value,
            unit = EXCLUDED.unit,
            measured_at = EXCLUDED.measured_at,
            received_at = EXCLUDED.received_at
        WHERE twin_state.measured_at < EXCLUDED.measured_at
        """, nativeQuery = true)
    int upsertIfNewer(
            @Param("sensorId") String sensorId,
            @Param("measurementType") String measurementType,
            @Param("value") double value,
            @Param("unit") String unit,
            @Param("measuredAt") Instant measuredAt,
            @Param("receivedAt") Instant receivedAt
    );
}