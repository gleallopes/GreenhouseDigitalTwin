package com.greenhousedigitaltwin.telemetry.infrastructure.mqtt;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** A loopback relay that observes subscriber PUBACKs and can interrupt delivery to the broker. */
final class MqttTestProxy implements AutoCloseable {

    private final ServerSocket listener = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
    private final Set<Socket> connections = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean available = new AtomicBoolean(true);
    private final AtomicBoolean downgradeGrants = new AtomicBoolean();
    private final AtomicBoolean dropNextAck = new AtomicBoolean();
    private final AtomicInteger forwardedAcks = new AtomicInteger();
    private final AtomicInteger droppedAcks = new AtomicInteger();
    private final ConcurrentLinkedQueue<Long> connectionAttempts = new ConcurrentLinkedQueue<>();

    MqttTestProxy(int brokerPort) throws IOException {
        Thread.startVirtualThread(() -> accept(brokerPort));
    }

    String uri() {
        return "tcp://127.0.0.1:" + listener.getLocalPort();
    }

    int forwardedAcks() {
        return forwardedAcks.get();
    }

    int droppedAcks() {
        return droppedAcks.get();
    }

    int connectionAttempts() {
        return connectionAttempts.size();
    }

    List<Long> attemptTimes() {
        return List.copyOf(connectionAttempts);
    }

    void dropNextAck() {
        dropNextAck.set(true);
    }

    void downgradeGrants(boolean value) {
        downgradeGrants.set(value);
    }

    void available(boolean value) {
        available.set(value);
        if (!value) {
            connections.forEach(MqttTestProxy::close);
        }
    }

    private void accept(int brokerPort) {
        while (!listener.isClosed()) {
            try {
                Socket client = listener.accept();
                connectionAttempts.add(System.nanoTime());
                if (!available.get()) {
                    close(client);
                } else {
                    connections.add(client);
                    Thread.startVirtualThread(() -> relay(client, brokerPort));
                }
            } catch (IOException exception) {
                if (!listener.isClosed()) {
                    throw new IllegalStateException("Test proxy accept failed", exception);
                }
            }
        }
    }

    private void relay(Socket client, int brokerPort) {
        try (client; Socket broker = new Socket("127.0.0.1", brokerPort)) {
            Thread downstream = Thread.startVirtualThread(() -> {
                try {
                    relayPackets(broker, client, false);
                } catch (IOException ignored) {
                    // One side closed the test connection.
                } finally {
                    close(client);
                }
            });
            try {
                relayPackets(client, broker, true);
            } finally {
                close(broker);
                downstream.join();
            }
        } catch (IOException ignored) {
            // Deliberate socket interruption also reaches this path.
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            connections.remove(client);
        }
    }

    private void relayPackets(Socket from, Socket to, boolean fromSubscriber) throws IOException {
        var input = new DataInputStream(from.getInputStream());
        var output = to.getOutputStream();
        int header;
        while ((header = input.read()) != -1) {
            byte[] encodedLength = new byte[4];
            int count = 0;
            int length = 0;
            int next;
            do {
                next = input.readUnsignedByte();
                if (count == encodedLength.length) {
                    throw new IOException("Invalid MQTT packet length");
                }
                encodedLength[count] = (byte) next;
                length |= (next & 127) << (7 * count++);
            } while ((next & 128) != 0);
            byte[] body = input.readNBytes(length);
            if (body.length != length) {
                return;
            }

            int packetType = header >>> 4;
            boolean acknowledgment = fromSubscriber && packetType == 4;
            if (!fromSubscriber && packetType == 9 && downgradeGrants.get()) {
                // Preserve the SUBACK packet identifier and simulate a broker granting QoS 0.
                Arrays.fill(body, 2, body.length, (byte) 0);
            }
            if (acknowledgment && dropNextAck.compareAndSet(true, false)) {
                droppedAcks.incrementAndGet();
                return;
            }
            output.write(header);
            output.write(encodedLength, 0, count);
            output.write(body);
            output.flush();
            if (acknowledgment) {
                forwardedAcks.incrementAndGet();
            }
        }
    }

    @Override
    public void close() throws IOException {
        listener.close();
        available(false);
    }

    private static void close(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // The socket is already being torn down.
        }
    }
}
