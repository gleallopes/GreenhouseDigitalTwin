package com.greenhousedigitaltwin.telemetry.application;

import com.greenhousedigitaltwin.telemetry.application.port.MeasurementInsertResult;
import com.greenhousedigitaltwin.telemetry.application.port.MeasurementRepository;
import com.greenhousedigitaltwin.telemetry.domain.Measurement;
import com.greenhousedigitaltwin.twin.application.UpdateTwinStateUseCase;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class PersistAcceptedTelemetryService {

    private final MeasurementRepository measurementRepository;
    private final UpdateTwinStateUseCase updateTwinStateUseCase;

    public PersistAcceptedTelemetryService(
            MeasurementRepository measurementRepository,
            UpdateTwinStateUseCase updateTwinStateUseCase
    ) {
        this.measurementRepository = measurementRepository;
        this.updateTwinStateUseCase = updateTwinStateUseCase;
    }

    @Transactional
    public TelemetryPersistenceOutcome persist(Measurement measurement) {
        MeasurementInsertResult result = measurementRepository.insertIfAbsent(measurement);

        if (result == MeasurementInsertResult.ALREADY_EXISTS)
            return TelemetryPersistenceOutcome.ALREADY_PROCESSED;

        updateTwinStateUseCase.updateFrom(measurement);

        return TelemetryPersistenceOutcome.PERSISTED;
    }
}
