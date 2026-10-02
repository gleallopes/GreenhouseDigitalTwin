# Product Vision

## Project

**GreenhouseDigitalTwin**

## Vision

GreenhouseDigitalTwin is a digital twin for small agricultural greenhouses suitable for apartments.

The system connects a physical greenhouse prototype to a software representation of its current state. The initial physical environment is a small greenhouse used to grow mint.

The digital twin is not merely a dashboard. It maintains a representation of the physical system, incorporates real measurements, tracks state, supports simulation and prediction, detects anomalies, and supports controlled bidirectional communication with the physical system.

## Problem

A physical greenhouse provides environmental measurements and can be affected by physical actions, but those observations and actions are difficult to reason about without a software representation.

The project therefore needs a system that can:

1. observe the physical greenhouse;
2. represent its current state digitally;
3. preserve historical observations;
4. evaluate environmental conditions;
5. simulate hypothetical scenarios without affecting the physical system;
6. generate explainable future-state predictions;
7. detect anomalous conditions;
8. communicate commands to physical actuators;
9. distinguish requested, acknowledged, and physically observed states;
10. compare predicted state with observed state.

## Initial Environmental Targets

The initial project assumptions are:

| Variable | Target range |
|---|---:|
| Temperature | 20–28 °C |
| Relative humidity | 60–80 % |
| Luminosity | 5,000–10,000 lux |

These values are project requirements/assumptions for the initial greenhouse model, not universal agricultural recommendations.

## Initial Physical System

The initial prototype includes:

- Arduino-based physical device;
- temperature sensor;
- humidity sensor;
- luminosity sensor;
- one simple actuator, initially a test light/LED;
- physical mint greenhouse.

## Core Concept

The system follows this model:

`Physical system → observations → Digital Twin → analysis/simulation/prediction → commands → physical system → feedback`

The Digital Twin must never treat a command as proof that the physical world changed.

## Success Criteria

The project is considered successful when:

1. a real or representative physical measurement can travel through the device boundary and communication layer;
2. the backend validates and persists accepted telemetry;
3. the Digital Twin reflects the latest valid physical state;
4. the current state is available through the application API and UI;
5. a simulated scenario can be executed without affecting the physical greenhouse;
6. predictions can be compared with later observations;
7. physical events can be detected and represented as anomalies/alerts when appropriate;
8. a command can travel from the Digital Twin to a physical actuator;
9. the system distinguishes command delivery/acknowledgement from physical-state confirmation;
10. the system remains explainable and testable when communication or devices fail.

## Engineering Principles

- Physical state takes precedence over assumed state.
- Historical measurements are immutable.
- Invalid telemetry must not update the Digital Twin.
- Simulation must be physically isolated.
- Desired state, command, and observed state are distinct concepts.
- Device communication failure must be explicit rather than silently interpreted as normal state.
- The system should remain simple enough to evolve without premature distributed-system complexity.
