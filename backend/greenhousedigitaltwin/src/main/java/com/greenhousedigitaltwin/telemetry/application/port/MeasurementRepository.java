package com.greenhousedigitaltwin.telemetry.application.port;

import com.greenhousedigitaltwin.telemetry.domain.Measurement;

public interface MeasurementRepository {
    void save(Measurement measurement);
}
