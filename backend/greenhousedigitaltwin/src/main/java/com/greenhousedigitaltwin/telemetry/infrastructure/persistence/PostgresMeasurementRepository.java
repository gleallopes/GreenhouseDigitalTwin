package com.greenhousedigitaltwin.telemetry.infrastructure.persistence;

import com.greenhousedigitaltwin.telemetry.application.port.MeasurementRepository;
import com.greenhousedigitaltwin.telemetry.domain.Measurement;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresMeasurementRepository implements MeasurementRepository {

    private final SpringDataMeasurementRepository repository;

    PostgresMeasurementRepository(SpringDataMeasurementRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Measurement measurement) {
        MeasurementEntity entity = MeasurementPersistenceMapper
                .toEntity(measurement);

        repository.save(entity);
    }
}
