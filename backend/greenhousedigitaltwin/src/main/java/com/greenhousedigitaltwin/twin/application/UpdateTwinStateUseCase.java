package com.greenhousedigitaltwin.twin.application;

import com.greenhousedigitaltwin.telemetry.domain.Measurement;

public interface UpdateTwinStateUseCase {
    boolean updateFrom(Measurement measurement);
}
