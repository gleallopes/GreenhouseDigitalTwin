package com.greenhousedigitaltwin.telemetry.infrastructure.mqtt;

import com.greenhousedigitaltwin.twin.application.UpdateTwinStateUseCase;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class MqttTelemetryIntegrationTest {

    private static final String TOPIC = "greenhouse/gh/telemetry";
    private static final String OTHER_TOPIC = "greenhouse/other/telemetry";
    private static final String UNKNOWN_TOPIC = "greenhouse/unregistered/telemetry";
    private static final String CURRENT = "2026-09-22T14:32:15.123456Z";
    private static final String LATER = "2026-09-22T14:32:16Z";
    private static final String CLIENT_ID = "dt012-" + UUID.randomUUID();

    @Container @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @Container
    static final MqttTestBroker BROKER = new MqttTestBroker();

    static MqttTestProxy proxy;

    @DynamicPropertySource
    static void mqttProperties(DynamicPropertyRegistry registry) {
        registry.add("telemetry.mqtt.enabled", () -> true);
        registry.add("telemetry.mqtt.broker-uri", MqttTelemetryIntegrationTest::proxyUri);
        registry.add("telemetry.mqtt.client-id", () -> CLIENT_ID);
        registry.add("telemetry.mqtt.topics", () -> TOPIC + "," + OTHER_TOPIC + "," + UNKNOWN_TOPIC);
        registry.add("telemetry.mqtt.connect-timeout", () -> "1s");
        registry.add("telemetry.mqtt.reconnect-initial-delay", () -> "100ms");
        registry.add("telemetry.mqtt.reconnect-max-delay", () -> "400ms");
    }

    private static synchronized String proxyUri() {
        if (proxy == null) {
            try {
                proxy = new MqttTestProxy(URI.create(BROKER.uri()).getPort());
                proxy.available(false);
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot start MQTT test relay", exception);
            }
        }
        return proxy.uri();
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired MqttTelemetrySubscriber subscriber;
    @MockitoSpyBean UpdateTwinStateUseCase twinUpdate;
    @LocalServerPort int httpPort;
    private MqttClient publisher;

    @BeforeAll
    static void verifiesColdStartupWithUnavailableBroker(@Autowired MqttTelemetrySubscriber subscriber) {
        try {
            await().atMost(Duration.ofSeconds(10)).until(() -> proxy.connectionAttempts() >= 2);
            assertThat(subscriber.state()).isIn(MqttTelemetrySubscriber.State.CONNECTING,
                    MqttTelemetrySubscriber.State.RECONNECTING);
        } finally {
            proxy.available(true);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        ready();
        jdbc.update("DELETE FROM twin_state");
        jdbc.update("DELETE FROM measurement");
        jdbc.update("INSERT INTO greenhouse VALUES ('gh','Greenhouse'), ('other','Other') ON CONFLICT DO NOTHING");
        jdbc.update("""
                INSERT INTO device VALUES ('device','gh','Device'), ('other-device','gh','Other device')
                ON CONFLICT DO NOTHING
                """);
        jdbc.update("""
                INSERT INTO sensor VALUES ('sensor','device','TEMPERATURE','CELSIUS') ON CONFLICT DO NOTHING
                """);
        publisher = new MqttClient(BROKER.uri(), UUID.randomUUID().toString(), new MemoryPersistence());
        publisher.setTimeToWait(5_000);
        var options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        publisher.connect(options);
    }

    @AfterEach
    void closePublisher() throws Exception {
        if (publisher != null) {
            try (var client = publisher) {
                if (client.isConnected()) {
                    client.disconnect();
                }
            } finally {
                publisher = null;
            }
        }
    }

    @AfterAll
    static void closeProxy(@Autowired MqttTelemetrySubscriber subscriber) throws IOException {
        subscriber.stop();
        if (proxy != null) {
            proxy.close();
        }
    }

    @Test
    void persistsAndAcknowledgesDuplicatesAndBothKindsOfIdentityConflict() throws Exception {
        String first = event("first", 25, CURRENT);
        publishAndAwaitAck(TOPIC, first);
        assertThat(jdbc.queryForMap("SELECT measurement_id, message_id, value, sensor_id FROM measurement"))
                .containsEntry("measurement_id", "m-first").containsEntry("message_id", "msg-first")
                .containsEntry("value", 25.0).containsEntry("sensor_id", "sensor");
        Map<String, Object> state = twin();
        assertThat(state).containsEntry("value", 25.0);

        publishAndAwaitAck(TOPIC, first);
        publishAndAwaitAck(TOPIC, event("first", 26, LATER).replace("m-first", "m-other"));
        publishAndAwaitAck(TOPIC, event("first", 27, LATER).replace("msg-first", "msg-other"));
        assertThat(history()).isEqualTo(1);
        assertThat(twin()).isEqualTo(state);
        assertThat(jdbc.queryForObject("SELECT value FROM measurement", Double.class)).isEqualTo(25.0);
    }

    @Test
    void preservesOrderingForOlderAndEqualTimestamps() throws Exception {
        publishAndAwaitAck(TOPIC, event("current", 25, CURRENT));
        Map<String, Object> state = twin();
        publishAndAwaitAck(TOPIC, event("older", 26, "2026-09-22T14:32:15.123455Z"));
        publishAndAwaitAck(TOPIC, event("equal", 27, CURRENT));
        assertThat(history()).isEqualTo(3);
        assertThat(twin()).isEqualTo(state);
    }

    @Test
    void deduplicatesAcrossBothTransports() throws Exception {
        String httpFirst = event("http", 25, CURRENT);
        assertThat(http(httpFirst).statusCode()).isEqualTo(201);
        Map<String, Object> state = twin();
        publishAndAwaitAck(TOPIC, httpFirst);
        assertThat(history()).isEqualTo(1);
        assertThat(twin()).isEqualTo(state);

        String mqttFirst = event("mqtt", 26, LATER);
        publishAndAwaitAck(TOPIC, mqttFirst);
        var duplicate = http(mqttFirst);
        assertThat(duplicate.statusCode()).isEqualTo(200);
        assertThat(duplicate.body()).contains("ALREADY_PROCESSED");
        assertThat(history()).isEqualTo(2);
        assertThat(twin()).containsEntry("value", 26.0);
    }

    @Test
    void acknowledgesPermanentSourceAndPayloadRejections() throws Exception {
        String valid = event("valid", 25, CURRENT);
        publishAndAwaitAck(TOPIC, valid.replace("\"device\"", "\"unknown\""));
        publishAndAwaitAck(TOPIC, valid.replace("\"sensor\"", "\"unknown\""));
        publishAndAwaitAck(TOPIC, valid.replace("\"device\"", "\"other-device\""));
        publishAndAwaitAck(OTHER_TOPIC, valid);
        publishAndAwaitAck(UNKNOWN_TOPIC, valid);
        publishAndAwaitAck(TOPIC, "{");
        publishAndAwaitAck(TOPIC, valid.replace("\"unit\":\"CELSIUS\",", ""));
        publishAndAwaitAck(TOPIC, valid.replace("TEMPERATURE", "UNKNOWN"));
        publishAndAwaitAck(TOPIC, valid.replace("CELSIUS", "PERCENT"));
        publishAndAwaitAck(TOPIC, event("impossible", 500, CURRENT));
        assertThat(history()).isZero();
        assertThat(jdbc.queryForList("SELECT * FROM twin_state")).isEmpty();

        String multibyte = event("é", 25, CURRENT);
        String exact = multibyte + " ".repeat(4096 - multibyte.getBytes(UTF_8).length);
        publishAndAwaitAck(TOPIC, exact + " ");
        assertThat(history()).isZero();
        publishAndAwaitAck(TOPIC, exact);
        assertThat(history()).isEqualTo(1);
    }

    @Test
    void rollsBackBeforeAckAndRecoversUnacknowledgedDelivery() throws Exception {
        var retryEntered = new CountDownLatch(1);
        var releaseRetry = new CountDownLatch(1);
        doAnswer(invocation -> {
            throw new IllegalStateException("simulated twin write failure");
        }).doAnswer(invocation -> {
            retryEntered.countDown();
            if (!releaseRetry.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test did not release retry");
            }
            return invocation.callRealMethod();
        }).when(twinUpdate).updateFrom(any());

        int startingAcks = proxy.forwardedAcks();
        try {
            publish(TOPIC, event("rollback", 25, CURRENT));
            assertThat(retryEntered.await(15, TimeUnit.SECONDS)).isTrue();
            assertThat(proxy.forwardedAcks()).isEqualTo(startingAcks);
            assertThat(history()).isZero();
            assertThat(jdbc.queryForList("SELECT * FROM twin_state")).isEmpty();
        } finally {
            releaseRetry.countDown();
        }
        awaitAcks(startingAcks + 1);
        assertThat(history()).isEqualTo(1);
        assertThat(twin()).containsEntry("value", 25.0);
    }

    @Test
    void safelyRedeliversWhenCommittedAckNeverReachesBroker() throws Exception {
        int forwarded = proxy.forwardedAcks();
        int dropped = proxy.droppedAcks();
        proxy.dropNextAck();
        publish(TOPIC, event("lost-ack", 25, CURRENT));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(proxy.droppedAcks()).isEqualTo(dropped + 1);
            assertThat(history()).isEqualTo(1);
        });
        Map<String, Object> committed = twin();
        awaitAcks(forwarded + 1);
        assertThat(history()).isEqualTo(1);
        assertThat(twin()).isEqualTo(committed);
    }

    @Test
    void retriesStartupWhileHttpRemainsAvailableAndDrainsPersistentSession() throws Exception {
        subscriber.stop();
        assertThat(subscriber.state()).isEqualTo(MqttTelemetrySubscriber.State.STOPPED);
        int beforeOutage = proxy.connectionAttempts();
        proxy.available(false);
        try {
            int backlog = 18;
            int queuedAck = proxy.forwardedAcks() + backlog;
            for (int index = 0; index < backlog; index++) {
                publish(TOPIC, event("queued-" + index, 25, CURRENT));
            }
            subscriber.start();
            subscriber.start();
            await().atMost(Duration.ofSeconds(10))
                    .until(() -> proxy.connectionAttempts() >= beforeOutage + 4);
            var attempts = proxy.attemptTimes();
            for (int index = 0; index < 3; index++) {
                long elapsed = attempts.get(beforeOutage + index + 1) - attempts.get(beforeOutage + index);
                assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofMillis(100L << index).toNanos());
            }
            assertThat(subscriber.state()).isIn(MqttTelemetrySubscriber.State.CONNECTING,
                    MqttTelemetrySubscriber.State.RECONNECTING);
            assertThat(http(event("during-outage", 26, LATER)).statusCode()).isEqualTo(201);
            assertThat(history()).isEqualTo(1);
            proxy.available(true);
            ready();
            awaitAcks(queuedAck);
            assertThat(history()).isEqualTo(backlog + 1);
            assertThat(twin()).containsEntry("value", 26.0);
        } finally {
            proxy.available(true);
        }
    }

    @Test
    void cancelsReconnectOnShutdownAndCanStartAgain() {
        proxy.available(false);
        try {
            await().atMost(Duration.ofSeconds(10))
                    .until(() -> subscriber.state() == MqttTelemetrySubscriber.State.RECONNECTING);
            subscriber.stop();
            subscriber.stop();
            int stoppedAttempts = proxy.connectionAttempts();
            await().during(Duration.ofMillis(700)).atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
                assertThat(subscriber.isRunning()).isFalse();
                assertThat(proxy.connectionAttempts()).isEqualTo(stoppedAttempts);
            });
        } finally {
            proxy.available(true);
            subscriber.start();
            ready();
        }
    }

    @Test
    void shutdownDoesNotAckAnUnfinishedTransaction() throws Exception {
        var updateEntered = new CountDownLatch(1);
        var releaseUpdate = new CountDownLatch(1);
        doAnswer(invocation -> {
            updateEntered.countDown();
            if (!releaseUpdate.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test did not release twin update");
            }
            return invocation.callRealMethod();
        }).when(twinUpdate).updateFrom(any());

        int originalAcks = proxy.forwardedAcks();
        CompletableFuture<Void> stopped = null;
        try {
            publish(TOPIC, event("draining", 25, CURRENT));
            assertThat(updateEntered.await(10, TimeUnit.SECONDS)).isTrue();
            stopped = CompletableFuture.runAsync(subscriber::stop, Thread::startVirtualThread);
            await().atMost(Duration.ofSeconds(3)).until(() -> !subscriber.isRunning());
            assertThat(proxy.forwardedAcks()).isEqualTo(originalAcks);
            assertThat(history()).isZero();
        } finally {
            releaseUpdate.countDown();
            if (stopped != null) {
                stopped.get(10, TimeUnit.SECONDS);
            }
            subscriber.start();
            ready();
        }
        publish(TOPIC, event("after-drain", 26, LATER));
        awaitAcks(originalAcks + 2);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(history()).isEqualTo(2));
        assertThat(twin()).containsEntry("value", 26.0);
    }

    @Test
    void refusesReadinessWhenBrokerGrantIsDowngradedAndRecoversWithExactGrant() {
        subscriber.stop();
        proxy.downgradeGrants(true);
        int attempts = proxy.connectionAttempts();
        try {
            subscriber.start();
            await().atMost(Duration.ofSeconds(10))
                    .until(() -> proxy.connectionAttempts() >= attempts + 2);
            await().during(Duration.ofMillis(650)).atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> assertThat(subscriber.state())
                            .isNotEqualTo(MqttTelemetrySubscriber.State.READY));
        } finally {
            proxy.downgradeGrants(false);
            ready();
        }
    }

    private void publishAndAwaitAck(String topic, String payload) throws Exception {
        int next = proxy.forwardedAcks() + 1;
        publish(topic, payload);
        awaitAcks(next);
    }

    private void publish(String topic, String payload) throws Exception {
        publisher.publish(topic, payload.getBytes(UTF_8), 1, false);
    }

    private void awaitAcks(int expected) {
        await().atMost(Duration.ofSeconds(15)).until(() -> proxy.forwardedAcks() >= expected);
    }

    private void ready() {
        await().atMost(Duration.ofSeconds(15))
                .until(() -> subscriber.state() == MqttTelemetrySubscriber.State.READY);
    }

    private long history() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM measurement", Long.class);
    }

    private Map<String, Object> twin() {
        return jdbc.queryForMap("SELECT * FROM twin_state WHERE sensor_id = 'sensor'");
    }

    private HttpResponse<String> http(String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + httpPort + "/api/v1/telemetry"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try (var client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private String event(String id, int value, String timestamp) {
        return """
                {"measurementId":"m-%s","messageId":"msg-%s","deviceId":"device","sensorId":"sensor",
                 "measurementType":"TEMPERATURE","value":%d,"unit":"CELSIUS","measuredAt":"%s"}
                """.formatted(id, id, value, timestamp);
    }
}
