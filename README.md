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
  Main.java                  <- entry point
  engine/                    <- SimulationEngine, SimulationObserver, Statistics
  view/                      <- SimulationDisplay (JPanel)
  model/
    road/                    <- RoadNetwork, Road, Lane, Intersection, BusStop
    vehicle/                 <- Vehicle (abstract), Car, Truck, Bus, EmergencyVehicle
    light/                   <- TrafficLight, LightState, RedState, GreenState, YellowState
  factory/                   <- NetworkLoader
  strategy/                  <- TurnStrategy, RandomTurnStrategy, WeightedRandom<T>
  util/                      <- Direction enum, LightPhase enum
  exception/                 <- SimulationException, InvalidNetworkException
```

## How to Run

Compile from the `src/` directory:
```
javac -d out trafficsim/Main.java $(find trafficsim -name "*.java")
java -cp out trafficsim.Main
```

## Design Patterns

| Pattern | Where | Rubric |
|---------|-------|--------|
| Inheritance | Vehicle -> Car / Truck / Bus / EmergencyVehicle | Week 7 |
| Generics | WeightedRandom<T> | Week 7 |
| Exceptions | SimulationException -> InvalidNetworkException | Week 7 |
| Observer | SimulationObserver / SimulationDisplay | Week 13 |
| State | LightState / Red / Green / YellowState | Week 13 |
| Strategy | TurnStrategy / RandomTurnStrategy | Week 13 |
| Factory | NetworkLoader | Week 13 |
| Streams | Statistics | Week 13 |

## Communication

Discord -- respond when possible, notify the team if you can't make a deadline.
