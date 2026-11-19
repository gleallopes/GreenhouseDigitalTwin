# GreenhouseDigitalTwin — Codex Instructions

## Project

GreenhouseDigitalTwin is an IoT digital twin system for a small apartment greenhouse.

Technology:
- Java 25 LTS
- Spring Boot 4.1.1
- Maven
- PostgreSQL 17
- Flyway
- JUnit, Mockito, Testcontainers
- Hexagonal Architecture

## Architecture

Follow the existing hexagonal architecture.

- Domain layer: business rules and domain models.
- Application layer: use cases, ports, and orchestration.
- Infrastructure layer: HTTP, persistence, messaging, and external adapters.

Do not introduce infrastructure dependencies into the domain.

Prefer constructor injection.

Do not bypass existing application ports.

Use application.yml rather than application.properties.

## Development workflow

Before implementing a DT:

1. Inspect the existing codebase and relevant documentation.
2. Identify existing implementations that can be reused.
3. Explain the requirements and architectural implications.
4. Propose a step-by-step implementation plan.
5. Wait for explicit approval before editing files.

During implementation:

- Work on one DT at a time.
- Keep changes minimal and focused.
- Explain important design decisions.
- Write or update tests for behavioral changes.
- Avoid unnecessary refactoring.
- Do not modify database migrations that have already been applied.
- Create new Flyway migrations when schema changes are necessary.
- Do not add dependencies without approval.

## Critical telemetry invariants

- Duplicate telemetry must be handled idempotently.
- Identical retries return ALREADY_PROCESSED.
- Conflicting message identities must be rejected.
- Historical measurements must be preserved.
- Older measurements must not overwrite newer twin state.
- Equal timestamps must not overwrite current twin state.
- Measurement insertion and twin-state updates must preserve transactional consistency.
- Validation must verify source identity, unit compatibility, and physical plausibility.

## Security

- Never hardcode credentials, tokens, or secrets.
- Treat external telemetry as untrusted input.
- Do not disable security controls to make tests pass.
- Avoid logging secrets or unnecessary sensitive data.
- Flag security risks and explain their impact.

## Testing

Run relevant tests after each implementation step.

Before marking a DT complete, run:

mvn clean verify

Never claim tests passed unless they actually ran successfully.

## Teaching and reporting

After implementing a change, explain:

1. What changed and why.
2. Which files were modified.
3. How the implementation works.
4. Which architectural principles were applied.
5. Which tests were executed and their results.
6. Security considerations and limitations.
7. What I should understand before moving forward.

Use clear technical English and explain unfamiliar concepts.

Do not create commits, push changes, or modify unrelated files without explicit approval.
