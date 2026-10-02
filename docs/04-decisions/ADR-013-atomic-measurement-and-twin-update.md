# ADR-013 — Measurement Persistence and Twin Update Are Atomic

- **Status:** Accepted
- **Date:** 2026-09-20
- **Decision:** Accepted measurement persistence and materialized Twin state update occur in the same PostgreSQL transaction.

## Context

Historical measurement history and current Twin state represent two views of the same accepted observation.

Allowing one to commit while the other fails would create inconsistency.

## Decision

Within one PostgreSQL transaction:

1. Persist the accepted `Measurement`.
2. Update `TWIN_STATE` if the observation is newer than the current observation for that sensor/type.
3. Commit both together.

Duplicate messages are identified by `messageId` and processed idempotently.

Older out-of-order measurements may be preserved in history but must not regress current Twin state.

MQTT processing itself is outside the database transaction.

## Consequences

- History and current state remain transactionally consistent.
- Restart recovery is simpler.
- Out-of-order observations are handled explicitly.
- MQTT/database distributed transactions are avoided.

## Invariant

```text
Measurement committed
    ↔
Corresponding Twin update committed when applicable
```

Both changes succeed or fail together within PostgreSQL.
