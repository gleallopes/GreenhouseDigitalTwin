package com.greenhousedigitaltwin.telemetry.infrastructure.mqtt;

import com.greenhousedigitaltwin.telemetry.application.port.DeviceGreenhouseRegistry;
import com.greenhousedigitaltwin.telemetry.application.port.IngestTelemetryUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "telemetry.mqtt", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MqttTelemetryProperties.class)
class MqttTelemetryConfiguration {

    @Bean
    MqttTelemetrySubscriber mqttTelemetrySubscriber(MqttTelemetryProperties properties,
            IngestTelemetryUseCase ingestion, DeviceGreenhouseRegistry registry) {
        var handler = new MqttTelemetryMessageHandler(ingestion, registry, properties.maxPayloadBytes());
        return new MqttTelemetrySubscriber(properties, handler);
    }
}
