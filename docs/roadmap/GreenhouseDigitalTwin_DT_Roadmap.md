# GreenhouseDigitalTwin — Development Task Roadmap

**Project:** GreenhouseDigitalTwin  
**Last updated:** 2026-11-18  
**Purpose:** A version-controlled record of implemented Development Tasks (DTs), current work, and the remaining project roadmap.

> **Accuracy note:** DT-001–DT-010 reflect milestones discussed during development. The **DT-011+ numbers and titles below are a proposed organization of already discussed project capabilities**, not previously approved tickets. Their exact boundaries, order, and acceptance criteria should be agreed before implementation. Where the historical record lacks a precise original ticket title, the title is descriptive rather than a verbatim Jira title.

## 1. Project vision and scope

Build a **digital twin for a tabletop, apartment-scale mint greenhouse**, linking a physical Arduino prototype with a backend that records telemetry, maintains current state, detects anomalies, supports predictive simulations, and eventually supports **bidirectional communication** between the physical greenhouse and its digital representation.

### Established requirements and decisions

- **Measurements:** temperature, humidity, and luminosity.
- **Mint target ranges:** temperature **20–28 °C**; relative humidity **60–80%**; luminosity **5,000–10,000 lux**. These are *desired operating ranges*, not the same as sensor plausibility limits.
- **Telemetry:** retain historical readings; show the newest measurement by `measuredAt`; do not regress current state when delayed readings arrive.
- **Reliability:** acknowledge identical retries without duplicate insertion; reject conflicting identities; use transactional persistence for measurement insertion and twin-state update.
- **Device communication:** Arduino-based prototype, with eventual two-way communication and command acknowledgment.
- **Simulation:** predictive simulation is a core project capability; it should be evaluated against observed physical behavior.
- **Architecture:** hexagonal/ports-and-adapters, documented requirements and ADRs, automated testing.
- **Backend stack:** Java 25 LTS, Spring Boot 4.1.1, Maven, PostgreSQL 17, Flyway, JPA/JDBC as appropriate, JUnit, Mockito, Testcontainers.
- **Local infrastructure:** PostgreSQL and Eclipse Mosquitto; Podman-compatible development workflow; `application.yml` preferred.

## 2. Milestone status at a glance

| DT | Milestone / descriptive title | Status | Evidence / caveat |
|---|---|---|---|
| DT-001 | Project skeleton and architecture foundations | **Completed** | Initial project setup and architecture established. |
| DT-002 | Local infrastructure: PostgreSQL and Mosquitto | **Completed** | Containerized local services configured. |
| DT-003 | Telemetry transport contract | **Completed / designed** | MQTT topic, payload and QoS contract discussed; this does **not** mean physical MQTT ingestion is implemented. |
| DT-004 | Measurement domain model | **Completed** | Measurement identity, sensor types, units and domain rules implemented/tested. |
| DT-005 | Telemetry validation | **Completed** | Structural, semantic, source and plausibility validation implemented/tested. |
| DT-006 | Measurement persistence and schema evolution | **Completed** | Flyway migrations and persistence tests reported passing. |
| DT-007 | Digital-twin current-state ordering | **Completed** | Newer/older/equal timestamp behavior implemented/tested. |
| DT-008 | Atomic, idempotent persistence and identity conflicts | **Completed** | Duplicate and concurrent handling; rollback covered by integration tests. |
| DT-009 | Telemetry ingestion orchestration | **Completed** | End-to-end scenarios and full `mvn clean verify` reported passing. |
| DT-010 | HTTP telemetry ingestion adapter | **In progress** | Contract and implementation steps discussed; final validation, error mapping and HTTP tests not yet confirmed complete. |

**Status interpretation:** “Completed” reflects the milestones and passing tests reported in our conversations, not a fresh audit of the repository. DT-003's contract/design completion should not be confused with a deployed MQTT device-to-backend path.

## 3. Completed development tasks

### DT-001 — Project skeleton and architecture foundations

**Goal:** Establish the Java/Spring Boot project and a maintainable hexagonal architecture.

**Scope covered:** Maven application skeleton, package/layer responsibilities, ports and adapters, foundational project documentation and architectural decisions.

**Outcome:** Backend foundation ready for domain, application and infrastructure work.

### DT-002 — Local infrastructure

**Goal:** Provide repeatable local dependencies.

**Scope covered:** PostgreSQL 17, Eclipse Mosquitto, containerized services, local networking/configuration and database connectivity.

**Outcome:** Local development infrastructure available for implementation and integration tests.

### DT-003 — Telemetry transport contract

**Goal:** Define how device telemetry is identified and transmitted.

**Scope covered:** MQTT topic/payload/QoS design and telemetry message fields, including device/sensor identity, measurement type, value, unit and measurement time.

**Outcome:** Transport contract designed. **Actual Arduino/MQTT adapter implementation remains future work.**

### DT-004 — Measurement domain

**Goal:** Represent telemetry as validated domain concepts rather than arbitrary request data.

**Scope covered:** `Measurement`, `MeasurementType`, `Unit`, sensor identity and measurement invariants.

**Outcome:** Domain model and tests established.

### DT-005 — Telemetry validation

**Goal:** Reject structurally or semantically unacceptable telemetry and unregistered/mismatched sources.

**Scope covered:** `TelemetryValidator`, `TelemetryMessage`, validation results/reasons, source registry and physical plausibility policy.

**Outcome:** Validation operates separately from duplicate detection. The duplicate check was intentionally moved out of validation into transactional persistence.

### DT-006 — Persistence and schema migrations

**Goal:** Store telemetry in PostgreSQL with reliable schema management.

**Scope covered:** Flyway schema migrations, measurement repository, identity constraints, persistence tests and migration updates.

**Outcome:** Historical measurement storage and database-level constraints tested.

### DT-007 — Digital-twin current-state ordering

**Goal:** Keep the current state aligned with the newest measurement timestamp.

**Established policy:**

- Newer `measuredAt` → persist history **and** update current state.
- Older `measuredAt` → persist history **without** replacing current state.
- Equal `measuredAt`, different message → persist history **without** replacing current state (first-wins policy).

**Outcome:** State-ordering logic and tests completed.

### DT-008 — Transactional idempotency and concurrency

**Goal:** Make ingestion safe under retries, identity collisions, concurrent requests and partial failures.

**Established policy:**

- First valid message → `PERSISTED`.
- Identical committed retry → `ALREADY_PROCESSED`.
- Same identity with different event payload → identity conflict.
- Measurement insert and twin-state update occur in one transaction; an exception during the update must roll back the insertion.
- Never acknowledge a duplicate based on an uncommitted insert that might roll back.

**Outcome:** Persistence and end-to-end tests, including rollback coverage, reported passing.

### DT-009 — Telemetry ingestion orchestration

**Goal:** Connect validation, measurement creation and persistence behind an application use case.

**Flow:** `IngestTelemetryCommand` → `TelemetryValidator` → `TelemetryMeasurementFactory` → `PersistAcceptedTelemetryUseCase` → ingestion outcome.

**Verified scenarios reported:** valid persistence, identical retries, out-of-order delivery, equal timestamps, unknown sensor, unknown device, incompatible units, implausible values, sensor/device mismatch, and existing DT-008 rollback tests.

**Completion gate:** Full `mvn clean verify` **passed**, as reported on 2026-11-18 conversation history.

## 4. Current task — DT-010: HTTP telemetry ingestion adapter

**Goal:** Expose the existing application use case over HTTP without putting business rules into Spring MVC.

**Proposed endpoint:** `POST /api/v1/telemetry`

**Request fields:** `measurementId`, `messageId`, `deviceId`, `sensorId`, `measurementType`, `value`, `unit`, `measuredAt`.

**Response contract agreed:**

| Case | HTTP status | Response / meaning |
|---|---|---|
| New telemetry persisted | `201 Created` | `PERSISTED` |
| Identical retry | `200 OK` | `ALREADY_PROCESSED`; device can stop retrying |
| Malformed JSON / missing required HTTP fields | `400 Bad Request` | Request-contract failure |
| Validly shaped but invalid telemetry | `422 Unprocessable Content` | Domain rejection |
| Conflicting telemetry identity | `409 Conflict` | Same identity, incompatible event |
| Unexpected server error | `500 Internal Server Error` | Internal failure |

**Implementation checklist:**

- [x] Define endpoint and response semantics.
- [x] Design `TelemetryRequest` and `TelemetryResponse` records and controller delegation pattern.
- [ ] Confirm controller/DTO code compiles in the current repository.
- [ ] Apply Jakarta Bean Validation (`@NotBlank`, `@NotNull`, `@Valid`) and verify missing-field `400` responses.
- [ ] Add centralized `@RestControllerAdvice` error mapping and stable JSON error body.
- [ ] Add MockMvc tests for `201`, `200`, `400`, `422`, `409` and malformed JSON.
- [ ] Add HTTP-to-database integration test(s) as appropriate.
- [ ] Run full `mvn clean verify` and review API contract/docs.

**Current conversation checkpoint:** Request validation annotations and a missing-`sensorId` MockMvc test were proposed. Their passing result has **not** yet been reported.

## 5. Proposed future development tasks (DT-011 onward)

> These are **proposed ticket IDs and sequencing** for capabilities already discussed. They are not claims that each task was previously specified or approved. We can split/merge/reorder them during planning.

### DT-011 — HTTP API hardening and documentation

**Goal:** Make the HTTP interface operationally clear and safe to consume.

**Proposed scope:** OpenAPI documentation, consistent error schemas, request/response examples, endpoint versioning decisions, and basic request limits. Keep domain logic in the application layer.

**Acceptance direction:** API contract documented and HTTP tests passing.

### DT-012 — MQTT telemetry ingestion adapter

**Goal:** Implement the MQTT pathway described by the DT-003 contract.

**Proposed scope:** Mosquitto subscription, message decoding, mapping to `IngestTelemetryCommand`, acknowledgment/retry semantics, malformed-message handling and integration tests.

**Acceptance direction:** A published valid MQTT telemetry message reaches the same application use case and produces correct database/twin state; retries remain idempotent.

### DT-013 — Physical Arduino sensor prototype

**Goal:** Connect the tabletop greenhouse to actual sensors.

**Proposed scope:** Temperature, humidity and luminosity sensor selection/wiring, device firmware, measurement timestamps, device/sensor registration and calibration checks.

**Acceptance direction:** The physical device emits realistic, correctly identified measurements for all three sensor types.

### DT-014 — Device connectivity and telemetry health

**Goal:** Distinguish fresh data from disconnected or stale devices.

**Proposed scope:** Last-seen timestamps, stale thresholds, connectivity/health representation and device status transitions. Avoid claiming a device is online merely because it exists in PostgreSQL.

**Acceptance direction:** Dashboard/API can distinguish current, stale and disconnected states.

### DT-015 — Digital-twin query API

**Goal:** Expose current and historical state for other clients.

**Proposed scope:** Read-only endpoints for greenhouses, devices, sensors, latest values and time-range history; pagination and query efficiency.

**Acceptance direction:** Clients can retrieve current readings and historical measurements consistently.

### DT-016 — Greenhouse dashboard

**Goal:** Provide a usable visualization of the digital twin.

**Proposed scope:** Current temperature/humidity/luminosity, last-updated timestamps, stale/disconnected indicators, history charts and anomaly status. Earlier design exercises considered a ~5-second dashboard refresh cadence.

**Acceptance direction:** UI displays correct newest state without confusing delayed historical arrivals with current readings.

### DT-017 — Anomaly detection and alerting

**Goal:** Detect and communicate conditions outside desired mint-growing ranges.

**Proposed scope:** Target-range policies, anomalies, alert lifecycle, hysteresis/debouncing and clear separation between *plausibility rejection* and *valid but undesirable conditions*.

**Acceptance direction:** Valid readings outside the mint comfort ranges can be stored and flagged without being rejected as impossible.

### DT-018 — Predictive simulation engine

**Goal:** Simulate likely greenhouse conditions and hypothetical scenarios without modifying the physical environment.

**Proposed scope:** Initial thermal/humidity/light models, scenario inputs, forecast horizon, model assumptions and prediction-vs-observation comparison.

**Acceptance direction:** Simulation output is reproducible, documented and compared against real measurements.

### DT-019 — Command domain and lifecycle

**Goal:** Define safe bidirectional control before adding actuators.

**Proposed scope:** Command identity, desired action, `PENDING`/acknowledged/applied/failed/timeout states, idempotency, retry rules, expiration and audit history.

**Acceptance direction:** Backend distinguishes *command accepted* from *physical action confirmed*.

### DT-020 — Device command transport and acknowledgment

**Goal:** Deliver commands to the Arduino and receive application-level confirmation.

**Proposed scope:** MQTT command topics/payloads, command correlation, timeout/retry behavior, acknowledgment messages and reconnection handling.

**Acceptance direction:** A command is only marked applied after the device confirms the physical operation.

### DT-021 — Actuator integration and safety

**Goal:** Connect physical controls to the digital twin.

**Proposed scope:** Choose appropriate greenhouse actuators (e.g., ventilation or lighting), safe operating limits, fail-safe behavior, manual override and hardware tests. Actual actuator selection remains open.

**Acceptance direction:** Commands produce safe, observable physical actions and accurately reflected twin state.

### DT-022 — End-to-end validation and release readiness

**Goal:** Verify the full digital/physical loop and prepare a demonstrable release.

**Proposed scope:** Device-to-backend-to-dashboard tests, command round trips, outage/recovery tests, simulation accuracy evaluation, security review, documentation, deployment packaging and demo walkthrough.

**Acceptance direction:** Repeatable demonstration of telemetry, historical state, current state, simulation, alerts and confirmed bidirectional action.

## 6. Cross-cutting work (not assigned DT numbers yet)

These topics have been discussed or are reasonable release concerns, but **no prior approved ticket numbers are established**:

- Security: authentication and authorization for device ingestion and command endpoints; secure credentials; transport security; input limits; auditability; threat modeling and AppSec testing.
- Reliability: retry backoff, message delivery guarantees, observability, logs/metrics, integration-test isolation and failure recovery.
- Data: retention, indexing, migration compatibility, time handling and performance testing.
- Documentation: ADRs, architecture diagrams, API contracts, test strategy, hardware wiring and operational instructions.
- Delivery: CI pipeline, automated checks, release/version strategy and deployment decisions.

## 7. Recommended implementation order

1. **Finish DT-010** — HTTP validation, exception handling, MockMvc and regression tests.
2. **Confirm the DT-003 MQTT contract** and implement DT-012; document HTTP/MQTT differences.
3. **Build DT-013** to produce real physical sensor data.
4. **Implement DT-014–DT-017** for connectivity, query APIs, dashboard and alerts.
5. **Develop DT-018** simulations against collected real measurements.
6. **Implement DT-019–DT-021** for acknowledged bidirectional commands and safe actuation.
7. **Complete DT-022** with whole-system verification and release documentation.

DT-011 can be completed immediately after DT-010 or alongside early transport work. Security, CI and observability should be addressed incrementally, not postponed until release.

## 8. Milestone definition of done

A DT is complete when its agreed acceptance criteria are met, tests pass at the appropriate layer, any affected API/schema/architecture documentation is updated, and the complete regression suite passes. For hardware work, physical demonstration and failure-mode checks supplement software tests.

## 9. Next immediate action

Resume **DT-010** at request validation: confirm the `TelemetryRequest` Jakarta annotations, `@Valid` controller parameter and missing-`sensorId` MockMvc test, then implement `@RestControllerAdvice` and verify the HTTP status contract.

---

*Roadmap compiled from the GreenhouseDigitalTwin development discussions. Future DT-011–DT-022 ticket numbering is a planning proposal, not a recovered Jira backlog.*
