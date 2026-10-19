package com.greenhousedigitaltwin.twin.infrastructure.persistence;

import com.greenhousedigitaltwin.twin.application.port.TwinStateRepository;
import com.greenhousedigitaltwin.twin.domain.SensorState;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PostgresTwinStateRepository implements TwinStateRepository {

    private final SpringDataTwinStateRepository repository;

    PostgresTwinStateRepository(SpringDataTwinStateRepository repository) {
        this.repository = repository;
    }



    @Override
    @Transactional
    public boolean updateIfNewer(SensorState incoming) {
        int affectedRows = repository.upsertIfNewer(
                incoming.sensorId().value(),
                incoming.measurementType().name(),
                incoming.value(),
                incoming.unit().name(),
                incoming.measuredAt(),
                incoming.receivedAt()
        );

        return affectedRows == 1;
    }
}
