package com.greenhousedigitaltwin.telemetry.domain;

public enum MeasurementType {
    TEMPERATURE (Unit.CELSIUS),
    HUMIDITY (Unit.PERCENT),
    LUMINOSITY (Unit.LUX);

    private final Unit unit;
    MeasurementType(Unit unit) {
        this.unit = unit;
    }

    public boolean supports(Unit unit) {
        return this.unit == unit;
    }
}
