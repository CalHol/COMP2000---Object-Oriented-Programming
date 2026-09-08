# COMP2000 — Traffic Simulation

A Java OOP group project simulating road traffic with vehicles, intersections, and traffic lights.

## Where to start

Don't read the whole codebase. Start with the per-pair guides in [`docs/`](docs/):

1. Everyone reads [`docs/READ_FIRST.md`](docs/READ_FIRST.md) — overview, how to run, defence checklist.
2. Then open **your pair's** guide only:
   - Pair A (Ali, Henry, Oscar) — [`docs/pair-a-engine.md`](docs/pair-a-engine.md)
   - Pair B (Callum, Jubril) — [`docs/pair-b-network.md`](docs/pair-b-network.md)
   - Pair C (Ben, Jacob) — [`docs/pair-c-vehicles.md`](docs/pair-c-vehicles.md)
   - Pair D (Addrita, Cam) — [`docs/pair-d-lights.md`](docs/pair-d-lights.md)
3. Full 11-page reference (Yakov's original): [`docs/COMP2000_Traffic_Simulation_Codebase_Guide.pdf`](docs/COMP2000_Traffic_Simulation_Codebase_Guide.pdf).

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
  view/                      <- SimulationDisplay (JPanel renderer and keyboard controls)
  model/
    road/                    <- RoadNetwork, Road, Lane, BusStop, Intersection (sealed),
                                SignalisedIntersection
    vehicle/                 <- Vehicle (sealed abstract), Car, Truck, Bus,
                                EmergencyVehicle, DriverProfile
    light/                   <- TrafficLight, LightState, RedState, GreenState, YellowState
  factory/                   <- NetworkLoader
  strategy/                  <- WeightedRandom<T>
  util/                      <- Direction, LightPhase, Axis enums
  exception/                 <- SimulationException, InvalidNetworkException
networks/
  grid.txt                   <- default demo network (3x3 signalised grid)
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
- `--seed=<long>` — deterministic RNG seed for spawning and driver slowdown

The GUI shows the original 1100×700 top-down city view. Keyboard controls: `space` pauses or resumes the simulation and `r` resets it.
The dashboard displays the generated run seed. Launching normally creates a new pattern; passing `--seed=<long>` deliberately repeats one.

## Design Patterns

| Pattern | Where | Rubric |
|---------|-------|--------|
| Inheritance | `Vehicle` (sealed) → `Car` / `Truck` / `Bus` / `EmergencyVehicle` | Week 7 |
| Generics | `WeightedRandom<T>` | Week 7 |
| Exceptions | `SimulationException` → `InvalidNetworkException` | Week 7 |
| Observer | `SimulationObserver` / `SimulationDisplay` | Week 13 |
| State | `LightState` / `RedState` / `GreenState` / `YellowState` | Week 13 |
| Factory | `NetworkLoader` (file + built-in default) | Week 13 |
| Streams / lambdas | `Statistics`, network parsing, vehicle cleanup | Week 13 |
| Threading | Background simulation loop with atomic UI snapshots | Week 13 |
| Sealed hierarchies | `Vehicle`, `Intersection` | Modern Java |
| Records | `SensorReading` | Modern Java |

## Features

- **Straight-through traffic** — vehicles stay in their lane through every junction
- **Collision avoidance** — vehicles keep a safe same-lane gap and wait while conflicting traffic clears a junction
- **Emergency priority** — siren-on vehicles may pass red lights but wait for occupied crossings; conflicting traffic yields only when the emergency approach is clear
- **Bus service** — buses dwell briefly at Central Station, Museum, or University when the stop serves their lane, then continue
- **Random traffic** — production runs randomise spawn timing, boundary lane, vehicle type, driver profile, cargo weight, and slowdown behaviour; `--seed` makes it reproducible
- **Smooth updates** — the simulation advances atomically at 30 updates per second so rendering never sees a half-updated world
- **Adaptive signal timing** — extend green if queue exceeds threshold
- **Australian lane placement** — vehicles spawn and travel on the left side of the road

## Communication

Discord — respond when possible, notify the team if you can't make a deadline.
