package com.greenhousedigitaltwin.twin.application.port;

import com.greenhousedigitaltwin.twin.domain.SensorState;

public interface TwinStateRepository {
    /**
     * Inserts a new sensor state or updates an existing state
     * only when the incoming observation is newer.
     *
     * This operation must be atomic under concurrent writes.
     *
     * @return true if current state changed, false otherwise.
     */

    boolean updateIfNewer(SensorState incoming);
}
