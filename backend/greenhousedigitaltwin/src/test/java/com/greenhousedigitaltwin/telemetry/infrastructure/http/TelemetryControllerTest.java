package com.greenhousedigitaltwin.telemetry.infrastructure.http;

import com.greenhousedigitaltwin.telemetry.application.IngestTelemetryCommand;
import com.greenhousedigitaltwin.telemetry.application.TelemetryIngestionOutcome;
import com.greenhousedigitaltwin.telemetry.application.TelemetryRejectionReason;
import com.greenhousedigitaltwin.telemetry.application.TelemetryValidationException;
import com.greenhousedigitaltwin.telemetry.application.port.IngestTelemetryUseCase;
import com.greenhousedigitaltwin.telemetry.domain.TelemetryIdentityConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TelemetryControllerTest {

    private static final String REQUEST = """
            {
              "measurementId": "measurement-1",
              "messageId": "message-1",
              "deviceId": "device-1",
              "sensorId": "sensor-1",
              "measurementType": "TEMPERATURE",
              "value": 0,
              "unit": "CELSIUS",
              "measuredAt": "2026-11-19T12:00:00.123456Z"
            }
            """;

    private final IngestTelemetryUseCase ingestion = mock(IngestTelemetryUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TelemetryController(ingestion))
                .setControllerAdvice(new TelemetryExceptionHandler(), new TelemetryRequestBodyAdvice())
                .build();
    }

    @ParameterizedTest
    @CsvSource({"PERSISTED, 201", "ALREADY_PROCESSED, 200"})
    void mapsOutcomeAndDelegatesOnce(TelemetryIngestionOutcome outcome, int httpStatus) throws Exception {
        when(ingestion.ingest(any())).thenReturn(outcome);

        submit(REQUEST).andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.outcome").value(outcome.name()));

        verify(ingestion).ingest(new IngestTelemetryCommand(
                "measurement-1", "message-1", "device-1", "sensor-1",
                "TEMPERATURE", 0.0, "CELSIUS", Instant.parse("2026-11-19T12:00:00.123456Z")));
        verifyNoMoreInteractions(ingestion);
    }

    @Test
    void rejectsInvalidRequestsBeforeDelegating() throws Exception {
        for (String body : new String[] {
                "{",
                REQUEST.replace("\"sensorId\": \"sensor-1\",", ""),
                REQUEST.replace("\"value\": 0,", ""),
                REQUEST.replace("\"value\": 0", "\"value\": null"),
                REQUEST.replace("message-1", "m".repeat(101)),
                REQUEST.replace("2026-11-19", "2026-02-30"),
                REQUEST.replace(".123456Z", ".1234567Z"),
                REQUEST.replace("\"2026-11-19T12:00:00.123456Z\"", "0")
        }) {
            submit(body).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
        verifyNoInteractions(ingestion);
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void rejectsOversizedBodyBeforeDelegating(boolean declareLength) throws Exception {
        String body = REQUEST + " ".repeat(TelemetryRequestBodyAdvice.MAX_BODY_BYTES + 1 - REQUEST.length());
        var request = post("/api/v1/telemetry").contentType(MediaType.APPLICATION_JSON).content(body);
        if (declareLength) {
            request.header("Content-Length", body.length());
        }

        mvc.perform(request)
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE"));
        verifyNoInteractions(ingestion);
    }

    @Test
    void acceptsBodyAtLimit() throws Exception {
        when(ingestion.ingest(any())).thenReturn(TelemetryIngestionOutcome.PERSISTED);
        submit(REQUEST + " ".repeat(TelemetryRequestBodyAdvice.MAX_BODY_BYTES - REQUEST.length()))
                .andExpect(status().isCreated());
        verify(ingestion).ingest(any());
    }

    @Test
    void mapsValidationRejection() throws Exception {
        when(ingestion.ingest(any())).thenThrow(
                new TelemetryValidationException(TelemetryRejectionReason.PHYSICAL_VALUE_OUT_OF_RANGE));
        submit(REQUEST).andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("PHYSICAL_VALUE_OUT_OF_RANGE"));
    }

    @Test
    void mapsIdentityConflict() throws Exception {
        when(ingestion.ingest(any())).thenThrow(new TelemetryIdentityConflictException("private diagnostic"));
        submit(REQUEST).andExpect(status().isConflict())
                .andExpect(content().json("""
                        {"code":"IDENTITY_CONFLICT","message":"Telemetry identity conflicts with an existing event."}
                        """, org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test
    void sanitizesUnexpectedFailure() throws Exception {
        when(ingestion.ingest(any())).thenThrow(new IllegalStateException("private diagnostic"));
        submit(REQUEST).andExpect(status().isInternalServerError())
                .andExpect(content().json("""
                        {"code":"INTERNAL_ERROR","message":"Telemetry ingestion failed."}
                        """, org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test
    void rejectsUnsupportedContentTypeBeforeDelegating() throws Exception {
        mvc.perform(post("/api/v1/telemetry").contentType(MediaType.TEXT_PLAIN).content(REQUEST))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(ingestion);
    }

    @Test
    void rejectsUnsupportedAcceptBeforeDelegating() throws Exception {
        mvc.perform(post("/api/v1/telemetry").contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_PLAIN).content(REQUEST))
                .andExpect(status().isNotAcceptable());
        verifyNoInteractions(ingestion);
    }

    private ResultActions submit(String body) throws Exception {
        return mvc.perform(post("/api/v1/telemetry").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
