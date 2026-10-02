# ADR-015 — Command Confirmation Requires Physical Feedback

- **Status:** Accepted
- **Date:** 2026-09-20
- **Decision:** A command reaches `CONFIRMED` only when physical feedback demonstrates the expected state.

## Context

Device acknowledgement proves receipt/acceptance of a command, not necessarily physical actuation.

## Decision

For initial actuator commands:

```text
TURN_ON  → expected physical state ON
TURN_OFF → expected physical state OFF
```

A command in `ACKNOWLEDGED` state becomes `CONFIRMED` only after compatible physical state feedback is observed.

Temporary mismatch is not immediately interpreted as permanent failure.

## Consequences

- Physical feedback is part of command lifecycle processing.
- The system can distinguish acknowledgement from confirmation.
- Confirmation timeout handling is required.
- Current actuator state remains independent from command terminal status.

## V1 Constraint

Initially, only one non-terminal command may be active for an actuator.
