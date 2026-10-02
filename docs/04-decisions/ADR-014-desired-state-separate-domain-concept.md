# ADR-014 — Desired State Is a Separate Domain Concept

- **Status:** Accepted
- **Date:** 2026-09-20
- **Decision:** Desired physical state is modeled independently from commands and observed state.

## Context

A command is an operation, while desired state represents a target condition.

For example, the command `TURN_ON` is a request to change an actuator. The resulting desired state may be `ON`, while the observed physical state may remain `OFF` until feedback confirms the change.

## Decision

Model three separate concepts:

```text
Command
Desired State
Observed State
```

Initial actuator semantics:

```text
Observed: ON | OFF | UNKNOWN
Desired:  ON | OFF | UNSET
```

`UNKNOWN` means physical state is not reliably known.

`UNSET` means no desired target has been specified.

## Consequences

- Intent and reality remain distinguishable.
- Commands remain historical operations.
- Desired state can persist beyond an individual command.
- The model becomes more expressive and requires explicit synchronization rules.
