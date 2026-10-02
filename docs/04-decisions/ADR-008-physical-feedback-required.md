# ADR-008 — Physical Feedback Is Required for Command Confirmation

- **Status:** Accepted
- **Date:** 2026-09-20
- **Decision:** A physical command is confirmed only when physical feedback demonstrates the expected state.

## Context

Several events can occur without proving physical actuation:

- backend accepts a command;
- MQTT publishes it;
- Arduino receives it;
- Arduino acknowledges it.

None necessarily proves the physical state changed as intended.

## Decision

The command lifecycle distinguishes:

```text
REQUESTED
→ SENT
→ ACKNOWLEDGED
→ CONFIRMED
```

`CONFIRMED` requires physical state feedback compatible with the command.

## Consequences

- Command status reflects evidence rather than assumption.
- The system can represent requested state and actual state separately.
- More feedback messages and correlation logic are required.

## Core Rule

```text
Command delivered ≠ Physical state confirmed
```
