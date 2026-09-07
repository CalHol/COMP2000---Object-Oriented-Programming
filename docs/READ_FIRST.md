# READ FIRST - COMP2000 Traffic Simulation

> Adapted from Yakov's *Codebase Reading and Defence Guide* (Sept 2026), split per pair by Henry.
> Full PDF: `docs/COMP2000_Traffic_Simulation_Codebase_Guide.pdf`.

You don't need to read the whole codebase. Read this file, then open **your pair's** guide only. Cross-read into other pairs' guides when your code touches theirs.

## The project in one minute

Java Swing traffic simulation. Cars, trucks, buses and emergency vehicles travel straight through a grid of signalised intersections. No turns, no roundabouts, no pedestrians, no physics engine.

Each tick: update lights -> read sensors -> move vehicles -> despawn off-map -> maybe spawn -> repaint.

```
network file -> roads, lanes, intersections, bus stops
                            |
                            v
each tick: update lights -> read sensors -> move vehicles
                         -> despawn -> maybe spawn -> repaint
```

## Pair assignments

| Pair | Members | Owns | Read this guide |
|---|---|---|---|
| A | Ali, Henry, Oscar | Engine, sensors, spawning, observer, display | `pair-a-engine.md` |
| B | Callum, Jubril | RoadNetwork, Road, Lane, BusStop, NetworkLoader | `pair-b-network.md` |
| C | Ben, Jacob | Vehicle + Car/Truck/Bus/EmergencyVehicle, DriverProfile | `pair-c-vehicles.md` |
| D | Addrita, Cam | TrafficLight + states, Intersection, SignalisedIntersection | `pair-d-lights.md` |

## How to run

From the repo root (`C:\Users\<you>\COMP2000` or wherever you cloned it):

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
- `<network-file>` - path to a network definition (defaults to `networks/grid.txt`)
- `--seed=<long>` - repeatable RNG seed

**There is no start button.** The moment the window opens the sim is running. Bottom bar has Pause / Resume / Step / Reset / Spawn 1. Space toggles pause, `s` steps once, `n` spawns one, `r` resets.

## Defence checklist (everyone)

- Run the program once normally and once with `--seed=123`.
- Point at the dashboard seed and explain reproducibility.
- Follow one car through `SimulationEngine` -> `TrafficSensor` -> `Vehicle.move`.
- Explain the centre-distance-to-bumper-gap calculation in your own words.
- Show a red light, then explain `STOP_LINE_RADIUS` and the all-red phase.
- Show a bus stop and explain why a bus only serves it when its lane matches.
- Show an emergency vehicle and state: **priority, not collision immunity**.
- Know that the display *reads* state; it does not control traffic movement.
- Be honest about scope: no turns, roundabouts, pedestrians or physics engine.

## Constants everyone should recognise

| Constant | Value | Meaning |
|---|---|---|
| `LANE_WIDTH` / `LANE_HALF_WIDTH` | 18 / 9 | Road is 36 units of asphalt; lane centres sit 9 units from road centre. |
| `STOP_LINE_RADIUS` | 52 | Virtual stop line distance before intersection centre. |
| `SIGHT_RANGE` | 250 | Hazards farther than this are ignored per tick. |
| `MIN_BUMPER_GAP` | 4 | Free space after subtracting vehicle half-lengths. |
| `SPAWN_CAP` | 18 | Max active vehicles before spawning pauses. |
| `SPAWN_PROBABILITY` | 0.06 | Chance per tick of attempting a spawn. |
| `DWELL_TICKS` | 30 | How long a bus waits at a served stop. |
| `ADAPTIVE_QUEUE_HIGH` | 4 | Vehicles queued before green may extend. |

## For every class you touch, ask three questions

1. What state does it own?
2. What does it change?
3. Who calls it?
