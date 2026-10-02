# ADR-012 — Domain Measurement Is Independent of Transport and Persistence

- **Status:** Accepted
- **Date:** 2026-09-20
- **Decision:** The domain `Measurement` is independent of MQTT payloads, database records, and REST representations.

## Context

The same observation crosses multiple technical boundaries. Reusing one representation everywhere would couple the domain to infrastructure.

## Decision

Maintain distinct representations:

```text
MQTT Payload
    ≠
Transport DTO
    ≠
Domain Measurement
    ≠
Database Entity
    ≠
REST DTO
```

The domain `Measurement` contains domain concepts and invariants, while adapters perform mapping.

## Consequences

- Domain tests require no infrastructure.
- Transport/database changes have less impact on domain logic.
- Mapping code increases slightly.
- JPA annotations must not be required by the domain entity.
