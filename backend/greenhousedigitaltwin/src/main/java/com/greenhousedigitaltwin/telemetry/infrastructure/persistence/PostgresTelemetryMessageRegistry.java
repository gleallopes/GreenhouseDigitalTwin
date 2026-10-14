package com.greenhousedigitaltwin.telemetry.infrastructure.persistence;

import com.greenhousedigitaltwin.telemetry.application.port.TelemetryMessageRegistry;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresTelemetryMessageRegistry implements TelemetryMessageRegistry {

    private final SpringDataMeasurementRepository repository;

    PostgresTelemetryMessageRegistry(SpringDataMeasurementRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean exists(String messageId) {
        return repository.existsByMessageId(messageId);
    }
}
