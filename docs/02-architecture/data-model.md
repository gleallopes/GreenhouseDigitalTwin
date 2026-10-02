# Data Model

## 1. Purpose

This document defines the conceptual data model and persistence relationships for GreenhouseDigitalTwin.

It does not prescribe a complete SQL schema. Physical database details may evolve during implementation.

## 2. Core Relationships

```text
GREENHOUSE
    │
    ├── 1:N DEVICE
    │       │
    │       ├── 1:N SENSOR
    │       │       │
    │       │       └── 1:N MEASUREMENT
    │       │
    │       └── 1:N ACTUATOR
    │
    ├── 1:N TWIN_STATE
    ├── 1:N COMMAND
    ├── 1:N SIMULATION
    ├── 1:N PREDICTION
    ├── 1:N ANOMALY
    └── 1:N ALERT
```

## 3. Greenhouse

Represents a physical agricultural environment.

Conceptual attributes:
- greenhouseId;
- configuration;
- crop information;
- associated devices;
- current Digital Twin state.

## 4. Device

Represents a physical computing/communication device.

Conceptual attributes:
- deviceId;
- greenhouseId;
- device type;
- connectivity/status;
- device metadata.

A device is not itself a sensor or actuator.

## 5. Sensor

Represents a physical source of environmental observations.

Conceptual attributes:
- sensorId;
- deviceId;
- measurement type;
- unit;
- configuration/status.

A sensor produces observations but does not represent the current greenhouse state by itself.

## 6. Actuator

Represents a physical component capable of changing the environment.

Conceptual attributes:
- actuatorId;
- deviceId;
- actuator type;
- supported capabilities;
- observed state.

Initial actuator:
- type: LIGHT;
- capabilities: TURN_ON, TURN_OFF;
- observed state: ON, OFF, UNKNOWN.

`UNKNOWN` must not be represented as `OFF`.

## 7. Measurement

Represents an accepted physical observation.

Conceptual attributes:

```text
measurementId
messageId
sensorId
measurementType
value
unit
measuredAt
receivedAt
```

### Measurement identity

`measurementId` is the domain/database identity.

`messageId` identifies the incoming telemetry message and supports idempotency/deduplication.

### Time

- `measuredAt`: when the physical observation occurred.
- `receivedAt`: when the backend received the message.

Both are represented using `Instant`/UTC semantics.

### Immutability

Once accepted and persisted, a historical measurement is immutable.

## 8. Twin State

Represents the latest known physical state.

Conceptual attributes:

```text
greenhouseId
sensorId
measurementType
value
unit
measuredAt
receivedAt
updatedAt
```

Twin state is a materialized current representation.

It is not a replacement for historical measurements.

### Out-of-order rule

For the same sensor and measurement type:

```text
incoming.measuredAt > current.measuredAt
```

is required for the incoming observation to replace the current Twin state.

Older measurements remain in history but do not regress current state.

## 9. Freshness

Freshness is derived from observation age.

Initial states:

```text
FRESH
AGING
STALE
UNKNOWN
```

Freshness is not authoritative historical data and should not replace the underlying observation timestamp.

## 10. Desired State

Desired state represents the physical condition the system wants an actuator to reach.

Initial actuator values:

```text
ON
OFF
UNSET
```

Desired state is distinct from:
- command;
- observed physical state.

## 11. Command

Conceptual attributes:

```text
commandId
actuatorId
action
status
createdAt
sentAt
acknowledgedAt
confirmedAt
completedAt
failureReason
```

Initial lifecycle:

```text
REQUESTED
SENT
ACKNOWLEDGED
CONFIRMED
FAILED
TIMEOUT
```

A retry creates a new command identity.

## 12. Simulation

Represents a hypothetical execution against the Digital Twin model.

Simulation data must remain distinguishable from live physical observations.

A simulation must not mutate live physical state or publish physical commands.

## 13. Prediction

Represents an estimated future state.

A prediction is distinct from simulation:

- simulation asks what would happen under a defined hypothetical scenario;
- prediction estimates what is likely to happen.

Predictions should remain traceable to their relevant model/input/context.

## 14. Anomaly

Represents a deviation from expected behavior.

An anomaly is distinct from simple range evaluation.

For example, an environmental value may be inside the configured target range while still exhibiting unusual behavior over time.

## 15. Alert

Represents a condition requiring attention.

An alert can be associated with:
- anomaly;
- environmental condition;
- device/communication condition;
- command failure;
- other relevant system events.

## 16. Database Integrity

Important initial constraints include:

- unique `MEASUREMENT.message_id`;
- required foreign keys;
- NOT NULL constraints where domain-required;
- immutable historical measurement semantics;
- unique command identity;
- valid references between greenhouse, device, sensor, actuator, and measurements.

## 17. Persistence Strategy

PostgreSQL is the authoritative durable store.

A specialized time-series database is intentionally deferred until project evidence demonstrates a need for it.

Indexes should be introduced based on actual query patterns. An initial useful index is:

```text
(sensor_id, measured_at)
```

for historical measurement access.

## 18. Domain/Persistence Separation

The conceptual domain model and database model are related but not identical.

For example:

```text
Domain Measurement
        ↕ mapping
MeasurementEntity
        ↕ persistence
PostgreSQL MEASUREMENT
```

This prevents database structure from becoming the domain model by accident.
