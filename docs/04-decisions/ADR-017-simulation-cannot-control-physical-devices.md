# ADR-017 — Simulation Cannot Control Physical Devices

- **Status:** Accepted
- **Date:** 2026-09-20
- **Decision:** The simulation subsystem has no capability to publish physical commands.

## Context

Simulation represents hypothetical behavior. It must not accidentally affect the real greenhouse.

## Decision

The simulation application boundary has no dependency path to the physical command publisher.

```text
Simulation
    |
    X
    |
Physical Command Publisher
```

Simulation may manipulate hypothetical digital state and produce simulation results only.

## Consequences

- Strong safety boundary.
- Simulation can be run freely without physical side effects.
- Integration tests can execute simulations without hardware.
- Real control requires an explicit command path outside simulation.
