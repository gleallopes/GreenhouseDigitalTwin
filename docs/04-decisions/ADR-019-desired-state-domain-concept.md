# ADR-019 — Desired State Is Modeled Independently from Commands

- **Status:** Accepted
- **Date:** 2026-09-20
- **Decision:** Desired physical state is an explicit domain concept independent from command records.

## Context

A command is an operation issued at a point in time. Desired state represents the condition the system currently wants the physical actuator to reach.

Conflating the two makes it difficult to represent persistent intent, retries, reconciliation, and differences between desired and observed reality.

## Decision

Model:

```text
Command = operation/request
Desired State = target condition
Observed State = physical reality
```

Example:

```text
Command: TURN_ON
Desired State: ON
Observed State: OFF
```

The system must be able to represent this temporary divergence.

## Consequences

- Reconciliation becomes explicit.
- Desired intent can persist independently of command history.
- Command records remain immutable historical operations.
- Observed state remains authoritative for physical reality.
- Additional synchronization logic is required.

## Core Invariant

```text
Command ≠ Desired State ≠ Observed State
```
