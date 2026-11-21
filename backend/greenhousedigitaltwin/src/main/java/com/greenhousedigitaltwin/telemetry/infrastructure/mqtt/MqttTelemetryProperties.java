package com.greenhousedigitaltwin.telemetry.infrastructure.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;

@ConfigurationProperties("telemetry.mqtt")
record MqttTelemetryProperties(
        boolean enabled, String brokerUri, String clientId, List<String> topics, int qos,
        boolean cleanSession, Duration connectTimeout, Duration reconnectInitialDelay,
        Duration reconnectMaxDelay, int maxPayloadBytes, String username, String password) {

    MqttTelemetryProperties {
        topics = topics == null ? List.of() : List.copyOf(topics);
        if (enabled) {
            require(validBroker(brokerUri), "broker-uri");
            require(clientId != null && clientId.matches("[A-Za-z0-9._-]{1,128}"), "client-id");
            require(!topics.isEmpty() && new HashSet<>(topics).size() == topics.size()
                    && topics.stream().allMatch(topic -> MqttTelemetryMessageHandler.greenhouseId(topic) != null),
                    "topics");
            require(qos == 1, "qos");
            require(!cleanSession, "clean-session");
            require(inRange(connectTimeout, Duration.ofSeconds(1), Duration.ofSeconds(30)), "connect-timeout");
            require(inRange(reconnectInitialDelay, Duration.ofMillis(1), Duration.ofMinutes(5)),
                    "reconnect-initial-delay");
            require(inRange(reconnectMaxDelay, reconnectInitialDelay, Duration.ofMinutes(5)),
                    "reconnect-max-delay");
            require(maxPayloadBytes > 0, "max-payload-bytes");
            require(password == null || password.isEmpty() || (username != null && !username.isBlank()), "username");
        }
    }

    private static boolean validBroker(String value) {
        try {
            URI uri = URI.create(value);
            return ("tcp".equals(uri.getScheme()) || "ssl".equals(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null
                    && uri.getPort() != 0 && uri.getPort() <= 65535
                    && uri.getRawPath().isEmpty() && uri.getRawQuery() == null && uri.getRawFragment() == null;
        } catch (IllegalArgumentException | NullPointerException exception) {
            return false;
        }
    }

    private static boolean inRange(Duration value, Duration min, Duration max) {
        return value != null && value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    private static void require(boolean condition, String property) {
        if (!condition) {
            throw new IllegalArgumentException("Invalid MQTT configuration for " + property);
        }
    }

    @Override
    public String toString() {
        return "MqttTelemetryProperties[enabled=" + enabled + "]";
    }
}
