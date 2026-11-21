# GreenhouseDigitalTwin

Digital Twin system for a small agricultural greenhouse designed for apartment use.

## Technology

- Java 25
- Spring Boot
- Maven
- PostgreSQL
- MQTT / Mosquitto
- React
- TypeScript
- Docker

## Project Structure

- `backend/` — Spring Boot backend
- `frontend/` — React frontend
- `firmware/` — Arduino firmware
- `infrastructure/` — local infrastructure configuration
- `docs/` — project documentation

## Telemetry HTTP API

See the [OpenAPI contract](docs/api/telemetry-v1.openapi.yml) for `POST /api/v1/telemetry`,
including request and response examples, error codes, and the request size limit.
