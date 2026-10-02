# ADR-003 — Ports and Adapters / Clean Boundaries

- **Status:** Accepted
- **Date:** 2026-09-20
- **Decision:** Use Ports and Adapters boundaries between domain/application logic and infrastructure.

## Context

The project uses MQTT, PostgreSQL, REST, and hardware. These technologies should not become dependencies of the core domain.

## Decision

Use the following dependency direction:

```text
Infrastructure → Application → Domain
```

The domain must remain independent of Spring, JPA, MQTT, PostgreSQL, and REST.

Application ports represent dependencies required by use cases. Infrastructure adapters implement those ports.

Example:

```text
Use Case
   ↓
Repository Port
   ↑
PostgreSQL Adapter
```

## Consequences

- Business rules remain testable without infrastructure.
- Technology can be replaced with less impact on the domain.
- Adapter code is separated from business rules.
- More mapping and boundary code is required.

## Constraints

REST and MQTT adapters must not contain business rules. Infrastructure must not become the owner of domain behavior.
