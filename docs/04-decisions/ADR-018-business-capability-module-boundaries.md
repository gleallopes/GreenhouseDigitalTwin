# ADR-018 — Modules Are Organized by Business Capability

- **Status:** Proposed
- **Date:** 2026-09-20
- **Decision:** Organize the modular monolith primarily by business capability rather than technical layer.

## Context

A traditional structure such as:

```text
controller/
service/
repository/
```

groups code by technology rather than business responsibility and can make module ownership and dependencies unclear.

## Decision

Use business-oriented modules such as:

```text
greenhouse/
telemetry/
twin/
command/
simulation/
prediction/
anomaly/
```

Each module may internally contain:

```text
domain/
application/
infrastructure/
```

Modules communicate through explicit contracts and must not directly manipulate another module's persistence.

## Consequences

### Positive

- High cohesion around business capabilities.
- Explicit module ownership.
- Easier evolution of individual capabilities.
- Better foundation for future service extraction if ever justified.

### Negative

- Requires deliberate dependency discipline.
- Some concepts cross module boundaries and require explicit contracts.

## Relationship to ADR-001

This decision refines the modular monolith decision. ADR-001 defines the deployment architecture; ADR-018 defines the internal module organization.

## Status Note

This decision is **Proposed** and should be formally accepted after the module dependency rules are reviewed.
