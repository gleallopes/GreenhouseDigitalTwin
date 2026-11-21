package com.greenhousedigitaltwin.telemetry.infrastructure.mqtt;

import com.greenhousedigitaltwin.telemetry.application.IngestTelemetryCommand;
import com.greenhousedigitaltwin.telemetry.application.TelemetryIngestionOutcome;
import com.greenhousedigitaltwin.telemetry.application.TelemetryRejectionReason;
import com.greenhousedigitaltwin.telemetry.application.TelemetryValidationException;
import com.greenhousedigitaltwin.telemetry.application.port.DeviceGreenhouseRegistry;
import com.greenhousedigitaltwin.telemetry.application.port.IngestTelemetryUseCase;
import com.greenhousedigitaltwin.telemetry.domain.TelemetryIdentityConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Instant;
import java.nio.charset.StandardCharsets;

import static com.greenhousedigitaltwin.telemetry.infrastructure.mqtt.MqttTelemetryMessageHandler.Outcome.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MqttTelemetryMessageHandlerTest {

    static final String TOPIC = "greenhouse/GH-001/telemetry";
    static final String PAYLOAD = """
            {"measurementId":"MEAS-001","messageId":"MSG-001","deviceId":"device","sensorId":"sensor",
             "measurementType":"TEMPERATURE","value":25.4,"unit":"CELSIUS","measuredAt":"2026-09-22T14:32:15Z"}""";

    private final IngestTelemetryUseCase ingestion = mock(IngestTelemetryUseCase.class);
    private final DeviceGreenhouseRegistry registry = mock(DeviceGreenhouseRegistry.class);
    private final MqttTelemetryMessageHandler handler = new MqttTelemetryMessageHandler(ingestion, registry, 4096);

    @Test
    void preservesBothIdentitiesAndMapsEveryField() {
        when(registry.belongsToGreenhouse("device", "GH-001")).thenReturn(true);
        when(ingestion.ingest(any())).thenReturn(TelemetryIngestionOutcome.PERSISTED);
        assertEquals(PERSISTED, handle(PAYLOAD));
        verify(ingestion).ingest(new IngestTelemetryCommand("MEAS-001", "MSG-001", "device", "sensor",
                "TEMPERATURE", 25.4, "CELSIUS", Instant.parse("2026-09-22T14:32:15Z")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"measurementId", "messageId", "deviceId", "sensorId", "measurementType",
            "value", "unit", "measuredAt"})
    void requiresEachContractField(String field) {
        String missing = PAYLOAD.replaceFirst("\"" + field + "\"\\s*:", "\"unexpected\":");
        assertInvalid(missing);
    }

    @Test
    void rejectsInvalidWireRepresentations() {
        for (String invalid : new String[]{"{", "[]", PAYLOAD + "{}",
                PAYLOAD.replace("\"value\":25.4", "\"value\":\"25.4\""),
                PAYLOAD.replace("\"value\":25.4", "\"value\":1e400"),
                PAYLOAD.replace("\"value\":25.4", "\"value\":1,\"value\":2"),
                PAYLOAD.replace("MEAS-001", " "),
                PAYLOAD.replace("MEAS-001", "x".repeat(101)),
                PAYLOAD.replace("\"device\"", "true"),
                PAYLOAD.replace("15Z", "15.1234567Z"),
                PAYLOAD.replace("09-22", "02-30"),
                PAYLOAD.replace("15Z", "15"),
                PAYLOAD.replace("MEAS-001", "\\u0000")}) {
            assertInvalid(invalid);
        }
        assertEquals(INVALID_PAYLOAD, handler.handle(TOPIC, null));
        assertEquals(INVALID_PAYLOAD, handler.handle(TOPIC, new byte[]{(byte) 0xc3, 0x28}));
        verifyNoInteractions(registry, ingestion);
    }

    @Test
    void enforcesByteBoundaryBeforeParsing() {
        when(registry.belongsToGreenhouse("device", "GH-001")).thenReturn(true);
        when(ingestion.ingest(any())).thenReturn(TelemetryIngestionOutcome.PERSISTED);
        String utf8 = PAYLOAD.replace("MEAS-001", "més");
        String exact = utf8 + " ".repeat(4096 - bytes(utf8).length);
        assertEquals(PERSISTED, handle(exact));
        assertEquals(PAYLOAD_TOO_LARGE, handle(exact + " "));
        verify(ingestion, times(1)).ingest(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"greenhouse//telemetry", "greenhouse/+/telemetry", "greenhouse/ /telemetry",
            "greenhouse/GH-001/telemetry/extra", "greenhouse/GH-001/state", "/greenhouse/GH-001/telemetry"})
    void rejectsMalformedTopics(String topic) {
        assertEquals(INVALID_TOPIC, handler.handle(topic, bytes(PAYLOAD)));
        verifyNoInteractions(registry, ingestion);
    }

    @Test
    void rejectsUnregisteredRelationshipAndPropagatesRegistryFailure() {
        assertEquals(SOURCE_MISMATCH, handle(PAYLOAD));
        assertEquals(SOURCE_MISMATCH, handler.handle("greenhouse/unknown/telemetry", bytes(PAYLOAD)));
        when(registry.belongsToGreenhouse("device", "GH-001"))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));
        assertThrows(DataAccessResourceFailureException.class, () -> handle(PAYLOAD));
        verifyNoInteractions(ingestion);
    }

    @Test
    void classifiesApplicationResultsAndPropagatesUnexpectedFailures() {
        when(registry.belongsToGreenhouse("device", "GH-001")).thenReturn(true);
        when(ingestion.ingest(any())).thenReturn(TelemetryIngestionOutcome.ALREADY_PROCESSED)
                .thenThrow(new TelemetryIdentityConflictException("conflict"))
                .thenThrow(new TelemetryValidationException(TelemetryRejectionReason.UNKNOWN_SENSOR))
                .thenThrow(new IllegalStateException("unexpected"));
        assertEquals(ALREADY_PROCESSED, handle(PAYLOAD));
        assertEquals(IDENTITY_CONFLICT, handle(PAYLOAD));
        assertEquals(VALIDATION_REJECTED, handle(PAYLOAD));
        assertThrows(IllegalStateException.class, () -> handle(PAYLOAD));
    }

    private void assertInvalid(String payload) {
        assertEquals(INVALID_PAYLOAD, handle(payload));
        verifyNoInteractions(registry, ingestion);
    }

    private MqttTelemetryMessageHandler.Outcome handle(String payload) {
        return handler.handle(TOPIC, bytes(payload));
    }

    static byte[] bytes(String payload) {
        return payload.getBytes(StandardCharsets.UTF_8);
    }
}
