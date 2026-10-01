package com.greenhousedigitaltwin.telemetry.application;
import com.greenhousedigitaltwin.greenhouse.domain.SensorId;
import com.greenhousedigitaltwin.telemetry.application.port.MeasurementPlausibilityValidator;
import com.greenhousedigitaltwin.telemetry.application.port.MeasurementSourceRegistry;

import com.greenhousedigitaltwin.telemetry.application.port.MeasurementSourceValidationResult;
import com.greenhousedigitaltwin.telemetry.application.port.TelemetryMessageRegistry;
import com.greenhousedigitaltwin.telemetry.domain.MeasurementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TelemetryValidatorTest {

    @Mock
    private MeasurementSourceRegistry sourceRegistry;

    @Mock
    private TelemetryMessageRegistry messageRegistry;

    @Mock
    private MeasurementPlausibilityValidator plausibilityValidator;

    private TelemetryValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TelemetryValidator(
                sourceRegistry,
                messageRegistry,
                plausibilityValidator
        );
    }

    //      Structure Validation Test

    @Test
    void shouldAcceptStructurallyValidTelemetry() {
        TelemetryMessage message = new TelemetryMessage(
                "MSG-001",
                "ARD-001",
                "TEMP-001",
                "TEMPERATURE",
                25.4,
                "CELSIUS",
                Instant.parse("2026-09-24T18:30:15Z")
        );

        when(sourceRegistry.validateSource(
                "ARD-001",
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE
        )).thenReturn(
                MeasurementSourceValidationResult.VALID
        );

        when(plausibilityValidator.isPlausible(
                MeasurementType.TEMPERATURE,
                25.4
        )).thenReturn(true);

        TelemetryValidationResult result = validator.validate(message);
        assertTrue(result.valid());
        assertNull(result.rejectionReason());
    }

    @Test
    void shouldMissingMessageId(){
        TelemetryMessage message = new TelemetryMessage(
                "",
                "ARD-001",
                "TEMP-001",
                "TEMPERATURE",
                25.4,
                "CELSIUS",
                Instant.parse("2026-09-24T18:30:15Z")
        );

        TelemetryValidationResult result = validator.validate(message);
        assertFalse(result.valid());
        assertEquals(
                TelemetryRejectionReason.STRUCTURAL_ERROR,
                result.rejectionReason()
        );
    }

    @Test
    void shouldMissingSensorId(){
        TelemetryMessage message = new TelemetryMessage(
                "MSG-001",
                "",
                "TEMP-001",
                "TEMPERATURE",
                25.4,
                "CELSIUS",
                Instant.parse("2026-09-24T18:30:15Z")
        );

        TelemetryValidationResult result = validator.validate(message);
        assertFalse(result.valid());
        assertEquals(
                TelemetryRejectionReason.STRUCTURAL_ERROR,
                result.rejectionReason()
        );
    }

    @Test
    void shouldMissingTimestamp(){
        TelemetryMessage message = new TelemetryMessage(
                "MSG-001",
                "ARD-001",
                "TEMP-001",
                "TEMPERATURE",
                25.4,
                "CELSIUS",
                null
        );

        TelemetryValidationResult result = validator.validate(message);
        assertFalse(result.valid());
        assertEquals(
                TelemetryRejectionReason.INVALID_TIMESTAMP,
                result.rejectionReason()
        );
    }

    @Test
    void shouldRejectNonFiniteValue(){
        TelemetryMessage message = new TelemetryMessage(
                "MSG-001",
                "ARD-001",
                "TEMP-001",
                "TEMPERATURE",
                Double.NaN,
                "CELSIUS",
                Instant.parse("2026-09-24T18:30:15Z")
        );
        TelemetryValidationResult result = validator.validate(message);
        assertFalse(result.valid());
        assertEquals(
                TelemetryRejectionReason.STRUCTURAL_ERROR,
                result.rejectionReason()
        );
    }

    @Test
    void shouldRejectUnknownMeasurementType(){
        TelemetryMessage message = new TelemetryMessage(
                "MSG-001",
                "ARD-001",
                "TEMP-001",
                "PRESSURE",
                25.4,
                "CELSIUS",
                Instant.parse("2026-09-24T18:30:15Z")
        );

        TelemetryValidationResult result = validator.validate(message);
        assertFalse(result.valid());
        assertEquals(
                TelemetryRejectionReason.SEMANTIC_ERROR,
                result.rejectionReason()
        );
    }

    @Test
    void shouldRejectIncompatibleMeasurementTypeAndUnit(){
        TelemetryMessage message = new TelemetryMessage(
                "MSG-001",
                "ARD-001",
                "TEMP-001",
                "TEMPERATURE",
                25.4,
                "LUX",
                Instant.parse("2026-09-24T18:30:15Z")
        );
        TelemetryValidationResult result = validator.validate(message);
        assertFalse(result.valid());
        assertEquals(
                TelemetryRejectionReason.INCOMPATIBLE_MEASUREMENT_TYPE,
                result.rejectionReason()
        );
    }

    //        Semantic validation Test
    @Test
    void shouldAcceptHumidityMeasurementType(){
        TelemetryMessage message = new TelemetryMessage(
                "MSG-001",
                "ARD-001",
                "HUM-001",
                "HUMIDITY",
                25.4,
                "PERCENT",
                Instant.parse("2026-09-24T18:30:15Z")
        );

        when(sourceRegistry.validateSource(
                "ARD-001",
                new SensorId("HUM-001"),
                MeasurementType.HUMIDITY
        )).thenReturn(
                MeasurementSourceValidationResult.VALID
        );

        when(plausibilityValidator.isPlausible(
                MeasurementType.HUMIDITY,
                25.4
        )).thenReturn(true);

        TelemetryValidationResult result = validator.validate(message);
        assertTrue(result.valid());
        assertNull(result.rejectionReason());
    }

    //        Source Validation Test
    @Test
    void shouldAcceptTelemetryFromValidSource() {

        when(sourceRegistry.validateSource(
                "ARD-001",
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE
        )).thenReturn(
                MeasurementSourceValidationResult.VALID
        );

        when(plausibilityValidator.isPlausible(
                MeasurementType.TEMPERATURE,
                25.4
        )).thenReturn(true);

        var result = validator.validate(validMessage());

        assertTrue(result.valid());
    }

    @Test
    void shouldRejectUnknownDevice() {

        when(sourceRegistry.validateSource(
                "ARD-001",
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE
        )).thenReturn(
                MeasurementSourceValidationResult.UNKNOWN_DEVICE
        );

        var result = validator.validate(validMessage());

        assertFalse(result.valid());

        assertEquals(
                TelemetryRejectionReason.UNKNOWN_DEVICE,
                result.rejectionReason()
        );
    }

    @Test
    void shouldRejectUnknownSensor() {
        when(sourceRegistry.validateSource(
                "ARD-001",
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE
        )).thenReturn(
                MeasurementSourceValidationResult.UNKNOWN_SENSOR
        );
        var result = validator.validate(validMessage());
        assertFalse(result.valid());
    }

    @Test
    void shouldRejectSensorAndDeviceMismatch() {
        when(sourceRegistry.validateSource(
                "ARD-001",
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE
        )).thenReturn(
                MeasurementSourceValidationResult.SENSOR_DEVICE_MISMATCH
        );
        var result = validator.validate(validMessage());
        assertFalse(result.valid());
        assertEquals(
                TelemetryRejectionReason.SENSOR_DEVICE_MISMATCH,
                result.rejectionReason()
        );
    }

    //        Duplicates validation Test
    @Test
    void shouldAcceptNewTelemetryMessage(){
//      source from MOCK
        when(sourceRegistry.validateSource(
                "ARD-001",
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE
        )).thenReturn(MeasurementSourceValidationResult.VALID);

        when(messageRegistry.exists("MSG-001"))
                .thenReturn(false);

        when(plausibilityValidator.isPlausible(
                MeasurementType.TEMPERATURE,
                25.4
        )).thenReturn(true);

        var result = validator.validate(validMessage());
        assertTrue(result.valid());
    }

    @Test
    void shouldRejectDuplicateTelemetryMessage(){
//        source from MOCK
        when(sourceRegistry.validateSource(
                "ARD-001",
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE
        )).thenReturn(MeasurementSourceValidationResult.VALID);

        when(messageRegistry.exists("MSG-001"))
                .thenReturn(true);

        var result = validator.validate(validMessage());
        assertFalse(result.valid());
        assertEquals(
                TelemetryRejectionReason.DUPLICATE_MESSAGE,
                result.rejectionReason()
        );
    }

    //        Plausability verification Test
    @Test
    void shouldAcceptPlausibleSensorData(){
        when(sourceRegistry.validateSource(
                "ARD-001",
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE
        )).thenReturn(MeasurementSourceValidationResult.VALID);

        when(plausibilityValidator.isPlausible(
                MeasurementType.TEMPERATURE,
                25.4
        )).thenReturn(true);

        var result = validator.validate(validMessage());
        assertTrue(result.valid());
    }

    @Test
    void shouldRejectPlausibleSensorDataOutOfRange(){

        var message = new TelemetryMessage(
                "MSG-001",
                "ARD-001",
                "TEMP-001",
                "TEMPERATURE",
                -999.0,
                "CELSIUS",
                Instant.parse("2026-09-30T15:00:00Z")
        );

        when(sourceRegistry.validateSource(
                "ARD-001",
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE
        )).thenReturn(MeasurementSourceValidationResult.VALID);

        when(messageRegistry.exists("MSG-001"))
                .thenReturn(false);

        when(plausibilityValidator.isPlausible(
                MeasurementType.TEMPERATURE,
                -999.0
        )).thenReturn(false);

        var result = validator.validate(message);
        assertFalse(result.valid());
        assertEquals(
                TelemetryRejectionReason.PHYSICAL_VALUE_OUT_OF_RANGE,
                result.rejectionReason());
    }

    @Test
    void shouldRejectDuplicateMessageBeforeReachPlausibility(){
        var message = validMessage();

        when(sourceRegistry.validateSource(
                "ARD-001",
                new SensorId("TEMP-001"),
                MeasurementType.TEMPERATURE
        )).thenReturn(MeasurementSourceValidationResult.VALID);

        when(messageRegistry.exists("MSG-001"))
                .thenReturn(true);

        var result = validator.validate(message);

        assertFalse(result.valid());

        assertEquals(
                TelemetryRejectionReason.DUPLICATE_MESSAGE,
                result.rejectionReason()
        );

        verifyNoInteractions(plausibilityValidator);
    }

    private TelemetryMessage validMessage() {
        return new TelemetryMessage(
                "MSG-001",
                "ARD-001",
                "TEMP-001",
                "TEMPERATURE",
                25.4,
                "CELSIUS",
                Instant.parse("2026-09-30T15:00:00Z")
        );
    }
}
