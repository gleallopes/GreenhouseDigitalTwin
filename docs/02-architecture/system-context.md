# System Context

## 1. Purpose

GreenhouseDigitalTwin connects a physical mini greenhouse to a software Digital Twin.

The system observes the physical environment, maintains a digital representation of its state, analyzes that state, supports simulation and prediction, and can issue explicit commands to physical actuators.

## 2. System Boundary

The GreenhouseDigitalTwin system includes:

- backend application;
- Digital Twin domain;
- telemetry processing;
- command processing;
- simulation;
- prediction;
- anomaly and alert processing;
- PostgreSQL persistence;
- MQTT integration;
- React frontend.

The physical greenhouse and its Arduino/device firmware are external physical systems from the backend's architectural perspective.

## 3. Context Diagram

```text
                         ┌──────────────────────┐
                         │      User            │
                         │  greenhouse operator │
                         └──────────┬───────────┘
                                    │
                                    │ HTTPS / REST
                                    ▼
                         ┌──────────────────────┐
                         │   React Frontend     │
                         └──────────┬───────────┘
                                    │
                                    ▼
                 ┌─────────────────────────────────────┐
                 │       GreenhouseDigitalTwin         │
                 │                                     │
                 │  Twin / Telemetry / Command /       │
                 │  Simulation / Prediction / Alerts   │
                 └───────┬───────────────────┬─────────┘
                         │                   │
                    PostgreSQL             MQTT
                         │                   │
                         ▼                   ▼
                 ┌──────────────┐   ┌──────────────────┐
                 │ Historical   │   │ MQTT Broker      │
                 │ data/state   │   └────────┬─────────┘
                 └──────────────┘            │
                                             │
                                             ▼
                                  ┌─────────────────────┐
                                  │ Physical Device     │
                                  │ Arduino + firmware  │
                                  ├─────────────────────┤
                                  │ Sensors             │
                                  │ Actuators           │
                                  └──────────┬──────────┘
                                             │
                                             ▼
                                      Physical greenhouse
                                           / mint
```

## 4. External Entities

### User

Uses the React frontend to:
- inspect current greenhouse state;
- inspect freshness/connectivity;
- inspect environmental status;
- inspect alerts;
- run simulations;
- inspect predictions;
- issue authorized physical commands.

### Physical Device

Initial implementation is Arduino-based.

Responsibilities:
- read sensors;
- publish telemetry;
- receive commands;
- acknowledge commands;
- report actuator state;
- continue operating independently of backend availability where possible.

### MQTT Broker

Provides message transport between backend and physical device.

The broker is transport infrastructure, not the source of truth for physical state.

### PostgreSQL

Provides durable storage for:
- greenhouse/device/sensor configuration;
- historical measurements;
- current Twin state;
- command history;
- desired state;
- simulation/prediction/anomaly/alert data as those capabilities are implemented.

## 5. Main Data Flows

### Telemetry flow

```text
Physical Sensor
      ↓
Arduino
      ↓
MQTT
      ↓
Telemetry Adapter
      ↓
Validation
      ↓
Measurement
      ↓
PostgreSQL
      ↓
Twin State
      ↓
REST API
      ↓
React
```

### Command flow

```text
User/Application
      ↓
Command Use Case
      ↓
Command Persistence
      ↓
MQTT Publisher
      ↓
Arduino
      ↓
ACK
      ↓
Command Lifecycle
      ↓
Physical State Feedback
      ↓
Observed State
      ↓
Command Confirmation
```

## 6. Important Trust Boundaries

### Device → Backend

Telemetry is untrusted input until validated.

### MQTT → Application

MQTT messages are transport representations and must not directly become domain objects without validation.

### Frontend → Backend

Requests are untrusted external input and must pass application-level validation and authorization.

### Simulation → Physical Control

There must be no architectural path from simulation execution to physical command publication.

## 7. Core Architectural Invariant

The system must preserve:

```text
Requested action
      ≠
Desired physical state
      ≠
Observed physical state
```

This distinction is fundamental to the Digital Twin's correctness.
