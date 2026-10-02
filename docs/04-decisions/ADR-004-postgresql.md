# ADR-004 — PostgreSQL as Primary Persistence

- **Status:** Accepted
- **Date:** 2026-09-20
- **Decision:** Use PostgreSQL as the initial authoritative durable database.

## Context

The system needs durable historical telemetry, current Twin state, device configuration, commands, and related domain data.

The current scale does not justify introducing a specialized time-series database.

## Decision

Use PostgreSQL as the primary persistence mechanism.

Initial persistence includes:

- Greenhouse
- Device
- Sensor
- Measurement
- Twin State

Later persistence may include:

- Actuator
- Command
- Desired State
- Simulation
- Prediction
- Anomaly
- Alert

## Consequences

### Positive

- Mature relational database.
- Strong constraints and transactions.
- Suitable for current project scale.
- Supports both historical data and materialized current state.

### Negative

- Time-series-specific optimizations are not provided automatically.
- Schema design and indexing require deliberate decisions.

## Rejected Alternative

A specialized time-series database is deferred because there is currently no demonstrated workload requiring it.

## Revisit When

Reconsider if telemetry volume, retention, query patterns, or performance measurements justify specialized time-series infrastructure.
