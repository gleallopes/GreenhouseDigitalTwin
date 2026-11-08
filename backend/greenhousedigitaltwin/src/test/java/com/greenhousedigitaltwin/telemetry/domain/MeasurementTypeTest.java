package com.greenhousedigitaltwin.telemetry.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MeasurementTypeTest {

    @Test
    void shouldAcceptCompatibleUnits() {
        assertTrue(MeasurementType.TEMPERATURE.supports(Unit.CELSIUS));
        assertTrue(MeasurementType.HUMIDITY.supports(Unit.PERCENT));
        assertTrue(MeasurementType.LUMINOSITY.supports(Unit.LUX));
    }

    @Test
    void shouldRejectIncompatibleUnits() {
        assertFalse(MeasurementType.TEMPERATURE.supports(Unit.PERCENT));
        assertFalse(MeasurementType.HUMIDITY.supports(Unit.CELSIUS));
        assertFalse(MeasurementType.LUMINOSITY.supports(Unit.CELSIUS));
    }
}