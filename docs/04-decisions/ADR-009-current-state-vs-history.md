# ADR-009 — Current Twin State Is Separate from Historical Measurements

- **Status:** Accepted
- **Date:** 2026-09-20
- **Decision:** Maintain a materialized current Twin state separately from immutable historical measurements.

## Context

Historical measurements answer what happened over time. The application also needs efficient access to the latest known state.

Querying the entire measurement history for every current-state request would unnecessarily couple current-state access to historical data.

## Decision

Maintain:

```text
MEASUREMENT
    = historical observations

TWIN_STATE
    = latest known state
```

The Twin state is derived from accepted observations and is not an independent source of physical truth.

## Consequences

- Current-state reads are efficient.
- Historical evidence remains intact.
- Synchronization logic must prevent older observations from regressing current state.
- Persistence must keep history and current state consistent.

## Important Rule

The materialized Twin state represents the latest accepted knowledge; the physical world remains the ultimate reality.
