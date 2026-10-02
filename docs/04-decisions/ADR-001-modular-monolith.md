# ADR-001 — Modular Monolith

- **Status:** Accepted
- **Date:** 2026-09-15
- **Decision:** Use a Spring Boot modular monolith as the initial backend architecture.

## Context

GreenhouseDigitalTwin is initially a single-greenhouse learning and engineering project with one physical device, a small number of sensors/actuators, and no demonstrated need for independent deployment or scaling of backend components.

A distributed microservice architecture would introduce network boundaries, distributed failure modes, deployment complexity, and operational overhead before those problems exist.

## Decision

The backend will be implemented as a **modular monolith** organized by business capability.

Initial modules include:

- Greenhouse
- Telemetry
- Twin
- Command
- Simulation
- Prediction
- Anomaly
- Infrastructure

Modules maintain explicit boundaries and communicate through application/domain contracts rather than directly manipulating one another's persistence.

## Consequences

### Positive

- One deployable application.
- Simple local development and testing.
- Lower operational complexity.
- Business boundaries are established early.
- Future extraction into services remains possible if justified.

### Negative

- Modules share one runtime and deployment unit.
- Strong discipline is required to prevent accidental coupling.
- Independent scaling is not available initially.

## Rejected Alternative

**Microservices** were rejected for the initial system because independent deployment, scaling, and team ownership have not been demonstrated as requirements.

## Revisit When

Reconsider this decision if independent deployment/scaling, organizational ownership, runtime isolation, or other concrete requirements justify distributed services.
