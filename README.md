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

## Local MQTT telemetry

The backend defaults to MQTT disabled. For a backend running directly on the
host, start the existing loopback-bound PostgreSQL and Mosquitto services with
`podman compose up -d postgres mosquitto` from the repository root. Supply the
Compose `POSTGRES_PASSWORD` and backend `DB_PASSWORD` through your local
environment. Use matching values for the same PostgreSQL role.

Enable ingestion with `MQTT_ENABLED=true` and set `MQTT_TOPICS` to the
comma-separated list of exact telemetry topics. For example:

```sh
MQTT_ENABLED=true MQTT_TOPICS=greenhouse/GH-001/telemetry \
  mvn -f backend/greenhousedigitaltwin/pom.xml spring-boot:run
```

Use `MQTT_ENABLED=false` to disable the subscriber on the next backend start.
Run only one enabled backend with a given subscriber client ID.

| Variable | Default or accepted value |
|---|---|
| `MQTT_ENABLED` | `false` |
| `MQTT_BROKER_URI` | `tcp://127.0.0.1:1883`. On the same Compose network, use `tcp://mosquitto:1883`. |
| `MQTT_CLIENT_ID` | `greenhouse-digital-twin`. Keep this stable across reconnects. |
| `MQTT_TOPICS` | Required when enabled. Exact `greenhouse/{greenhouseId}/telemetry` topics. |
| `MQTT_QOS` | `1`, required by the DT-012 profile |
| `MQTT_CLEAN_SESSION` | `false`, required by the DT-012 profile |
| `MQTT_CONNECT_TIMEOUT` | `5s` |
| `MQTT_RECONNECT_INITIAL_DELAY` | `1s` |
| `MQTT_RECONNECT_MAX_DELAY` | `30s` |
| `MQTT_MAX_PAYLOAD_BYTES` | `4096` |
| `MQTT_USERNAME`, `MQTT_PASSWORD` | Empty for the isolated local broker. Supply credentials through the environment when configured. |

Publish with the Compose broker's command-line client. Replace the sample
greenhouse, device and sensor with an existing registered temperature source.
Use fresh measurement and message IDs for each new observation and reuse both
IDs when retrying that observation.

```sh
podman compose exec -T mosquitto mosquitto_pub -h 127.0.0.1 \
  -V mqttv311 -q 1 -t 'greenhouse/GH-001/telemetry' -s <<'JSON'
{
  "measurementId": "MEAS-001",
  "messageId": "MSG-001",
  "deviceId": "ARD-001",
  "sensorId": "TEMP-001",
  "measurementType": "TEMPERATURE",
  "value": 25.4,
  "unit": "CELSIUS",
  "measuredAt": "2026-09-22T14:32:15.123456Z"
}
JSON
```

The command sends a non-retained message. Its PUBACK confirms broker delivery.
Verify ingestion through PostgreSQL's `measurement` and `twin_state` tables.
The [communication contract](docs/02-architecture/communication.md#7-qos)
describes subscriber ACKs, session recovery, and the security boundary.

### Backend verification

Use Java 25 and a Docker-compatible runtime. For rootless Podman, point
Testcontainers at the running user API socket using
`DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock`. In an environment
where Ryuk cannot run, set `TESTCONTAINERS_RYUK_DISABLED=true` for the test
invocation and verify cleanup of your disposable test resources.

From `backend/greenhousedigitaltwin`, run the MQTT suite against its disposable
Testcontainers services:

```sh
mvn -Dtest=MqttTelemetryMessageHandlerTest,MqttTelemetrySubscriberTest,PahoMqttSmokeTest,MqttTelemetryIntegrationTest test
```

For `mvn clean verify`, configure `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `DB_PASSWORD`
for an isolated PostgreSQL 17 instance. Some existing application tests use the
configured datasource directly and write test records.
