# Architecture Decision Records

This directory contains the Architecture Decision Records (ADRs) for GreenhouseDigitalTwin.

## ADR Index

| ADR | Decision | Status |
|---|---|---|
| ADR-001 | Modular Monolith | Accepted |
| ADR-002 | MQTT for Physical Communication | Accepted |
| ADR-003 | Ports and Adapters / Clean Boundaries | Accepted |
| ADR-004 | PostgreSQL as Primary Persistence | Accepted |
| ADR-005 | Historical Telemetry Is Immutable | Accepted |
| ADR-006 | Desired State Is Separate from Observed State | Accepted |
| ADR-007 | Simulation Is Physically Isolated | Accepted |
| ADR-008 | Physical Feedback Is Required for Command Confirmation | Accepted |
| ADR-009 | Current Twin State Is Separate from Historical Measurements | Accepted |
| ADR-010 | State Freshness Is Explicit | Accepted |
| ADR-011 | Measurement Time and Reception Time Are Separate | Accepted |
| ADR-012 | Domain Measurement Is Independent of Transport and Persistence | Accepted |
| ADR-013 | Measurement Persistence and Twin Update Are Atomic | Accepted |
| ADR-014 | Desired State Is a Separate Domain Concept | Accepted |
| ADR-015 | Command Confirmation Requires Physical Feedback | Accepted |
| ADR-016 | Commands Have Explicit Identity | Accepted |
| ADR-017 | Simulation Cannot Control Physical Devices | Accepted |
| ADR-018 | Modules Are Organized by Business Capability | Proposed |
| ADR-019 | Desired State Is Modeled Independently from Commands | Accepted |

## ADR Conventions

Each ADR records:

- Status
- Date
- Context
- Decision
- Consequences
- Rejected alternatives where previously established
- Constraints and invariants where relevant

ADRs document architectural decisions and their reasoning. They are not implementation tutorials.

## Status Definitions

- **Proposed:** decision has been formulated but awaits formal acceptance.
- **Accepted:** decision is part of the current architecture.
- **Superseded:** a later ADR replaces this decision.
- **Deprecated:** the decision is no longer recommended but remains historically relevant.

## Important Architectural Principles

The current architecture is centered on the following principles:

```text
Modular Monolith
       +
Ports and Adapters
       +
Business-Capability Modules
       +
MQTT for Physical Communication
       +
PostgreSQL for Durable Persistence
       +
Immutable Historical Measurements
       +
Desired State ≠ Observed State
       +
Physical Feedback Required for Confirmation
       +
Simulation Physically Isolated
       +
Explicit State Freshness
```

## Notes on Related Decisions

ADR-006, ADR-014, and ADR-019 all address the separation between intent, operations, and physical reality. They should be read together:

```text
Command
   ↓
Desired State
   ↓
Physical System
   ↓
Observed State
```

ADR-008 and ADR-015 similarly reinforce the requirement that physical confirmation comes from physical feedback rather than messaging success alone.

ADR-001 and ADR-018 should be read together:

- ADR-001 establishes the modular-monolith deployment architecture.
- ADR-018 defines the business-capability organization inside that monolith.
