# Communication Architecture

## 1. Communication Channels

GreenhouseDigitalTwin uses:

- HTTP/REST between React and backend;
- MQTT between backend and physical device;
- PostgreSQL connection between backend and persistence layer.

## 2. MQTT Role

MQTT is the transport mechanism for physical-system communication.

It provides:
- telemetry transport;
- command transport;
- acknowledgement transport;
- physical-state feedback transport.

MQTT delivery does not establish that the physical world changed.

## 3. MQTT Broker

The broker mediates communication between backend and physical devices.

```text
Backend
  │
  ▼
MQTT Broker
  │
  ▼
Arduino
```

The backend does not need a direct network connection to every device.

## 4. Topics

Initial topic structure:

```text
greenhouse/{greenhouseId}/telemetry
greenhouse/{greenhouseId}/state/{component}
greenhouse/{greenhouseId}/command/{component}
greenhouse/{greenhouseId}/event/{eventType}
```

Examples:

```text
greenhouse/GH-001/telemetry
greenhouse/GH-001/state/light
greenhouse/GH-001/command/light
greenhouse/GH-001/event/command
```

## 5. Telemetry Payload

The MQTT payload is a JSON object with these eight required fields:

```json
{
  "measurementId": "MEAS-001",
  "messageId": "MSG-001",
  "deviceId": "ARD-001",
  "sensorId": "TEMP-001",
  "measurementType": "TEMPERATURE",
  "value": 25.4,
  "unit": "CELSIUS",
  "measuredAt": "2026-09-22T14:32:15Z"
}
```

The backend assigns `receivedAt` when creating the validated measurement.
Publishers supply both identifiers and preserve them across retries and across
HTTP and MQTT submissions of the same event. The adapter passes both through
unchanged. See the [validation specification](../03-design/Telemetry%20Validation%20Specification.md#8-transactional-idempotency).

The subscriber accepts one UTF-8 JSON object. Duplicate keys, trailing JSON,
unknown fields, missing fields, and type coercion are rejected. Identifiers
are nonblank. Measurement and message IDs are limited to 100 characters, and
device and sensor IDs to 50. `measuredAt` must include an offset or `Z`, with
zero to six fractional digits. The greenhouse topic segment is nonblank,
limited to 50 characters, and excludes wildcard and control characters.

## 6. Telemetry Processing

```text
MQTT Message
     ↓
Structural Validation
     ↓
Topic Greenhouse / Device Check
     ↓
Semantic Validation
     ↓
Source Validation
     ↓
Physical Plausibility
     ↓
Accepted Measurement
     ↓
Transactional Idempotency + History Insert + Conditional Twin Update
```

Invalid messages must not update the Digital Twin.

## 7. QoS

DT-012 uses MQTT 3.1.1, QoS 1 for publication and subscription, and
`retain=false` for telemetry publishers. The subscriber uses a stable client ID
and `cleanSession=false`. It enables manual acknowledgment before CONNECT and
verifies the granted subscription QoS before reporting readiness. Processing
starts serially. The reconnect delay doubles up to the configured maximum and
resets after a terminal acknowledgment on a ready connection. The subscriber
checks exact configured topics and actual delivery QoS. Messages already
queued in a persistent session can be handled after CONNACK while the client
waits for the new SUBACK. Readiness still requires an exact grant for every
configured topic.

The broker's PUBACK to a publisher confirms protocol delivery to the broker.
The subscriber sends its own PUBACK after the ingestion use case returns
`PERSISTED` or `ALREADY_PROCESSED` and the database transaction has committed.
Permanent rejections, including malformed or oversized payloads and identity
conflicts, are acknowledged after sanitized reporting for deliveries with an
acknowledgment. Transient and unexpected failures leave the delivery
unacknowledged where possible and trigger a disconnect and scheduled reconnect.
Withholding PUBACK on an open connection alone does not trigger immediate
redelivery. If the session survives and the broker redelivers after a database
commit, transactional idempotency prevents another write. PUBACK carries no
application outcome, and DT-012 defines no application receipt topic.

Broker availability is independent of HTTP ingestion. Shutdown cancels retries,
stops accepting deliveries and gives work already in progress time to finish.
An unfinished delivery is left for session recovery. The adapter bounds its
shutdown wait. Client cleanup can still depend on an in-progress database call
returning.

MQTT ingestion defaults to disabled. Connection and subscription settings and
the payload cap are externalized through `application.yml`. The cap defaults
to 4096 bytes. Local development uses the existing loopback host binding.
Session recovery assumes the broker retains the session and has capacity to
queue QoS 1 deliveries. A publisher can receive PUBACK with no subscriber
session available. Queue exhaustion, lost sessions, or broker restart can
lose pending messages. The broker and database do not share a transaction,
so the practical guarantee is duplicate-tolerant processing of deliveries
retained by the session. Persistence across broker restarts is deferred.

## 8. Retained Messages

Retained physical-state messages may be considered for state synchronization.

Commands should not be retained initially because a stale command must not be replayed after reconnection.

## 9. Command Payload

Conceptual command:

```json
{
  "commandId": "CMD-000001",
  "actuatorId": "LIGHT-001",
  "action": "TURN_ON",
  "createdAt": "2026-09-25T13:00:00Z"
}
```

## 10. Command Acknowledgement

Conceptual acknowledgement:

```json
{
  "commandId": "CMD-000001",
  "actuatorId": "LIGHT-001",
  "status": "ACKNOWLEDGED",
  "receivedAt": "2026-09-25T13:00:01Z"
}
```

An acknowledgement means the physical device received/accepted the command.

It does not mean the actuator reached the expected physical state.

## 11. Physical-State Feedback

Conceptual state message:

```json
{
  "actuatorId": "LIGHT-001",
  "state": "ON",
  "observedAt": "2026-09-25T13:00:02Z"
}
```

The Digital Twin uses this as physical observation.

Initial V1 state feedback is based on device/GPIO-reported state.

## 12. Command Confirmation

Initial mapping:

```text
TURN_ON  → expected ON
TURN_OFF → expected OFF
```

Confirmation requires:

```text
ACKNOWLEDGED
      +
expected physical-state feedback
      =
CONFIRMED
```

Therefore:

```text
MQTT publish succeeded
        ≠
ACKNOWLEDGED
        ≠
CONFIRMED
```

## 13. Command Lifecycle

```text
REQUESTED
    │
    ▼
  SENT
   ├──────────────► TIMEOUT
   ▼
ACKNOWLEDGED
   ├──────────────► TIMEOUT
   ▼
CONFIRMED

Failure can occur where explicitly defined:
REQUESTED/SENT/ACKNOWLEDGED → FAILED
```

Terminal states:
- CONFIRMED;
- FAILED;
- TIMEOUT.

Late messages must not resurrect terminal commands.

## 14. Retry Semantics

A retry is a new command.

Example:

```text
CMD-001 → TIMEOUT

retry

CMD-002 → REQUESTED
```

The original command remains part of history.

## 15. Communication Failures

The system must distinguish:

- broker unavailable;
- publication failure;
- device unavailable;
- missing acknowledgement;
- missing physical-state confirmation;
- explicit device error.

A timeout means expected evidence did not arrive within the configured deadline. It is not by itself proof that the physical actuator failed.

## 16. MQTT Adapter Boundary

The MQTT adapter owns:
- topic names;
- serialization/deserialization;
- MQTT client configuration;
- QoS;
- broker connection handling.

DT-012 checks that the greenhouse in the topic matches the registered
greenhouse for the payload device through an application port. The shared
ingestion path remains responsible for device and sensor validation. The
greenhouse lookup precedes ingestion and runs outside the write transaction.
Concurrent reassignment of a registered device can therefore race with this
check.

The application/domain owns:
- telemetry validation rules;
- command lifecycle;
- state transitions;
- confirmation semantics;
- business decisions.

## 17. Device Reconnection

When a physical device reconnects, it should be able to publish current physical state and resume telemetry.

The backend uses those observations to reconstruct current Digital Twin state.

## 18. Security Boundary

MQTT communication is an untrusted external boundary.

The architecture must support:
- device authentication;
- authorization of commands;
- secure transport;
- credential protection.

Topic-to-device consistency checks the claimed source relationship. Publisher
authentication, device-scoped authorization and verified TLS are separate
deployment hardening work. The anonymous local broker must stay within the
existing development network. The adapter's byte limit is checked after
Paho receives the payload. Broker packet and queue limits, publisher rate
controls, and advanced replay protection remain deployment hardening work.
