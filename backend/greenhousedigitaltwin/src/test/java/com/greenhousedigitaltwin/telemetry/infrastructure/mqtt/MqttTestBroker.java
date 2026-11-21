package com.greenhousedigitaltwin.telemetry.infrastructure.mqtt;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;

final class MqttTestBroker extends GenericContainer<MqttTestBroker> {

    private static final int MQTT_PORT = 1883;

    MqttTestBroker() {
        super("docker.io/library/eclipse-mosquitto:2");
        withExposedPorts(MQTT_PORT);
        withCreateContainerCmdModifier(command -> command.getHostConfig().withPortBindings(
                new PortBinding(Ports.Binding.bindIp("127.0.0.1"), ExposedPort.tcp(MQTT_PORT))));
        withCopyToContainer(Transferable.of("listener 1883\nallow_anonymous true\n"),
                "/mosquitto/config/test.conf");
        withCommand("mosquitto", "-c", "/mosquitto/config/test.conf");
    }

    String uri() {
        return "tcp://127.0.0.1:" + getMappedPort(MQTT_PORT);
    }
}
