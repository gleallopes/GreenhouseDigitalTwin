# Telemetry Validation Specification

**Project:** GreenhouseDigitalTwin  
**Document:** Telemetry Validation Specification  
**Related Ticket:** DT-005  
**Status:** Accepted  
**Version:** 1.0

---

## 1. Purpose

This document defines the validation rules applied to telemetry messages received from physical greenhouse devices before they are converted into valid domain `Measurement` objects.

The validation process protects the Digital Twin from malformed, semantically invalid, unauthorized, duplicated, or physically implausible telemetry.

Only telemetry that passes all applicable validation stages may produce a valid domain `Measurement` and subsequently update the Digital Twin.

---

## 2. Scope

This specification covers validation of incoming telemetry at the application boundary.

The following validation responsibilities are included:

1. Structural validation
2. Semantic validation
3. Source validation
4. Duplicate message detection
5. Physical plausibility validation

This specification does **not** define:

- Environmental suitability for the plant
- Digital Twin state update rules
- Historical persistence implementation
- PostgreSQL implementation details
- MQTT transport configuration
- Sensor-specific physical limits that have not yet been established
- Anomaly detection

These responsibilities belong to other parts of the system.

---

## 3. Telemetry Contract

The initial telemetry message contains:

```text
messageId
deviceId
sensorId
measurementType
value
unit
measuredAt
```

Example:

```json
{
  "messageId": "MSG-001",
  "deviceId": "ARD-001",
  "sensorId": "TEMP-001",
  "measurementType": "TEMPERATURE",
  "value": 25.4,
  "unit": "CELSIUS",
  "measuredAt": "2026-09-30T15:00:00Z"
}
```

The backend generates `receivedAt` when the message is accepted into the application processing pipeline.

`measuredAt` represents when the physical observation occurred.

`receivedAt` represents when the backend received the observation.

---

## 4. Validation Pipeline

Telemetry validation follows this sequence:

```text
MQTT Message
     |
     v
Structural Validation
     |
     v
Semantic Validation
     |
     v
Source Validation
     |
     v
Duplicate Detection
     |
     v
Physical Plausibility
     |
     v
Accepted Measurement
```

A validation failure terminates processing for that message.

Rejected telemetry must not create a valid `Measurement` and must not update the Digital Twin.

---

# 5. Structural Validation

Structural validation determines whether the telemetry message contains the information required to process it.

### 5.1 Required fields

The following fields are required:

- `messageId`
- `deviceId`
- `sensorId`
- `measurementType`
- `value`
- `unit`
- `measuredAt`

### 5.2 Rules

A telemetry message is structurally invalid when:

- the message itself is null;
- a required identifier is missing;
- a required textual field is blank;
- `value` is missing;
- `value` is not finite;
- `measuredAt` is missing.

Zero is a valid measurement value and must not be interpreted as missing data.

For example:

```text
value = 0
```

is different from:

```text
value = missing
```

### 5.3 Rejection reason

```text
STRUCTURAL_ERROR
```

---

# 6. Semantic Validation

Semantic validation determines whether the fields of the telemetry message are meaningful together.

### 6.1 Measurement types

The initial supported measurement types are:

```text
TEMPERATURE
HUMIDITY
LUMINOSITY
```

### 6.2 Units

The initial supported units are:

```text
CELSIUS
PERCENT
LUX
```

### 6.3 Type/unit compatibility

The following combinations are initially valid:

| Measurement Type | Unit |
|---|---|
| TEMPERATURE | CELSIUS |
| HUMIDITY | PERCENT |
| LUMINOSITY | LUX |

Examples of invalid combinations:

```text
TEMPERATURE + LUX
HUMIDITY + CELSIUS
LUMINOSITY + PERCENT
```

An unknown measurement type or unit is also semantically invalid.

### 6.4 Rejection reasons

Unknown or invalid semantic information may produce:

```text
SEMANTIC_ERROR
```

or, when the type and unit are individually recognized but incompatible:

```text
INCOMPATIBLE_MEASUREMENT_TYPE
```

---

# 7. Source Validation

Source validation determines whether the telemetry originates from a legitimate and correctly associated physical source.

The validator must establish:

1. The device is known.
2. The sensor is known.
3. The sensor belongs to the reported device.
4. The sensor supports the reported measurement type.

Conceptually:

```text
Greenhouse
    |
    +-- Device ARD-001
          |
          +-- Sensor TEMP-001
                |
                +-- TEMPERATURE
```

A telemetry message claiming:

```text
deviceId = ARD-001
sensorId = TEMP-001
measurementType = TEMPERATURE
```

is valid only when the registered source configuration supports that relationship.

### 7.1 Application boundary

Source validation is represented through an application port:

```text
MeasurementSourceRegistry
```

The validator does not directly access PostgreSQL or another persistence technology.

This preserves the dependency direction:

```text
Telemetry Validator
        |
        v
MeasurementSourceRegistry
        ^
        |
Infrastructure Adapter
```

### 7.2 Rejection reasons

Possible source validation failures are:

```text
UNKNOWN_DEVICE
UNKNOWN_SENSOR
SENSOR_DEVICE_MISMATCH
INCOMPATIBLE_MEASUREMENT_TYPE
```

---

# 8. Duplicate Message Detection

The telemetry protocol uses MQTT QoS 1.

QoS 1 provides at-least-once delivery semantics, meaning the same telemetry message may be delivered more than once.

Therefore every telemetry message has a unique:

```text
messageId
```

The `messageId` represents the identity of the telemetry message.

Example:

```text
MSG-001
```

If the same message is delivered twice:

```text
MSG-001 → first delivery
MSG-001 → second delivery
```

the second delivery must not create another historical measurement.

### 8.1 Application boundary

Duplicate detection is represented through:

```text
TelemetryMessageRegistry
```

The application asks whether a `messageId` has already been processed.

### 8.2 Rejection reason

```text
DUPLICATE_MESSAGE
```

### 8.3 Database integrity

Application-level duplicate detection is not sufficient by itself to guarantee uniqueness under concurrent processing.

The persistence layer must therefore enforce uniqueness for:

```text
MEASUREMENT.message_id
```

using a database uniqueness constraint.

The application check and database constraint provide complementary protection.

---

# 9. Physical Plausibility Validation

Physical plausibility determines whether a measurement could reasonably represent a physical observation.

This validation is deliberately distinct from environmental suitability.

For example:

```text
Temperature = 35 °C
```

may be unsuitable for the greenhouse's configured environmental range while still being a legitimate physical measurement.

Therefore:

```text
Telemetry Validity
        !=
Environmental Suitability
```

A value such as:

```text
Temperature = -999 °C
```

may instead be considered physically implausible and rejected.

### 9.1 Application boundary

Physical plausibility is represented through:

```text
MeasurementPlausibilityValidator
```

The validator receives the measurement type and value and determines whether the observation is physically plausible.

### 9.2 Sensor-specific limits

Exact plausibility limits must not be invented before the actual sensor hardware and its documented operating characteristics are established.

The implementation must therefore keep these limits configurable or replaceable.

### 9.3 Rejection reason

```text
PHYSICAL_VALUE_OUT_OF_RANGE
```

---

# 10. Validation vs Environmental Evaluation

Telemetry validation answers:

> "Can this observation be trusted as a valid physical measurement?"

Environmental evaluation answers:

> "Is the current greenhouse environment suitable according to the configured requirements?"

These are different concerns.

For example:

```text
Temperature = 35 °C
```

may result in:

```text
Telemetry validation:
VALID

Environmental evaluation:
NOT_SUITABLE
```

The measurement should therefore be preserved rather than discarded.

This distinction is important for historical analysis, anomaly detection, simulation, and prediction.

---

# 11. Validation Result

Validation produces a result representing either acceptance or rejection.

Conceptually:

```text
TelemetryValidationResult
    |
    +-- ACCEPTED
    |
    +-- REJECTED
          |
          +-- STRUCTURAL_ERROR
          +-- SEMANTIC_ERROR
          +-- UNKNOWN_DEVICE
          +-- UNKNOWN_SENSOR
          +-- SENSOR_DEVICE_MISMATCH
          +-- INCOMPATIBLE_MEASUREMENT_TYPE
          +-- DUPLICATE_MESSAGE
          +-- PHYSICAL_VALUE_OUT_OF_RANGE
          +-- INVALID_TIMESTAMP
```

The validation result should contain enough information for the application layer to determine the next processing step and for observability mechanisms to explain rejection.

---

# 12. Validation Ordering

Validation is intentionally ordered so that inexpensive and fundamental checks occur before checks requiring external information.

The current order is:

```text
1. Structural
2. Semantic
3. Source
4. Duplicate
5. Physical plausibility
```

A failed earlier validation prevents unnecessary subsequent processing.

For example:

```text
Malformed message
      |
      v
STRUCTURAL_ERROR
      |
      X
No source lookup
No duplicate lookup
No plausibility evaluation
```

Similarly:

```text
Unknown device
      |
      v
UNKNOWN_DEVICE
      |
      X
No duplicate processing
No plausibility evaluation
```

This keeps the validation pipeline deterministic and avoids unnecessary dependencies.

---

# 13. Domain Boundary

The validation layer must not couple the domain model to transport or persistence representations.

The following concepts remain separate:

```text
MQTT Payload
     !=
TelemetryMessage
     !=
Domain Measurement
     !=
Database Record
     !=
REST Response
```

The purpose of these boundaries is to prevent changes in one technical layer from unnecessarily changing the domain model.

---

# 14. Architectural Constraints

The following constraints apply to DT-005:

### Domain independence

The domain must not depend on:

- Spring
- MQTT
- JPA
- PostgreSQL
- REST

### Application responsibilities

The application layer may coordinate:

- validation
- source registries
- duplicate detection
- domain creation

### Infrastructure responsibilities

Infrastructure provides implementations for application ports, including future persistence and transport adapters.

### Validation isolation

The validator must not:

- publish MQTT messages;
- directly query PostgreSQL;
- update the Digital Twin;
- perform environmental suitability evaluation;
- generate physical commands.

---

# 15. Security and Trust Boundary

Telemetry received from a physical device must not automatically be considered trustworthy.

The application validates:

```text
message structure
      +
message semantics
      +
registered source
      +
message identity
      +
physical plausibility
```

Only after these checks may telemetry become an accepted domain observation.

Device authentication and secure MQTT communication are separate security concerns covered by the system security architecture.

---

# 16. Important Invariants

The following invariants must remain true:

### INV-001 — Invalid telemetry does not update the Twin

```text
Rejected telemetry
        X
        |
        X
Digital Twin
```

### INV-002 — Duplicate messages do not create duplicate measurements

For a given `messageId`, at most one historical measurement may be persisted.

### INV-003 — Zero is a valid measurement

Zero must never be used as a missing-data sentinel.

### INV-004 — Environmental unsuitability does not imply invalid telemetry

A valid physical observation may be outside the desired environmental range.

### INV-005 — Historical measurements remain evidence

Rejected telemetry must not overwrite previously accepted measurements.

### INV-006 — Transport is not the domain

MQTT payload structures must not become domain entities directly.

---

# 17. Testing Requirements

DT-005 must include tests for:

### Structural validation

- null message
- missing message ID
- missing device ID
- missing sensor ID
- missing measurement type
- missing value
- missing unit
- missing timestamp
- non-finite numeric value

### Semantic validation

- valid temperature
- valid humidity
- valid luminosity
- unknown measurement type
- unknown unit
- incompatible type/unit combination

### Source validation

- valid device/sensor relationship
- unknown device
- unknown sensor
- sensor/device mismatch
- unsupported measurement type

### Duplicate detection

- new message accepted
- duplicate message rejected
- duplicate detection uses `messageId`
- invalid earlier validation prevents duplicate lookup when appropriate

### Physical plausibility

- plausible value accepted
- implausible value rejected
- unusual but physically possible value remains valid telemetry

### Pipeline behavior

- earlier validation failures stop subsequent processing
- accepted telemetry proceeds to Measurement creation

---

# 18. Current Implementation Status

DT-005 currently establishes the validation architecture and validation rules.

Implemented responsibilities:

```text
[x] Structural validation
[x] Semantic validation
[x] Source validation
[x] Duplicate detection
[x] Physical plausibility boundary
[x] Unit/type compatibility
[x] Validation result model
[x] Application ports
[x] Automated unit tests
```

Intentionally deferred:

```text
[ ] PostgreSQL source registry implementation
[ ] PostgreSQL duplicate detection implementation
[ ] Sensor-specific plausibility configuration
```

Those responsibilities belong to subsequent implementation work.

---

# 19. Future Evolution

The validation architecture should allow additional validation stages without coupling the validator to infrastructure.

Potential future validations include:

- sensor calibration status
- device firmware compatibility
- timestamp quality
- clock skew handling
- sensor-specific operating ranges
- rate-of-change validation
- cross-sensor consistency
- cryptographic message authentication
- device certificate validation

These should only be introduced when justified by project requirements or observed system behavior.

---

# 20. Summary

DT-005 establishes a controlled boundary between untrusted physical telemetry and the Digital Twin domain.

The system follows:

```text
Physical Device
      |
      v
Telemetry Message
      |
      v
+-----------------------+
| Structural Validation |
+-----------------------+
      |
      v
+-----------------------+
| Semantic Validation   |
+-----------------------+
      |
      v
+-----------------------+
| Source Validation     |
+-----------------------+
      |
      v
+-----------------------+
| Duplicate Detection   |
+-----------------------+
      |
      v
+-----------------------+
| Physical Plausibility |
+-----------------------+
      |
      v
Domain Measurement
      |
      v
Persistence / Twin
```

The central principle is:

> **Only validated physical observations may enter the Digital Twin domain.**

Telemetry validation establishes data integrity; it does not determine whether the greenhouse environment is suitable for the plant. Those concerns remain deliberately separated.