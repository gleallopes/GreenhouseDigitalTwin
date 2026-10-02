# Architecture

## 1. Architectural Style

GreenhouseDigitalTwin uses a **modular monolith** with **business-capability modules** and **Ports and Adapters / Clean Architecture boundaries**.

The initial system is intentionally not decomposed into microservices.

### Rationale

The project initially has:
- one primary application;
- one development team;
- no demonstrated independent deployment/scaling requirements;
- strong need for clear domain boundaries;
- significant physical-system integration complexity.

A modular monolith provides explicit boundaries without introducing unnecessary distributed-system complexity.

## 2. High-Level Structure

```text
                         React / TypeScript
                                │
                              REST
                                │
                                ▼
┌────────────────────────────────────────────────────────────┐
│                    Spring Boot Backend                     │
│                                                            │
│  ┌────────────┐  ┌────────────┐  ┌─────────────────────┐  │
│  │ Greenhouse │  │ Telemetry  │  │ Digital Twin        │  │
│  └────────────┘  └────────────┘  └─────────────────────┘  │
│                                                            │
│  ┌────────────┐  ┌────────────┐  ┌─────────────────────┐  │
│  │ Command    │  │ Simulation │  │ Prediction          │  │
│  └────────────┘  └────────────┘  └─────────────────────┘  │
│                                                            │
│  ┌────────────┐  ┌────────────┐                            │
│  │ Anomaly    │  │ Alerts     │                            │
│  └────────────┘  └────────────┘                            │
│                                                            │
│              Application / Domain Boundaries              │
│                              │                             │
│                 Infrastructure Adapters                   │
└───────────────────────┬───────────────┬───────────────────┘
                        │               │
                   PostgreSQL          MQTT
                                        │
                                        ▼
                              Arduino / Physical System
```

## 3. Business-Capability Modules

Initial modules:

```text
com.greenhousedigitaltwin
├── greenhouse
├── telemetry
├── twin
├── command
├── simulation
├── prediction
└── anomaly
```

Additional capabilities such as alerts may be represented as their own module when the implementation requires it.

Modules are organized by business capability rather than by technical type.

## 4. Internal Module Structure

Each module may contain:

```text
module/
├── domain/
├── application/
└── infrastructure/
```

Example:

```text
telemetry/
├── domain/
├── application/
│   ├── ReceiveTelemetryUseCase
│   ├── ReceiveTelemetryService
│   └── port/
└── infrastructure/
    ├── mqtt/
    └── persistence/
```

## 5. Layer Responsibilities

### Domain

Contains:
- entities;
- value objects;
- domain rules;
- domain concepts;
- state transitions.

The domain must not depend on:
- Spring;
- JPA;
- PostgreSQL;
- MQTT;
- REST;
- React.

### Application

Contains:
- use cases;
- application services;
- transaction boundaries;
- orchestration;
- ports required by use cases.

Example:

```java
public interface MeasurementRepository {
    void save(Measurement measurement);
}
```

The port expresses what the application needs without choosing how it is implemented.

### Infrastructure

Contains adapters for:
- MQTT;
- PostgreSQL/JPA;
- REST;
- external integrations.

Infrastructure depends inward on application/domain contracts.

## 6. Dependency Direction

The intended dependency direction is:

```text
Infrastructure
      ↓
Application
      ↓
Domain
```

The reverse direction is prohibited.

For example:

```text
MQTT adapter
     ↓
ReceiveTelemetryUseCase
     ↓
Telemetry domain
```

Not:

```text
Domain
  ↓
MQTT client
```

## 7. Module Boundary Rules

1. Domain objects are owned by their module.
2. Application use cases are the primary entry points for module behavior.
3. Infrastructure does not bypass application rules.
4. Modules must not directly manipulate another module's persistence.
5. Cross-module interaction uses explicit contracts.
6. Shared code must remain small and stable.
7. A generic `shared` package must not become a dumping ground for unrelated business logic.

## 8. Persistence Boundary

The domain does not use JPA entities directly.

```text
Domain Measurement
       │
       ▼
MeasurementRepository
       │
       ▼
PostgreSQL Adapter
       │
       ▼
MeasurementEntity
       │
       ▼
PostgreSQL
```

This prevents persistence concerns from becoming domain rules.

## 9. REST Boundary

REST controllers are adapters.

```text
HTTP request
     ↓
REST Controller
     ↓
Application Use Case
     ↓
Domain
```

Controllers must not contain core business rules.

REST response DTOs must not expose persistence entities.

## 10. MQTT Boundary

MQTT adapters are responsible for:
- subscribing/publishing;
- transport serialization;
- topic handling;
- transport-level error handling.

Business decisions remain in application/domain layers.

```text
MQTT
 ↓
MQTT Adapter
 ↓
Application Port
 ↓
Domain/Application
```

## 11. Transactions

The accepted telemetry processing path should preserve the invariant:

```text
Accepted Measurement
       +
Current Twin State Update
       =
One atomic persistence operation
```

The measurement and materialized Twin update should succeed or fail together within the PostgreSQL transaction.

MQTT transport itself is not part of the database transaction.

## 12. Runtime Behavior

The backend is a single deployable application initially.

External infrastructure:

```text
┌────────────────────┐
│ Spring Boot        │
│ Modular Monolith   │
└───────┬─────┬──────┘
        │     │
        ▼     ▼
   PostgreSQL MQTT
              │
              ▼
           Arduino
```

## 13. Deliberately Deferred Technologies

The initial architecture does not require:
- Kafka;
- Redis;
- Kubernetes;
- Spring Cloud;
- WebSockets/SSE;
- Elasticsearch;
- specialized time-series databases;
- ML infrastructure.

These can be introduced only when concrete requirements justify them.
