package com.greenhousedigitaltwin.telemetry.infrastructure.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PahoMqttSmokeTest {

    private static final int QOS = 1;
    private static final String TOPIC = "greenhouse/smoke/telemetry";

    @Test
    void redeliversUntilManuallyAcknowledged() throws Exception {
        try (var broker = new MqttTestBroker()) {
            broker.start();
            verifyDelivery(broker.uri());
        }
    }

    private static void verifyDelivery(String uri) throws Exception {
        BlockingQueue<MqttMessage> deliveries = new LinkedBlockingQueue<>();
        byte[] firstPayload = "first".getBytes(StandardCharsets.UTF_8);
        byte[] nextPayload = "next".getBytes(StandardCharsets.UTF_8);

        try (var publisher = new MqttClient(uri, "smoke-publisher", new MemoryPersistence());
             var subscriber = new MqttClient(uri, "smoke-subscriber", new MemoryPersistence())) {
            publisher.setTimeToWait(10_000);
            subscriber.setTimeToWait(10_000);
            subscriber.setManualAcks(true);
            subscriber.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    deliveries.add(message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            try {
                publisher.connect(options(true));
                subscriber.connect(options(false));
                var subscription = subscriber.subscribeWithResponse(TOPIC, QOS);
                assertArrayEquals(new int[]{QOS}, subscription.getGrantedQos());

                publisher.publish(TOPIC, firstPayload, QOS, false);
                MqttMessage first = receive(deliveries, firstPayload);
                subscriber.disconnect();

                assertTrue(subscriber.connectWithResult(options(false)).getSessionPresent());
                MqttMessage retry = receive(deliveries, firstPayload);
                assertTrue(retry.isDuplicate());
                assertEquals(first.getId(), retry.getId());
                subscriber.messageArrivedComplete(retry.getId(), retry.getQos());
                subscriber.disconnect();

                assertTrue(subscriber.connectWithResult(options(false)).getSessionPresent());
                publisher.publish(TOPIC, nextPayload, QOS, false);
                MqttMessage next = receive(deliveries, nextPayload);
                subscriber.messageArrivedComplete(next.getId(), next.getQos());
            } finally {
                try {
                    if (subscriber.isConnected()) {
                        subscriber.disconnect();
                    }
                } finally {
                    if (publisher.isConnected()) {
                        publisher.disconnect();
                    }
                }
            }
        }
    }

    private static MqttConnectOptions options(boolean cleanSession) {
        var options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        options.setCleanSession(cleanSession);
        options.setConnectionTimeout(5);
        return options;
    }

    private static MqttMessage receive(BlockingQueue<MqttMessage> deliveries, byte[] expected) throws Exception {
        MqttMessage message = deliveries.poll(10, TimeUnit.SECONDS);
        assertNotNull(message, "Timed out waiting for MQTT delivery");
        assertEquals(QOS, message.getQos());
        assertArrayEquals(expected, message.getPayload());
        return message;
    }
}
