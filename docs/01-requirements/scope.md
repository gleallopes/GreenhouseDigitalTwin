# V1 Scope

## In Scope

### Greenhouse and device management

- Register a greenhouse.
- Register physical devices.
- Register sensors.
- Associate devices and sensors with a greenhouse.

### Telemetry

- Receive temperature, humidity, and luminosity telemetry.
- Validate telemetry structurally and semantically.
- Validate telemetry source identity and relationships.
- Detect duplicate messages.
- Validate physical plausibility.
- Persist accepted historical measurements.
- Preserve measurement and reception timestamps.
- Update current Digital Twin state from accepted observations.

### Digital Twin state

- Maintain current observed state.
- Track freshness of observations.
- Represent unknown/stale/disconnected conditions explicitly.
- Evaluate environmental suitability using configured ranges.
- Expose current state through the API.
- Display current state through the frontend.

### Bidirectional communication

- Issue commands to physical actuators.
- Track command lifecycle.
- Publish commands through MQTT.
- Receive device acknowledgements.
- Receive physical-state feedback.
- Confirm commands only when physical feedback demonstrates the expected state.
- Handle command failure and timeout.

### Simulation and prediction

- Create and execute hypothetical simulation scenarios.
- Preserve simulation results.
- Prevent simulations from producing physical commands.
- Generate initial explainable future-state predictions.
- Compare predictions against observed state.
- Record model error.

### Anomaly and alerts

- Detect environmental anomalies.
- Generate alerts for relevant abnormal conditions.

### Testing and operations

- Unit testing.
- Integration testing.
- End-to-end testing with representative/simulated devices.
- Separate physical hardware validation.
- Structured logging.
- Telemetry and command metrics.
- Device connectivity visibility.

## Out of Scope for Initial V1

The following are intentionally not required initially:

- microservice decomposition;
- Kubernetes;
- Kafka;
- Redis;
- specialized time-series databases;
- machine-learning-based prediction;
- automatic optimization;
- autonomous closed-loop environmental control;
- complex distributed orchestration;
- WebSockets/SSE unless later justified by requirements or measurements;
- a 3D visualization of the greenhouse.

These may be reconsidered when evidence from the project justifies them.

## Boundary Rules

### Simulation boundary

Simulation is hypothetical. It must not publish physical commands or mutate physical state.

### Physical command boundary

Commands are explicit requests to the physical system. The application must distinguish:

`Command != Desired State != Observed State`

### Historical data boundary

Accepted historical measurements are immutable. Current Digital Twin state is a materialized representation of the latest valid observation and is not a replacement for history.

### Communication boundary

MQTT is a transport mechanism. Successful message delivery is not equivalent to successful physical actuation.

## Assumptions

- The first physical device is Arduino-based.
- MQTT is the communication mechanism between backend and physical device.
- PostgreSQL is the primary durable persistence store.
- The backend is a Spring Boot application.
- The frontend is React with TypeScript.
- The initial system is a modular monolith.
- The initial actuator is a simple light/LED.
- Initial prediction should be simple and explainable rather than ML-based.

## Requirement Evolution

Requirements may evolve as the physical prototype and measurements reveal new constraints. Changes that affect architectural boundaries, consistency guarantees, security, communication semantics, or other durable technical decisions should be reflected in an ADR.
