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

Conceptual payload:

```json
{
  "messageId": "MSG-001",
  "deviceId": "ARD-001",
  "sensorId": "TEMP-001",
  "measurementType": "TEMPERATURE",
  "value": 25.4,
  "unit": "CELSIUS",
  "measuredAt": "2026-09-22T14:32:15Z"
}
```

The backend adds `receivedAt` when the message is received.

## 6. Telemetry Processing

```text
MQTT Message
     ↓
Structural Validation
     ↓
Semantic Validation
     ↓
Source Validation
     ↓
Duplicate Detection
     ↓
Physical Plausibility
     ↓
Accepted Measurement
     ↓
Persistence + Twin Update
```

Invalid messages must not update the Digital Twin.

## 7. QoS

Initial MQTT QoS is **QoS 1**.

This means the system must tolerate duplicate delivery.

Therefore:
- `messageId` is required;
- duplicate detection is required;
- database uniqueness provides the final persistence integrity guarantee.

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

Security details are governed by the corresponding ADRs and security requirements.
