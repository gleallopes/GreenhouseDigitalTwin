# ADR-016 — Commands Have Explicit Identity

- **Status:** Accepted
- **Date:** 2026-09-20
- **Decision:** Every physical command receives a unique `commandId`.

## Context

Commands cross application, messaging, and device boundaries. The system must correlate acknowledgements and physical feedback with the command that initiated the operation.

MQTT QoS 1 can also result in duplicate delivery.

## Decision

Every command has a unique distributed identity:

```text
commandId
```

The identity is used for:

- correlation;
- traceability;
- acknowledgement matching;
- duplicate handling;
- debugging;
- persistence.

Retries create a new command identity.

## Consequences

- Command histories remain explicit.
- Duplicate device messages can be handled idempotently.
- Distributed tracing/correlation is easier.
- Command records require an externally unique identifier.

## Invariant

A retry is a new command; the original command's historical outcome is not overwritten.
