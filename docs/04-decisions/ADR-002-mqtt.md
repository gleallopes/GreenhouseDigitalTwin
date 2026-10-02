# ADR-002 — MQTT for Physical Communication

- **Status:** Accepted
- **Date:** 2026-09-15
- **Decision:** Use MQTT as the communication protocol between the backend and physical greenhouse devices.

## Context

The system requires bidirectional communication between Spring Boot and an Arduino-based physical greenhouse.

The communication model must support asynchronous device communication, intermittent connectivity, lightweight messages, and command/telemetry flows.

## Decision

Use MQTT through a broker.

Initial architecture:

```text
Spring Boot ↔ MQTT Broker ↔ Arduino
```

Initial topic structure:

```text
greenhouse/{greenhouseId}/telemetry
greenhouse/{greenhouseId}/command/{component}
greenhouse/{greenhouseId}/event/{eventType}
greenhouse/{greenhouseId}/state/{component}
```

MQTT is an infrastructure concern. Domain and application layers must not depend on MQTT-specific types.

Initial telemetry and command communication use QoS 1 where appropriate.

## Important Semantic Rule

MQTT delivery does not mean physical success.

```text
MQTT message delivered
        ≠
Physical state confirmed
```

Command confirmation requires physical feedback.

## Consequences

### Positive

- Natural fit for IoT telemetry.
- Supports publish/subscribe communication.
- Lightweight for Arduino-class devices.
- Provides QoS semantics.
- Supports asynchronous bidirectional communication.

### Negative

- Introduces a broker as another infrastructure component.
- Requires handling duplicate delivery under QoS 1.
- Requires explicit command correlation and state confirmation.

## Rejected Alternative

Direct HTTP communication between Spring Boot and Arduino was not selected as the primary physical communication mechanism because the project's communication model benefits from asynchronous broker-mediated messaging.

## Revisit When

Reconsider if physical communication requirements, device constraints, or operational evidence make another protocol more appropriate.
