package com.greenhousedigitaltwin.telemetry.infrastructure.persistence;

import com.greenhousedigitaltwin.telemetry.domain.Measurement;

final class MeasurementPersistenceMapper {
    private MeasurementPersistenceMapper() {
    }

    static MeasurementEntity toEntity(Measurement measurement) {
        return new  MeasurementEntity(
                measurement.getMessageId().value(),
                measurement.getSensorId().value(),
                measurement.getType().name(),
                measurement.getValue(),
                measurement.getUnit().name(),
                measurement.getMeasuredAt(),
                measurement.getReceivedAt()
        );
    }
}
