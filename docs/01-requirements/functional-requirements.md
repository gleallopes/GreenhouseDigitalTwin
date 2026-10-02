# Functional Requirements

## FR-001 — Register Greenhouse

The system shall allow a greenhouse to be registered with a unique identity and its relevant configuration.

**Acceptance criteria**
- A greenhouse has a unique identifier.
- A registered greenhouse can be referenced by associated devices and sensors.
- Duplicate greenhouse identities are rejected.

## FR-002 — Register Physical Device

The system shall allow a physical device, such as an Arduino, to be registered and associated with a greenhouse.

**Acceptance criteria**
- A device has a unique identity.
- A device belongs to a greenhouse.
- A device can have a connectivity/status representation.

## FR-003 — Register Sensor

The system shall allow sensors to be registered and associated with a physical device.

**Acceptance criteria**
- A sensor has a unique identity.
- A sensor belongs to a device.
- A sensor has a supported measurement type and unit.

## FR-004 — Receive Telemetry

The system shall receive telemetry containing the identity of the message, device, sensor, measurement type, value, unit, and physical measurement timestamp.

**Acceptance criteria**
- Valid telemetry can enter the application processing pipeline.
- Telemetry preserves the physical observation timestamp.
- The backend records its reception timestamp.

## FR-005 — Validate Telemetry

The system shall validate incoming telemetry before it can become an accepted domain measurement.

Validation shall include:
- structural validation;
- semantic validation;
- source validation;
- duplicate-message detection;
- physical plausibility validation.

**Acceptance criteria**
- Invalid telemetry is rejected.
- Rejected telemetry does not update the Digital Twin.
- A rejection reason can be identified.

## FR-006 — Persist Telemetry

The system shall persist every accepted measurement as immutable historical data.

**Acceptance criteria**
- Accepted measurements are stored in PostgreSQL.
- Historical measurements are not silently overwritten.
- Message identity is unique.
- Measurement time and reception time are preserved.

## FR-007 — Maintain Current Twin State

The system shall maintain the current known physical state of the greenhouse based on accepted observations.

**Acceptance criteria**
- A valid new observation can update the corresponding Twin state.
- Older out-of-order measurements do not regress the current state.
- Duplicate messages do not create duplicate state changes.

## FR-008 — Synchronize Physical and Digital State

The system shall synchronize the Digital Twin with valid observations from the physical system.

**Acceptance criteria**
- Only accepted physical observations can update observed state.
- The Digital Twin represents the latest valid known physical state.
- Physical feedback takes precedence over assumed state.

## FR-009 — Track State Freshness

The system shall represent whether current observations are fresh, aging, stale, or unknown.

**Acceptance criteria**
- Freshness is derived from observation age.
- Missing observations do not appear as valid zero values.
- Stale state remains visible but is explicitly marked stale.

## FR-010 — Configure Environmental Ranges

The system shall support environmental target ranges for required environmental variables.

Initial ranges:
- temperature: 20–28 °C;
- relative humidity: 60–80 %;
- luminosity: 5,000–10,000 lux.

**Acceptance criteria**
- A configured range has a measurement type and compatible unit.
- The range can be used by environmental evaluation.

## FR-011 — Evaluate Environmental Conditions

The system shall evaluate individual environmental observations against configured target ranges.

**Acceptance criteria**
- An observation inside its configured range is suitable.
- An observation outside its configured range is not suitable.
- Missing, invalid, or stale required observations do not get treated as suitable observations.

## FR-012 — Determine Overall Environmental Status

The system shall determine an overall greenhouse environmental status from the required observations.

Initial rule:
- missing/unknown/stale required observation → `UNKNOWN`;
- any required observation outside its range → `NOT_SUITABLE`;
- all required observations valid and inside their ranges → `SUITABLE`.

## FR-013 — Detect Environmental Anomaly

The system shall detect environmental conditions that deviate from expected behavior.

**Acceptance criteria**
- Anomaly detection is distinct from ordinary range evaluation.
- An anomaly can be associated with the relevant observation/context.
- An anomaly does not automatically imply that a physical command should be issued.

## FR-014 — Generate Alert

The system shall generate an alert when a relevant anomaly or important environmental condition requires attention.

**Acceptance criteria**
- Alerts identify the relevant greenhouse/context.
- Alerts provide enough information to understand why they were generated.
- Alert generation does not silently alter physical state.

## FR-015 — Create Simulation Scenario

The system shall allow a hypothetical scenario to be defined using the Digital Twin model.

**Acceptance criteria**
- A scenario is distinguishable from live physical state.
- Simulation input does not mutate the physical greenhouse.
- A simulation has an identifiable scenario/context.

## FR-016 — Execute Simulation

The system shall execute a simulation against the Digital Twin model.

**Acceptance criteria**
- Simulation produces a result representing the hypothetical scenario.
- Simulation does not publish physical commands.
- Simulation does not overwrite live observed state.

## FR-017 — Preserve Simulation Results

The system shall preserve simulation results so they can be inspected and compared later.

**Acceptance criteria**
- A result can be associated with its scenario.
- Simulation results are distinguishable from physical observations.
- Preserving a simulation result does not alter physical state.

## FR-018 — Generate Future-State Prediction

The system shall generate an initial explainable prediction of future greenhouse state.

**Acceptance criteria**
- Prediction is distinguishable from simulation.
- Prediction identifies the relevant time/context.
- The initial prediction approach is explainable and testable.
- Prediction does not directly control physical devices.

## FR-019 — Issue Physical Command

The system shall allow an authorized application operation to issue an explicit command to a physical actuator.

**Acceptance criteria**
- A command has a unique identity.
- A command targets a specific actuator.
- A command specifies an action.
- Simulation cannot issue physical commands.

## FR-020 — Track Command Status

The system shall track the lifecycle of physical commands.

Initial lifecycle:
`REQUESTED → SENT → ACKNOWLEDGED → CONFIRMED`

Failure paths:
`REQUESTED/SENT/ACKNOWLEDGED → FAILED or TIMEOUT`, according to the applicable failure condition.

**Acceptance criteria**
- Invalid lifecycle transitions are rejected.
- Terminal commands are not resurrected by late messages.
- Retries create a new command identity.

## FR-021 — Confirm Physical State

The system shall confirm a physical command only when physical-state feedback demonstrates the expected resulting state.

Initial mapping:
- `TURN_ON → ON`
- `TURN_OFF → OFF`

**Acceptance criteria**
- MQTT publication success does not produce `CONFIRMED`.
- Device acknowledgement does not by itself produce `CONFIRMED`.
- Expected physical-state feedback is required.
- Physical state remains authoritative.

## FR-022 — Compare Predicted and Observed State

The system shall compare predicted state with later observed physical state.

**Acceptance criteria**
- A prediction can be associated with the observations used for validation.
- Comparison identifies the relevant predicted and observed values.
- Comparison does not modify historical observations.

## FR-023 — Record Model Error

The system shall record the difference between predicted and observed state for model evaluation.

**Acceptance criteria**
- Model error is traceable to the prediction and observation.
- Historical observations remain immutable.
- Model error can be used to evaluate and improve the prediction approach.
