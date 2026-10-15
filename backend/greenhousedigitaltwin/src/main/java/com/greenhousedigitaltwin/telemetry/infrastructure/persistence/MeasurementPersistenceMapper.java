package com.greenhousedigitaltwin.telemetry.infrastructure.persistence;

import com.greenhousedigitaltwin.telemetry.domain.Measurement;

final class MeasurementPersistenceMapper {
    private MeasurementPersistenceMapper() {
    }

    static MeasurementEntity toEntity(Measurement measurement) {
        return new  MeasurementEntity(
                measurement.id().value(),
                measurement.messageId().value(),
                measurement.sensorId().value(),
                measurement.type().name(),
                measurement.value(),
                measurement.unit().name(),
                measurement.measuredAt(),
                measurement.receivedAt()
        );
    }
}
