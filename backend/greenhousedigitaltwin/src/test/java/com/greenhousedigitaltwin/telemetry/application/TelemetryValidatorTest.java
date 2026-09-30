package com.greenhousedigitaltwin.telemetry.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class TelemetryValidatorTest {
    private final TelemetryValidator validator = new TelemetryValidator();

    @Test
    void shouldAcceptStructurallyValidTelemetry() {
        TelemetryMessage message = new TelemetryMessage(
                "MSG-001",
                "ARD-001",
                "TEMP-001",
                "TEMPERATURE",
                25.4,
                "CELCIUS",
                Instant.parse("2026-09-24T18:30:15Z")
        );

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
                "CELCIUS",
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
                "CELCIUS",
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
                "CELCIUS",
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
                "CELCIUS",
                Instant.parse("2026-09-24T18:30:15Z")
        );
        TelemetryValidationResult result = validator.validate(message);
        assertFalse(result.valid());
        assertEquals(
                TelemetryRejectionReason.STRUCTURAL_ERROR,
                result.rejectionReason()
        );
    }
}
