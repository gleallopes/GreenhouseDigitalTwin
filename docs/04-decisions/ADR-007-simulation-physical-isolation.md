# ADR-007 — Simulation Is Physically Isolated

- **Status:** Accepted
- **Date:** 2026-09-20
- **Decision:** Simulation must have no path to physical command publication.

## Context

Simulation represents hypothetical scenarios. Accidentally allowing a simulation to trigger an actuator would turn a virtual experiment into an uncontrolled physical action.

## Decision

Simulation is isolated from physical control.

```text
Simulation
    X
    │
    X──→ Physical Command
```

Simulation may read digital models and produce hypothetical results, but it cannot issue physical commands or modify observed physical state.

## Consequences

- Simulation is safer.
- What-if scenarios cannot accidentally affect the greenhouse.
- Application boundaries become clearer.
- A separate path must be used for real physical control.

## Invariant

A simulation result must never be interpreted as evidence that a physical actuator changed.
