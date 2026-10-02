# ADR-010 — State Freshness Is Explicit

- **Status:** Accepted
- **Date:** 2026-09-20
- **Decision:** Current Twin state must expose the freshness of its observations explicitly.

## Context

A stored value can remain unchanged while the physical device becomes disconnected. Displaying the last value without its age can make stale information appear current.

## Decision

Freshness is derived from the age of the latest accepted observation.

Initial conceptual states:

```text
FRESH
AGING
STALE
UNKNOWN
```

`UNKNOWN` means that no valid observation has ever been received.

Freshness is not stored as an authoritative historical fact; it is derived from timestamps and current time.

## Consequences

- The UI can distinguish current from stale information.
- Restart recovery can recalculate freshness.
- Exact freshness thresholds must be determined from the actual telemetry interval/configuration.
- The system must retain observation timestamps.
