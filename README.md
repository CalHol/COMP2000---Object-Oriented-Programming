# COMP2000 — Traffic Simulation

A Java OOP group project simulating road traffic with vehicles, intersections, and traffic lights.

## Team

| Name | Pair | Responsibility |
|------|------|----------------|
| Ali | A | Core Engine + Observer pattern |
| Henry | A | Core Engine + Observer pattern |
| Oscar | A | Core Engine + Observer pattern |
| Callum | B | Road Network + Factory pattern |
| Jubril | B | Road Network + Factory pattern |
| Ben | C | Vehicles |
| Jacob | C | Vehicles |
| Addrita | D | Traffic Lights + Intersections |
| Cam | D | Traffic Lights + Intersections |

## Project Structure

```
src/trafficsim/
  Main.java                  <- entry point (loads networks/grid.txt if present)
  SimConstants.java          <- central tuning knobs
  engine/                    <- SimulationEngine, SimulationObserver, Statistics,
                                SensorReading, TrafficSensor, VehicleSpawner
  view/                      <- SimulationDisplay (JPanel renderer), MainFrame
  command/                   <- SimulationCommand (sealed) + Pause/Resume/Step/Reset/
                                SetTickRate/SpawnOne commands
  pedestrian/                <- Pedestrian, CrosswalkPedestrian, PedestrianPopulation
  model/
    road/                    <- RoadNetwork, Road, Lane, BusStop, Intersection (sealed),
                                SignalisedIntersection, Roundabout
    vehicle/                 <- Vehicle (sealed abstract), Car, Truck, Bus,
                                EmergencyVehicle, DriverProfile, VehicleVisitor
    light/                   <- TrafficLight, LightState, RedState, GreenState, YellowState
  factory/                   <- NetworkLoader
  strategy/                  <- TurnStrategy, RandomTurnStrategy,
                                StraightPreferredTurnStrategy, WeightedRandom<T>
  util/                      <- Direction, LightPhase, Axis enums
  exception/                 <- SimulationException, InvalidNetworkException
networks/
  grid.txt                   <- default demo network (3x3 grid with 2 roundabouts)
```

## How to Run

From the repo root:

**PowerShell (Windows):**
```
javac -d src/out (Get-ChildItem src/trafficsim -Recurse -Filter *.java | ForEach-Object { $_.FullName })
java -cp src/out trafficsim.Main
```

**Bash / Git Bash:**
```
javac -d src/out $(find src/trafficsim -name "*.java")
java -cp src/out trafficsim.Main
```

Optional args:
- `<network-file>` — path to a network definition (defaults to `networks/grid.txt`)
- `--seed=<long>` — deterministic RNG seed for the shared noise source

The GUI shows a 1320×1010 top-down view. Controls in the bottom toolbar: **Pause / Resume / Step / Reset / Spawn 1** + tick-rate slider. Keyboard: `space` toggle-pause, `s` step-once, `n` spawn-one, `r` reset. Checkboxes toggle **Highlight EV** (yellow halo around emergency vehicles) and **Congestion overlay** (per-road red tint).

## Design Patterns

| Pattern | Where | Rubric |
|---------|-------|--------|
| Inheritance | `Vehicle` (sealed) → `Car` / `Truck` / `Bus` / `EmergencyVehicle` | Week 7 |
| Generics | `WeightedRandom<T>`, `VehicleVisitor<R>` | Week 7 |
| Exceptions | `SimulationException` → `InvalidNetworkException` | Week 7 |
| Observer | `SimulationObserver` / `SimulationDisplay` | Week 13 |
| State | `LightState` / `RedState` / `GreenState` / `YellowState` | Week 13 |
| Strategy | `TurnStrategy` / `RandomTurnStrategy` / `StraightPreferredTurnStrategy` | Week 13 |
| Factory | `NetworkLoader` (file + built-in default) | Week 13 |
| Command | `SimulationCommand` (sealed) + 6 concrete commands | Week 13 |
| Visitor | `VehicleVisitor<R>` over sealed `Vehicle` | Week 13 |
| Streams / lambdas | `Statistics`, HUD chart | Week 13 |
| Parallelism | `SimulationEngine.step` — 4-phase (lights, sensors, moves, transfers) | Week 13 |
| Sealed hierarchies | `Vehicle`, `Intersection`, `SimulationCommand` | Modern Java |
| Records | `SensorReading` | Modern Java |

## Features

- **Fluid turns at signals** — Bezier arc interpolation, no teleporting
- **Circulating roundabouts** — vehicles enter, arc CCW around the ring, exit at chosen direction; in-ring spacing enforced
- **Adaptive signal timing** — extend green if queue exceeds threshold
- **Day/night cycle** — 2400-tick sinusoidal, street lamps + headlight cones + varied window lighting
- **Pedestrians** — walk block perimeters, park paths, and crosswalks (yield when their axis is green)
- **Tunnel portals** — roads enter/exit through stone-arched openings at map edges
- **Emergency vehicles** — blaze through traffic; nearby vehicles slow and nudge to the shoulder

## Communication

Discord — respond when possible, notify the team if you can't make a deadline.
