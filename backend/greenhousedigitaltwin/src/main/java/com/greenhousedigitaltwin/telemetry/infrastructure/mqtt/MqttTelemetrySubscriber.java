package com.greenhousedigitaltwin.telemetry.infrastructure.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongArray;

final class MqttTelemetrySubscriber implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(MqttTelemetrySubscriber.class);
    private static final long QUIESCE_MILLIS = 2_000;
    private static final long DISCONNECT_MILLIS = 1_000;

    private final MqttTelemetryProperties settings;
    private final MqttTelemetryMessageHandler handler;
    private final AtomicLongArray rejections = new AtomicLongArray(
            MqttTelemetryMessageHandler.Outcome.values().length + 1);
    private volatile Session session;

    MqttTelemetrySubscriber(MqttTelemetryProperties settings, MqttTelemetryMessageHandler handler) {
        this.settings = settings;
        this.handler = handler;
    }

    @Override
    public synchronized void start() {
        if (session != null && !session.executor.isTerminated()) {
            return;
        }
        session = new Session();
        session.executor.execute(session::connect);
    }

    @Override
    public void stop() {
        Session current = session;
        if (current != null) {
            current.stop();
        }
    }

    @Override
    public boolean isRunning() {
        return state() != State.STOPPED;
    }

    State state() {
        Session current = session;
        return current == null ? State.STOPPED : current.state;
    }

    enum State { STOPPED, CONNECTING, CONNECTED, READY, RECONNECTING }

    private MqttConnectOptions connectionOptions() {
        var options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        options.setCleanSession(settings.cleanSession());
        options.setAutomaticReconnect(false);
        options.setConnectionTimeout(Math.toIntExact(settings.connectTimeout().toSeconds()));
        options.setHttpsHostnameVerificationEnabled(true);
        if (settings.username() != null && !settings.username().isBlank()) {
            options.setUserName(settings.username());
        }
        if (settings.password() != null && !settings.password().isEmpty()) {
            options.setPassword(settings.password().toCharArray());
        }
        return options;
    }

    static boolean grantsRequestedQos(int[] grants, int topicCount, int qos) {
        return grants != null && grants.length == topicCount && Arrays.stream(grants).allMatch(grant -> grant == qos);
    }

    private void reportRejection(String reason, int bucket) {
        long count = rejections.incrementAndGet(bucket);
        if (isPowerOfTwo(count)) {
            LOG.warn("MQTT telemetry rejected: reason={}, count={}", reason, count);
        }
    }

    private static boolean isPowerOfTwo(long value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    private enum Failure { CONNECT, SUBSCRIBE, CONNECTION_LOST, PROCESSING, ACKNOWLEDGMENT }

    private final class Session {
        private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, task ->
                Thread.ofPlatform().daemon().name("telemetry-mqtt-lifecycle").unstarted(task));
        private volatile State state = State.CONNECTING;
        private volatile Attempt active;
        private ScheduledFuture<?> retry;
        private long delayMillis = settings.reconnectInitialDelay().toMillis();
        private long failures;

        Session() {
            executor.setRemoveOnCancelPolicy(true);
            executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        }

        void connect() {
            Attempt attempt;
            synchronized (this) {
                if (state == State.STOPPED) {
                    return;
                }
                state = State.CONNECTING;
                attempt = new Attempt(this);
                active = attempt;
            }

            Failure phase = Failure.CONNECT;
            try {
                var client = new MqttClient(settings.brokerUri(), settings.clientId(), new MemoryPersistence());
                attempt.client = client;
                client.setTimeToWait(settings.connectTimeout().toMillis());
                client.setCallback(attempt);
                client.setManualAcks(true);
                client.connect(connectionOptions());
                synchronized (this) {
                    if (state == State.STOPPED || attempt.failed) {
                        return;
                    }
                    state = State.CONNECTED;
                    attempt.connection.countDown();
                }

                phase = Failure.SUBSCRIBE;
                String[] topics = settings.topics().toArray(String[]::new);
                int[] requested = new int[topics.length];
                Arrays.fill(requested, settings.qos());
                int[] grants = client.subscribeWithResponse(topics, requested).getGrantedQos();
                if (!grantsRequestedQos(grants, topics.length, settings.qos())) {
                    recover(attempt, phase);
                    return;
                }
                synchronized (this) {
                    if (state != State.STOPPED && !attempt.failed) {
                        state = State.READY;
                        LOG.debug("MQTT telemetry subscription ready");
                    }
                }
            } catch (MqttException | RuntimeException exception) {
                recover(attempt, phase);
            } finally {
                attempt.connection.countDown();
            }
        }

        synchronized void recover(Attempt attempt, Failure reason) {
            if (state == State.STOPPED || active != attempt || attempt.failed) {
                return;
            }
            attempt.failed = true;
            attempt.connection.countDown();
            state = State.RECONNECTING;
            long waitMillis = delayMillis;
            delayMillis = Math.min(delayMillis * 2, settings.reconnectMaxDelay().toMillis());
            if (isPowerOfTwo(++failures)) {
                LOG.warn("MQTT telemetry recovery scheduled: reason={}, attempts={}, backoffMs={}",
                        reason, failures, waitMillis);
            }
            // Close promptly but keep all connection operations off Paho's callback thread.
            executor.execute(() -> {
                attempt.close();
                synchronized (this) {
                    if (state != State.STOPPED) {
                        retry = executor.schedule(this::connect, waitMillis, TimeUnit.MILLISECONDS);
                    }
                }
            });
        }

        synchronized void acknowledged(Attempt attempt) {
            if (active == attempt && state == State.READY) {
                failures = 0;
                delayMillis = settings.reconnectInitialDelay().toMillis();
            }
        }

        void stop() {
            synchronized (this) {
                if (state != State.STOPPED) {
                    state = State.STOPPED;
                    if (retry != null) {
                        retry.cancel(false);
                    }
                    Attempt attempt = active;
                    if (attempt != null) {
                        attempt.connection.countDown();
                        executor.execute(attempt::close);
                    }
                    executor.shutdown();
                }
            }
            try {
                long waitMillis = settings.connectTimeout().toMillis() + QUIESCE_MILLIS + DISCONNECT_MILLIS;
                if (!executor.awaitTermination(waitMillis, TimeUnit.MILLISECONDS)) {
                    LOG.warn("MQTT shutdown deadline reached; client cleanup remains scheduled");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private final class Attempt implements MqttCallback {
        private final Session owner;
        private final CountDownLatch connection = new CountDownLatch(1);
        private volatile boolean failed;
        private volatile MqttClient client;

        Attempt(Session owner) {
            this.owner = owner;
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            try {
                // A resumed session can fill Paho's callback queue before SUBACK arrives.
                // Consume eligible deliveries after CONNACK without advertising readiness yet.
                connection.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                owner.recover(this, Failure.PROCESSING);
                return;
            }
            State currentState = owner.state;
            if (failed || owner.active != this || (currentState != State.CONNECTED && currentState != State.READY)) {
                return;
            }

            try {
                if (message.getQos() != settings.qos() || message.isRetained()) {
                    reportRejection("UNEXPECTED_DELIVERY_PROFILE", rejections.length() - 1);
                } else {
                    var result = settings.topics().contains(topic)
                            ? handler.handle(topic, message.getPayload())
                            : MqttTelemetryMessageHandler.Outcome.INVALID_TOPIC;
                    switch (result) {
                        case PERSISTED, ALREADY_PROCESSED -> { }
                        default -> reportRejection(result.name(), result.ordinal());
                    }
                }
            } catch (RuntimeException exception) {
                owner.recover(this, Failure.PROCESSING);
                return;
            }

            try {
                MqttClient current = client;
                if (!failed && current != null && message.getQos() > 0) {
                    current.messageArrivedComplete(message.getId(), message.getQos());
                    owner.acknowledged(this);
                }
            } catch (MqttException | RuntimeException exception) {
                owner.recover(this, Failure.ACKNOWLEDGMENT);
            }
        }

        @Override
        public void connectionLost(Throwable cause) {
            owner.recover(this, Failure.CONNECTION_LOST);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }

        void close() {
            MqttClient current = client;
            if (current == null) {
                return;
            }
            try {
                if (current.isConnected()) {
                    current.disconnectForcibly(QUIESCE_MILLIS, DISCONNECT_MILLIS);
                }
                current.close(true);
            } catch (MqttException exception) {
                // A synchronous CONNECT timeout can leave Paho's in-flight connection attempt active.
                try {
                    current.disconnectForcibly(0, DISCONNECT_MILLIS, false);
                    current.close(true);
                } catch (MqttException cleanupFailure) {
                    LOG.warn("MQTT client close did not complete");
                }
            } finally {
                client = null;
            }
        }
    }
}
