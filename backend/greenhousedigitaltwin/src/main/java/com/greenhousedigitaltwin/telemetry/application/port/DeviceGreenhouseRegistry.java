package com.greenhousedigitaltwin.telemetry.application.port;

public interface DeviceGreenhouseRegistry {

    boolean belongsToGreenhouse(String deviceId, String greenhouseId);
}
