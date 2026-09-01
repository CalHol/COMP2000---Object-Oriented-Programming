package trafficsim.engine;

import trafficsim.SimConstants;
import trafficsim.command.SimulationCommand;
import trafficsim.util.Direction;
import trafficsim.model.road.Intersection;
import trafficsim.model.road.Lane;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.road.Roundabout;
import trafficsim.pedestrian.PedestrianPopulation;
import trafficsim.engine.Statistics;
import trafficsim.model.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * The four-phase heartbeat of the simulation. See vault
 * {@code 02-Design Patterns/Parallelism} for the phase invariants.
 */
public class SimulationEngine {

    private final RoadNetwork network;
    private final List<SimulationObserver> observers = new ArrayList<>();
    private final ConcurrentLinkedDeque<SimulationCommand> commandQueue = new ConcurrentLinkedDeque<>();
    private final VehicleSpawner spawner;
    private final PedestrianPopulation pedestrians;

    /** Vehicles currently tracing a Bezier turn at a signalised intersection. */
    private final List<Vehicle> turningVehicles = new ArrayList<>();

    /** Rolling avg-speed samples for HUD strip chart. */
    private final double[] speedHistory = new double[SimConstants.HISTORY_SAMPLES];
    private int historyHead = 0;

    private volatile int tickRate;
    private volatile boolean running;
    private volatile boolean paused;
    private long tickCount;

    public SimulationEngine(RoadNetwork network, int tickRate) {
        this(network, tickRate, new VehicleSpawner());
    }

    public SimulationEngine(RoadNetwork network, int tickRate, VehicleSpawner spawner) {
        this.network = network;
        this.tickRate = tickRate;
        this.spawner = spawner;
        this.pedestrians = new PedestrianPopulation(network, 4);
    }

    public PedestrianPopulation getPedestrians() { return pedestrians; }
    public VehicleSpawner getSpawner() { return spawner; }
    public double[] getSpeedHistory() { return speedHistory; }
    public int getHistoryHead() { return historyHead; }
    public List<Vehicle> getTurningVehicles() { return turningVehicles; }

    public void addObserver(SimulationObserver observer) { observers.add(observer); }
    public void notifyObservers() { for (SimulationObserver o : observers) o.onSimulationStep(); }

    public void submit(SimulationCommand cmd) { commandQueue.add(cmd); }

    private void drainCommands() {
        SimulationCommand cmd;
        while ((cmd = commandQueue.pollFirst()) != null) cmd.execute(this);
    }

    public void step() {
        drainCommands();
        if (paused) return;
        doStep();
    }

    public void forceStepOnce() { doStep(); }

    private void doStep() {
        tickCount++;
        updateLights();
        List<Vehicle> vehicles = collectVehicles();
        Map<Vehicle, SensorReading> readings = readSensors(vehicles);
        moveLaneVehicles(readings);
        moveRingVehicles();
        moveTurningVehicles();
        completeTurns();
        handleRoundaboutExits();
        initiateTurns(readings);
        handleRoundaboutEntries(readings);
        spawner.despawnOffMap(network);
        if (vehicles.size() + turningVehicles.size() < SimConstants.SPAWN_CAP) spawner.tick(network);
        pedestrians.stepAll();
        recordMetrics(vehicles);
        notifyObservers();
    }

    // -- named phase methods -------------------------------------------------

    private void updateLights() {
        network.getIntersections().parallelStream().forEach(Intersection::update);
    }

    private Map<Vehicle, SensorReading> readSensors(List<Vehicle> vehicles) {
        return vehicles.parallelStream()
                .collect(Collectors.toMap(v -> v, v -> TrafficSensor.read(v, network)));
    }

    private void moveLaneVehicles(Map<Vehicle, SensorReading> readings) {
        readings.entrySet().parallelStream().forEach(e -> e.getKey().move(e.getValue()));
    }

    private void moveRingVehicles() {
        for (Intersection ix : network.getIntersections()) {
            if (ix instanceof Roundabout ring) {
                for (Vehicle rv : ring.getInRing()) rv.moveInRing();
            }
        }
    }

    private void moveTurningVehicles() {
        // Advance active turns; movement happens inside Vehicle.move via moveAlongTurn,
        // but turning vehicles aren't in any lane so the readings loop skipped them.
        // Give each one a tick with a "clear" sensor reading so its own move() advances the arc.
        for (Vehicle v : turningVehicles) v.move(SensorReading.clear(null, null, v.getDirection()));
    }

    private void completeTurns() {
        var it = turningVehicles.iterator();
        while (it.hasNext()) {
            Vehicle v = it.next();
            if (!v.turnComplete()) continue;
            Lane target = v.getTurnFinalLane();
            Direction newDir = v.getTurnFinalDirection();
            Intersection at = v.getTurnIntersection();

            // Verify the final spot in the target lane is actually clear NOW; if a queue built
            // up during the turn, walk further along the direction until we find room.
            double[] spot = null;
            if (target != null && at != null) {
                spot = findClearSpot(target, at.getX(), at.getY(), newDir, v.getLength());
            }
            if (spot == null) {
                // Can't finish safely — hold the turn one more tick (leave in turningVehicles).
                continue;
            }

            it.remove();
            v.finishSignalTurn();
            v.setDirection(newDir);
            v.repositionAt(spot[0], spot[1]);
            target.addVehicle(v);
            v.markTurned();
        }
    }

    /** Kick off any pending turns as fluid Bezier arcs instead of teleport-transfers. */
    private void initiateTurns(Map<Vehicle, SensorReading> readings) {
        for (var entry : readings.entrySet()) {
            Vehicle v = entry.getKey();
            Direction dir = v.consumePendingDirection();
            if (dir == null) continue;
            SensorReading r = entry.getValue();
            Intersection at = r.currentIntersection();
            if (at == null) continue;
            if (at instanceof Roundabout) continue;
            if (v.isTurningAtSignal()) continue;

            Lane target = null;
            for (Road road : at.getConnectedRoads()) {
                Lane lane = road.laneFor(dir);
                if (lane != null && lane != v.getLane()) { target = lane; break; }
            }
            if (target == null) continue;

            v.getLane().removeVehicle(v);
            v.startSignalTurn(at, dir, target);
            turningVehicles.add(v);
        }
    }

    private void handleRoundaboutEntries(Map<Vehicle, SensorReading> readings) {
        for (var entry : readings.entrySet()) {
            Vehicle v = entry.getKey();
            if (v.isInRing()) continue;
            SensorReading r = entry.getValue();
            if (!(r.currentIntersection() instanceof Roundabout ring)) continue;
            if (v.getLane() == null) continue;

            Direction approach = v.getDirection();
            double entryAngle = Vehicle.entryAngleFor(approach);
            if (!ring.entryIsClear(entryAngle)) continue;

            var options = new ArrayList<Direction>();
            options.add(approach);
            options.addAll(approach.perpendiculars());
            Direction exit = v.getTurnStrategy() == null
                    ? approach : v.getTurnStrategy().chooseDirection(options);

            v.getLane().removeVehicle(v);
            v.enterRing(ring, approach, exit);
            ring.addToRing(v);
        }
    }

    private void handleRoundaboutExits() {
        for (Intersection ix : network.getIntersections()) {
            if (!(ix instanceof Roundabout ring)) continue;
            var snapshot = new ArrayList<>(ring.getInRing());
            for (Vehicle v : snapshot) {
                if (v.getRingExit() == null) continue;
                if (!v.hasReachedRingExit()) continue;
                Direction exit = v.getRingExit();
                Lane targetLane = null;
                for (Road road : ring.getConnectedRoads()) {
                    Lane l = road.laneFor(exit);
                    if (l != null) { targetLane = l; break; }
                }
                if (targetLane == null) continue;
                // Start OUTSIDE the outer ring so exit doesn't teleport the vehicle
                // back inside the ring — the visual "glitch" the user was seeing.
                double[] spot = findClearSpot(targetLane, ring.getX(), ring.getY(), exit,
                        v.getLength(), ring.getOuterRadius() + 6);
                if (spot == null) continue;
                ring.removeFromRing(v);
                v.exitRing();
                v.setDirection(exit);
                v.repositionAt(spot[0], spot[1]);
                targetLane.addVehicle(v);
            }
        }
    }

    private static double[] findClearSpot(Lane target, int cx, int cy, Direction dir, double myLength) {
        return findClearSpot(target, cx, cy, dir, myLength, 36.0);
    }

    /**
     * Walk outward along {@code dir} from ({@code cx}, {@code cy}) starting at
     * {@code startDist}, offsetting perpendicular-right onto the target lane, until
     * we find a spot with no overlap. {@code startDist} must be large enough to
     * clear the intersection footprint (INT_HALF for signals, outerRadius for
     * roundabouts).
     */
    private static double[] findClearSpot(Lane target, int cx, int cy, Direction dir,
                                          double myLength, double startDist) {
        int laneOffsetX = dir.rightX() * Lane.LANE_HALF_WIDTH;
        int laneOffsetY = dir.rightY() * Lane.LANE_HALF_WIDTH;
        for (int step = 0; step < 20; step++) {
            double px = cx + dir.dx() * (startDist + step * 10.0) + laneOffsetX;
            double py = cy + dir.dy() * (startDist + step * 10.0) + laneOffsetY;
            boolean clear = true;
            for (Vehicle other : target.getVehicles()) {
                double minSep = (myLength + other.getLength()) / 2.0 + 5.0;
                if (Math.hypot(other.getX() - px, other.getY() - py) < minSep) { clear = false; break; }
            }
            if (clear) return new double[] { px, py };
        }
        return null;
    }

    private List<Vehicle> collectVehicles() {
        List<Vehicle> all = new ArrayList<>();
        for (Road road : network.getRoads())
            for (Lane lane : road.getLanes())
                all.addAll(lane.getVehicles());
        return all;
    }

    private void recordMetrics(List<Vehicle> vehicles) {
        speedHistory[historyHead] = Statistics.averageSpeed(vehicles);
        historyHead = (historyHead + 1) % speedHistory.length;
    }

    // -- lifecycle -----------------------------------------------------------

    public void run() {
        running = true;
        Thread t = new Thread(() -> {
            while (running) {
                long tickMs = 1000L / Math.max(1, tickRate);
                long start = System.currentTimeMillis();
                step();
                long sleep = tickMs - (System.currentTimeMillis() - start);
                if (sleep > 0) {
                    try { Thread.sleep(sleep); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                }
            }
        }, "sim-loop");
        t.setDaemon(true);
        t.start();
    }

    public void reset() {
        for (Road road : network.getRoads())
            for (Lane lane : road.getLanes()) lane.getVehicles().clear();
        for (Intersection ix : network.getIntersections())
            if (ix instanceof Roundabout r) r.getInRing().clear();
        tickCount = 0;
    }

    /** Manually spawn a single random vehicle — used by the SpawnCommand button. */
    public boolean forceSpawn() {
        int before = collectVehicles().size();
        spawner.tick(network);
        return collectVehicles().size() > before;
    }

    public void setPaused(boolean paused) { this.paused = paused; }
    public boolean isPaused() { return paused; }
    public void setTickRate(int hz) { this.tickRate = hz; }
    public int getTickRate() { return tickRate; }
    public long getTickCount() { return tickCount; }
    public RoadNetwork getNetwork() { return network; }
    public boolean isRunning() { return running; }
}
