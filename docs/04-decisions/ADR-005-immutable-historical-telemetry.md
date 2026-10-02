# ADR-005 — Historical Telemetry Is Immutable

- **Status:** Accepted
- **Date:** 2026-09-20
- **Decision:** Accepted historical measurements are treated as immutable evidence.

## Context

The Digital Twin needs historical observations for analysis, simulation validation, prediction evaluation, anomaly detection, and debugging.

Rewriting historical observations would destroy evidence of what the physical system actually reported.

## Decision

Once an accepted `Measurement` is persisted, its historical observation data must not be silently modified or deleted as part of normal processing.

Corrections, if ever required, must preserve the original observation and make the correction explicit.

## Consequences

- Historical data remains traceable.
- Auditability improves.
- Model validation can compare predictions with original observations.
- Data correction requires explicit mechanisms rather than silent updates.
- Storage grows monotonically with accepted observations.

## Invariant

Historical measurements answer:

> What did the physical system report?

They must not be rewritten merely because the current Twin state changes.
