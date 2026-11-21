package com.greenhousedigitaltwin.telemetry.infrastructure.mqtt;

import com.greenhousedigitaltwin.telemetry.application.port.DeviceGreenhouseRegistry;
import com.greenhousedigitaltwin.telemetry.application.port.IngestTelemetryUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MqttTelemetrySubscriberTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(MqttTelemetryConfiguration.class);

    @Test
    void disabledByDefaultAndExplicitlyDisabledNeverCreateSubscriber() {
        context.run(application -> assertFalse(application.containsBean("mqttTelemetrySubscriber")));
        context.withPropertyValues("telemetry.mqtt.enabled=false", "telemetry.mqtt.broker-uri=bad")
                .run(application -> assertFalse(application.containsBean("mqttTelemetrySubscriber")));
    }

    @Test
    void rejectsInvalidEnabledConfigurationBeforeStartingSubscriber() {
        context.withBean(IngestTelemetryUseCase.class, () -> command -> null)
                .withBean(DeviceGreenhouseRegistry.class, () -> (device, greenhouse) -> false)
                .withPropertyValues("telemetry.mqtt.enabled=true", "telemetry.mqtt.broker-uri=bad")
                .run(application -> assertTrue(application.getStartupFailure() != null));
        assertThrows(IllegalArgumentException.class, () -> settings("tcp://name:private@localhost:1883", 1, false));
        assertThrows(IllegalArgumentException.class, () -> settings("tcp://127.0.0.1", 0, false));
        assertThrows(IllegalArgumentException.class, () -> settings("tcp://127.0.0.1", 1, true));
        assertFalse(settings("tcp://127.0.0.1", 1, false).toString().contains("example-password"));
    }

    @Test
    void requiresOneExactGrantPerTopic() {
        assertTrue(MqttTelemetrySubscriber.grantsRequestedQos(new int[]{1, 1}, 2, 1));
        assertFalse(MqttTelemetrySubscriber.grantsRequestedQos(new int[]{1, 0}, 2, 1));
        assertFalse(MqttTelemetrySubscriber.grantsRequestedQos(new int[]{128}, 1, 1));
        assertFalse(MqttTelemetrySubscriber.grantsRequestedQos(new int[]{1}, 2, 1));
        assertFalse(MqttTelemetrySubscriber.grantsRequestedQos(null, 1, 1));
    }

    private MqttTelemetryProperties settings(String uri, int qos, boolean cleanSession) {
        return new MqttTelemetryProperties(true, uri, "test", List.of("greenhouse/gh/telemetry"),
                qos, cleanSession, Duration.ofSeconds(1), Duration.ofMillis(100), Duration.ofSeconds(1),
                4096, "example-user", "example-password");
    }
}
