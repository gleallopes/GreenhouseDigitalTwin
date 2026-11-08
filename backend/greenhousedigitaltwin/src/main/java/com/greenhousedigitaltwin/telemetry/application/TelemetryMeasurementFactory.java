package com.greenhousedigitaltwin.telemetry.application;

import com.greenhousedigitaltwin.greenhouse.domain.SensorId;
import com.greenhousedigitaltwin.telemetry.domain.*;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class TelemetryMeasurementFactory {

    private final Clock clock;

    public TelemetryMeasurementFactory(Clock clock) {
        this.clock = clock;
    }

    public Measurement create(IngestTelemetryCommand command){
        return new Measurement(
                new MeasurementId(command.measurementId()),
                new MessageId(command.messageId()),
                new SensorId(command.sensorId()),
                MeasurementType.valueOf(command.measurementType()),
                command.value(),
                Unit.valueOf(command.unit()),
                command.measuredAt(),
                clock.instant()
        );
    }
}
