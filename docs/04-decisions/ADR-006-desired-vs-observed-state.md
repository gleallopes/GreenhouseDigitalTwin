# ADR-006 — Desired State Is Separate from Observed State

- **Status:** Accepted
- **Date:** 2026-09-20
- **Decision:** Desired physical state and observed physical state are separate concepts.

## Context

A command expresses an intention, but the physical system may fail to execute it or may be changed by another cause.

Assuming that a successful command publication means the physical world changed would make the Digital Twin inaccurate.

## Decision

Maintain separate representations:

```text
Desired State = what the system wants
Observed State = what the physical system reports
Command = an operation requesting a change
```

The observed physical state is authoritative for representing current physical reality.

## Consequences

- Commands cannot automatically update observed state.
- Temporary mismatch is representable.
- Failures and manual/device-originated changes can be detected.
- The model becomes more explicit and slightly more complex.

## Core Rule

```text
Command ≠ Desired State ≠ Observed State
```
