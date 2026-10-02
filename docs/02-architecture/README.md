# GreenhouseDigitalTwin — Architecture

This directory contains the formal architecture baseline for GreenhouseDigitalTwin.

## Documents

- `system-context.md` — system boundary, actors, external systems, and major interactions.
- `architecture.md` — architectural style, modules, layers, dependencies, and runtime structure.
- `data-model.md` — conceptual persistence model and important data relationships.
- `communication.md` — MQTT communication architecture, topics, payload responsibilities, and reliability semantics.

## Architectural baseline

The system is a **modular monolith** implemented with Spring Boot.

The architecture combines:
- business-capability modules;
- Ports and Adapters / Clean Architecture boundaries;
- PostgreSQL for durable persistence;
- MQTT for physical-device communication;
- React/TypeScript for the user interface.

## Core rules

1. Domain code must not depend on infrastructure technologies.
2. Application use cases depend on domain concepts and ports.
3. Infrastructure implements application ports.
4. Modules communicate through explicit boundaries.
5. No module directly manipulates another module's persistence.
6. Historical measurements are immutable.
7. Current Twin state is separate from historical measurement history.
8. Desired state, command, and observed physical state are separate concepts.
9. Simulation has no path to physical commands.
10. MQTT delivery is not equivalent to physical actuation.
11. Physical feedback is required to confirm expected actuator state.
12. The architecture should avoid premature distributed-system complexity.

## Architecture status

This document set represents the current architecture baseline. Architectural changes that materially affect these rules should be captured in an ADR.
