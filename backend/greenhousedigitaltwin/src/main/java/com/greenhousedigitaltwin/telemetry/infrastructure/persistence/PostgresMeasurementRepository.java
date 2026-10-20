package com.greenhousedigitaltwin.telemetry.infrastructure.persistence;

import com.greenhousedigitaltwin.telemetry.application.port.MeasurementInsertResult;
import com.greenhousedigitaltwin.telemetry.application.port.MeasurementRepository;
import com.greenhousedigitaltwin.telemetry.domain.Measurement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public class PostgresMeasurementRepository implements MeasurementRepository {

    private final SpringDataMeasurementRepository repository;

    private final JdbcTemplate jdbcTemplate;

    PostgresMeasurementRepository(
            SpringDataMeasurementRepository repository,
            JdbcTemplate jdbcTemplate
    ) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(Measurement measurement) {
        MeasurementEntity entity = MeasurementPersistenceMapper
                .toEntity(measurement);

        repository.save(entity);
    }

    @Override
    public MeasurementInsertResult insertIfAbsent(Measurement measurement) {
        int affectedRows = jdbcTemplate.update(
                """
                        INSERT INTO measurement (
                            measurement_id,
                            message_id,
                            sensor_id,
                            measurement_type,
                            value,
                            unit,
                            measured_at,
                            received_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (message_id) DO NOTHING
                        """,
                measurement.id().value(),
                measurement.messageId().value(),
                measurement.sensorId().value(),
                measurement.type().name(),
                measurement.value(),
                measurement.unit().name(),
                Timestamp.from(measurement.measuredAt()),
                Timestamp.from(measurement.receivedAt())
        );

        return affectedRows == 1
                ? MeasurementInsertResult.INSERTED
                : MeasurementInsertResult.ALREADY_EXISTS;
    }
}
