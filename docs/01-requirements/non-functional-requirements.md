# Non-Functional Requirements

## NFR-001 — Graceful Device Disconnection

The system shall handle loss of physical device connectivity without corrupting the Digital Twin.

**Acceptance criteria**
- Device connectivity can be represented as disconnected/unavailable.
- Last known values may remain visible.
- Their freshness is evaluated independently.
- Disconnection does not fabricate new measurements.

## NFR-002 — Communication Failure Handling

The system shall explicitly handle communication failures between the backend, broker, and physical device.

**Acceptance criteria**
- Communication failures are observable.
- Failures do not silently become successful physical state.
- Recoverable failures can be retried/re-delivered according to the communication design.

## NFR-003 — No Silent Data Loss

The system shall not silently discard accepted telemetry or important command state.

**Acceptance criteria**
- Accepted measurements are durably persisted.
- Persistence failures prevent the system from falsely treating data as accepted.
- Important failures are observable through logs/metrics or explicit state.

## NFR-004 — Telemetry Processing Latency

Target: at least 95% of valid telemetry messages should be processed within 2 seconds under the initial expected workload.

This target is an initial engineering target and shall be validated experimentally.

## NFR-005 — Command Processing Latency

Target: at least 95% of commands should reach the defined command-publication/processing milestone within 2 seconds under the initial expected workload.

This target is an initial engineering target and shall be validated experimentally. Physical confirmation has its own timing constraints.

## NFR-006 — Timestamp Integrity

The system shall preserve reliable temporal information for measurements, commands, acknowledgements, and physical-state feedback.

**Acceptance criteria**
- Physical observation time is preserved.
- Backend reception time is recorded separately.
- Timestamps use a consistent representation based on `Instant`/UTC semantics.

## NFR-007 — Measurement Traceability

Every accepted measurement shall be traceable to its source and time.

**Acceptance criteria**
- Measurement can be traced to sensor and device.
- Message identity is preserved.
- Measurement and reception times are available.

## NFR-008 — Historical Data Immutability

Accepted historical measurements shall be immutable.

**Acceptance criteria**
- Existing measurements are not silently rewritten as new observations.
- Corrections, if ever required, must use an explicit mechanism rather than mutating historical truth.

## NFR-009 — Simulation Isolation

Simulation execution shall be isolated from live physical control.

**Acceptance criteria**
- No simulation operation can publish a physical actuator command.
- Simulation cannot overwrite live observed state.
- Simulation results are stored separately from physical observations.

## NFR-010 — Explicit Physical Commands

Physical actuator commands shall be explicit application operations.

**Acceptance criteria**
- Physical commands have unique identity.
- Commands are traceable to their target actuator.
- No implicit physical command is generated merely by running a simulation or prediction.

## NFR-011 — Physical State Takes Precedence

When representing actual physical state, confirmed physical feedback shall take precedence over assumptions based on requested commands.

**Acceptance criteria**
- Desired state is distinct from observed state.
- A command is not treated as proof of actuation.
- Unknown physical state is represented as unknown rather than falsely mapped to OFF.

## NFR-012 — Device Authentication

Physical devices shall be authenticated before their telemetry or control interactions are trusted.

**Acceptance criteria**
- The architecture defines a device identity/trust mechanism.
- Unknown devices cannot be silently accepted as trusted sources.

## NFR-013 — Command Authorization

Only authorized application operations shall be able to issue physical commands.

**Acceptance criteria**
- Command issuance has an authorization boundary.
- Unauthorized command attempts are rejected.
- Authorization is independent from device acknowledgement.

## NFR-014 — Secure Communication

Communication between system components shall use appropriate security mechanisms for the deployment environment.

**Acceptance criteria**
- The architecture defines secure transport for production-relevant communication.
- Credentials/secrets are not embedded in source code or telemetry payloads.
- Device communication is protected against unauthorized access according to the deployment threat model.

## NFR-015 — Device Connectivity Visibility

The system shall expose enough information to determine whether physical devices are connected/available.

**Acceptance criteria**
- Device status can be observed.
- Connectivity status is not confused with sensor measurement freshness.

## NFR-016 — Telemetry Metrics

The system shall provide metrics sufficient to understand telemetry processing.

At minimum, metrics should support visibility into:
- received telemetry;
- accepted telemetry;
- rejected telemetry;
- duplicate telemetry;
- processing latency;
- persistence failures.

## NFR-017 — Command Metrics

The system shall provide metrics sufficient to understand command processing.

At minimum, metrics should support visibility into:
- commands requested;
- commands published;
- acknowledgements;
- confirmations;
- failures;
- timeouts;
- command latency.

## NFR-018 — Structured Logging

Operationally relevant events shall be logged in a structured and traceable form.

**Acceptance criteria**
- Logs contain useful identifiers such as messageId, commandId, deviceId, actuatorId, or greenhouseId when applicable.
- Logs avoid exposing secrets.
- Failure reasons are distinguishable.

## NFR-019 — Separation of Concerns

The system shall preserve clear boundaries between domain logic, application use cases, and infrastructure adapters.

**Acceptance criteria**
- Domain logic does not depend on Spring, JPA, MQTT, PostgreSQL, or REST.
- Infrastructure adapters do not contain core business rules.
- REST and MQTT adapters translate external representations into application/domain operations.

## NFR-020 — Testability

The system shall be designed so core business rules can be tested independently of physical hardware and external infrastructure.

**Acceptance criteria**
- Domain/application logic can be unit tested.
- Hardware-independent integration tests can run in CI.
- Physical hardware tests are separate from ordinary CI execution.

## NFR-021 — Command Traceability

Every physical command shall be traceable throughout its lifecycle.

**Acceptance criteria**
- Command identity is unique.
- Lifecycle transitions are attributable to the command.
- A command can be correlated with acknowledgements and physical-state feedback.

## NFR-022 — Telemetry Traceability

Every accepted telemetry message shall be traceable through validation, persistence, and Twin-state update.

**Acceptance criteria**
- Message identity can be correlated across processing stages.
- Rejection reasons are identifiable for rejected messages.
- Accepted measurements can be traced to their source and persistence record.

## NFR-023 — Failure Explainability

Failures and unexpected states shall be represented in a way that allows a developer or operator to understand what happened.

**Acceptance criteria**
- Known failure categories are explicit.
- Command failures distinguish failure from timeout where applicable.
- Stale/unknown state is distinguishable from a valid measured value.
- Validation failures identify their rejection reason.
