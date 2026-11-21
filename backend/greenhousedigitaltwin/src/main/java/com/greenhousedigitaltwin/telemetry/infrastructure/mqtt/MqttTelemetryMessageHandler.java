package com.greenhousedigitaltwin.telemetry.infrastructure.mqtt;

import com.greenhousedigitaltwin.telemetry.application.IngestTelemetryCommand;
import com.greenhousedigitaltwin.telemetry.application.TelemetryValidationException;
import com.greenhousedigitaltwin.telemetry.application.port.DeviceGreenhouseRegistry;
import com.greenhousedigitaltwin.telemetry.application.port.IngestTelemetryUseCase;
import com.greenhousedigitaltwin.telemetry.domain.TelemetryIdentityConflictException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

final class MqttTelemetryMessageHandler {

    private static final Pattern TOPIC = Pattern.compile("greenhouse/([^/+#\\p{Cntrl}]+)/telemetry");
    private static final Pattern TIMESTAMP = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,6})?(?:Z|[+-]\\d{2}:\\d{2})");
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private final IngestTelemetryUseCase ingestion;
    private final DeviceGreenhouseRegistry registry;
    private final int maxPayloadBytes;

    MqttTelemetryMessageHandler(IngestTelemetryUseCase ingestion, DeviceGreenhouseRegistry registry,
                               int maxPayloadBytes) {
        this.ingestion = ingestion;
        this.registry = registry;
        this.maxPayloadBytes = maxPayloadBytes;
    }

    Outcome handle(String topic, byte[] payload) {
        String greenhouse = greenhouseId(topic);
        if (greenhouse == null) {
            return Outcome.INVALID_TOPIC;
        }
        if (payload == null || payload.length == 0) {
            return Outcome.INVALID_PAYLOAD;
        }
        if (payload.length > maxPayloadBytes) {
            return Outcome.PAYLOAD_TOO_LARGE;
        }

        IngestTelemetryCommand command;
        try {
            command = parse(payload);
        } catch (CharacterCodingException | tools.jackson.core.JacksonException |
                 DateTimeParseException | InvalidPayload exception) {
            return Outcome.INVALID_PAYLOAD;
        }

        if (!registry.belongsToGreenhouse(command.deviceId(), greenhouse)) {
            return Outcome.SOURCE_MISMATCH;
        }
        try {
            return switch (ingestion.ingest(command)) {
                case PERSISTED -> Outcome.PERSISTED;
                case ALREADY_PROCESSED -> Outcome.ALREADY_PROCESSED;
            };
        } catch (TelemetryValidationException exception) {
            return Outcome.VALIDATION_REJECTED;
        } catch (TelemetryIdentityConflictException exception) {
            return Outcome.IDENTITY_CONFLICT;
        }
    }

    static String greenhouseId(String topic) {
        if (topic == null) {
            return null;
        }
        var matcher = TOPIC.matcher(topic);
        if (matcher.matches() && validText(matcher.group(1), 50)) {
            return matcher.group(1);
        }
        return null;
    }

    private static IngestTelemetryCommand parse(byte[] payload) throws CharacterCodingException {
        String json = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(payload)).toString();
        JsonNode root = JSON.readTree(json);
        if (root == null || !root.isObject() || root.size() != 8) {
            throw new InvalidPayload();
        }
        var value = root.path("value");
        String timestamp = text(root, "measuredAt", 32);
        if (!value.isNumber() || !Double.isFinite(value.doubleValue()) || !TIMESTAMP.matcher(timestamp).matches()) {
            throw new InvalidPayload();
        }
        return new IngestTelemetryCommand(
                text(root, "measurementId", 100), text(root, "messageId", 100),
                text(root, "deviceId", 50), text(root, "sensorId", 50),
                text(root, "measurementType", Integer.MAX_VALUE), value.doubleValue(),
                text(root, "unit", Integer.MAX_VALUE), OffsetDateTime.parse(timestamp).toInstant());
    }

    private static String text(JsonNode root, String field, int maxLength) {
        JsonNode node = root.path(field);
        if (!node.isString() || !validText(node.asString(), maxLength)) {
            throw new InvalidPayload();
        }
        return node.asString();
    }

    private static boolean validText(String value, int maxLength) {
        return !value.isBlank() && value.codePointCount(0, value.length()) <= maxLength
                && value.indexOf(0) < 0 && StandardCharsets.UTF_8.newEncoder().canEncode(value);
    }

    enum Outcome {
        PERSISTED, ALREADY_PROCESSED, INVALID_TOPIC, INVALID_PAYLOAD, PAYLOAD_TOO_LARGE,
        SOURCE_MISMATCH, VALIDATION_REJECTED, IDENTITY_CONFLICT
    }

    private static final class InvalidPayload extends RuntimeException {
    }
}
