package com.greenhousedigitaltwin.twin.application;

import com.greenhousedigitaltwin.telemetry.domain.Measurement;
import com.greenhousedigitaltwin.twin.application.port.TwinStateRepository;
import com.greenhousedigitaltwin.twin.domain.SensorState;
import org.springframework.stereotype.Service;

@Service
public class UpdateTwinStateService implements UpdateTwinStateUseCase {

    private final TwinStateRepository twinStateRepository;

    public UpdateTwinStateService(TwinStateRepository twinStateRepository) {
        this.twinStateRepository = twinStateRepository;
    }

    @Override
    public boolean updateFrom(Measurement measurement) {
        SensorState state = new SensorState(
                measurement.sensorId(),
                measurement.type(),
                measurement.value(),
                measurement.unit(),
                measurement.measuredAt(),
                measurement.receivedAt()
        );

        return twinStateRepository.updateIfNewer(state);
    }
}
