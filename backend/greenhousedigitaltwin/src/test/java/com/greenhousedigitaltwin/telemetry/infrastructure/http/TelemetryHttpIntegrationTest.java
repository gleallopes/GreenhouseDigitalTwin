package com.greenhousedigitaltwin.telemetry.infrastructure.http;

import com.greenhousedigitaltwin.twin.application.UpdateTwinStateUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TelemetryHttpIntegrationTest {

    private static final String CURRENT = "2026-11-19T12:00:00.123456Z";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoSpyBean
    UpdateTwinStateUseCase twinUpdate;

    @BeforeEach
    void seedSource() {
        jdbc.update("DELETE FROM twin_state");
        jdbc.update("DELETE FROM measurement");
        jdbc.update("INSERT INTO greenhouse (id, name) VALUES ('gh', 'Test greenhouse') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO device (id, greenhouse_id, name) VALUES ('device', 'gh', 'Test device') ON CONFLICT DO NOTHING");
        jdbc.update("""
                INSERT INTO sensor (id, device_id, measurement_type, unit)
                VALUES ('sensor', 'device', 'TEMPERATURE', 'CELSIUS') ON CONFLICT DO NOTHING
                """);
    }

    @Test
    void preservesHistoryAndCurrentStateAcrossRetriesConflictsAndOrdering() throws Exception {
        String request = telemetry("1", 25, CURRENT);
        assertResponse(submit(request), 201, "PERSISTED");
        Map<String, Object> originalState = jdbc.queryForMap("SELECT * FROM twin_state");
        assertThat(originalState).containsEntry("value", 25.0);

        assertResponse(submit(request), 200, "ALREADY_PROCESSED");
        assertResponse(submit(telemetry("1", 25, "2026-11-19T09:00:00.123456-03:00")),
                200, "ALREADY_PROCESSED");
        assertResponse(submit(telemetry("1", 26, CURRENT)), 409, "IDENTITY_CONFLICT");
        assertThat(historySize()).isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT * FROM twin_state")).isEqualTo(originalState);

        assertResponse(submit(telemetry("2", 22, "2026-11-19T12:00:00.123455Z")), 201, "PERSISTED");
        assertResponse(submit(telemetry("3", 23, CURRENT)), 201, "PERSISTED");
        assertThat(historySize()).isEqualTo(3);
        assertThat(jdbc.queryForMap("SELECT * FROM twin_state")).isEqualTo(originalState);
    }

    @Test
    void enforcesBodyLimitInBytesBeforeWritingTelemetry() throws Exception {
        byte[] atLimit = paddedTelemetry("é1", 25, CURRENT, 4096);
        assertResponse(submit(HttpRequest.BodyPublishers.ofByteArray(atLimit)), 201, "PERSISTED");
        assertThat(historySize()).isEqualTo(1);
        Map<String, Object> originalState = jdbc.queryForMap("SELECT * FROM twin_state");
        assertThat(originalState).containsEntry("value", 25.0);

        byte[] overLimit = paddedTelemetry("é2", 26, "2026-11-19T12:00:01Z", 4097);
        HttpRequest.BodyPublisher stream =
                HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(overLimit));
        assertThat(stream.contentLength()).isEqualTo(-1);
        assertResponse(submit(stream), 413, "PAYLOAD_TOO_LARGE");

        assertThat(historySize()).isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT * FROM twin_state")).isEqualTo(originalState);
    }

    @Test
    void rejectsInvalidSourceUnitAndPhysicalValueWithoutWriting() throws Exception {
        String valid = telemetry("1", 25, CURRENT);
        assertResponse(submit(valid.replace("\"sensor\"", "\"unknown\"")), 422, "UNKNOWN_SENSOR");
        assertResponse(submit(valid.replace("celsius", "percent")), 422, "INCOMPATIBLE_MEASUREMENT_TYPE");
        assertResponse(submit(telemetry("1", 500, CURRENT)), 422, "PHYSICAL_VALUE_OUT_OF_RANGE");
        assertThat(historySize()).isZero();
        assertThat(jdbc.queryForList("SELECT * FROM twin_state")).isEmpty();
    }

    @Test
    void rollsBackNewMeasurementWhenTwinUpdateFailsAndAcceptsRetry() throws Exception {
        doThrow(new IllegalStateException("private diagnostic"))
                .doCallRealMethod().when(twinUpdate).updateFrom(any());
        String request = telemetry("1", 25, CURRENT);

        HttpResponse<String> failure = submit(request);
        assertResponse(failure, 500, "INTERNAL_ERROR");
        assertThat(failure.body()).doesNotContain("private diagnostic");
        assertThat(historySize()).isZero();
        assertThat(jdbc.queryForList("SELECT * FROM twin_state")).isEmpty();

        assertResponse(submit(request), 201, "PERSISTED");
        assertThat(historySize()).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT * FROM twin_state")).hasSize(1);
    }

    private long historySize() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM measurement", Long.class);
    }

    private String telemetry(String id, int value, String measuredAt) {
        return """
                {"measurementId":"measurement-%s","messageId":"message-%s","deviceId":"device",
                 "sensorId":"sensor","measurementType":" temperature ","value":%d,
                 "unit":" celsius ","measuredAt":"%s"}
                """.formatted(id, id, value, measuredAt);
    }

    private byte[] paddedTelemetry(String id, int value, String measuredAt, int bytes) {
        String json = telemetry(id, value, measuredAt);
        return (json + " ".repeat(bytes - json.getBytes(UTF_8).length)).getBytes(UTF_8);
    }

    private HttpResponse<String> submit(String json) throws Exception {
        return submit(HttpRequest.BodyPublishers.ofString(json));
    }

    private HttpResponse<String> submit(HttpRequest.BodyPublisher body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/telemetry"))
                .header("Content-Type", "application/json")
                .POST(body)
                .build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private void assertResponse(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).withFailMessage("Unexpected response %s", response.body()).isEqualTo(status);
        assertThat(response.body()).contains("\"" + code + "\"");
    }
}
