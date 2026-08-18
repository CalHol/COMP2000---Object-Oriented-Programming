package trafficsim.engine;

import trafficsim.model.road.Intersection;
import trafficsim.model.road.Lane;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.vehicle.Vehicle;
import java.util.ArrayList;
import java.util.List;

// TODO: [Pair A] — Henry + TBD
public class SimulationEngine {
    private RoadNetwork network;
    private List<SimulationObserver> observers = new ArrayList<>();
    private int tickRate;
    private boolean running;

    public SimulationEngine(RoadNetwork network, int tickRate) {
        this.network = network;
        this.tickRate = tickRate;
        this.running = false;
    }

    public void step() {
        // Phase 1: update all intersections (traffic lights)
        for (Intersection i : network.getIntersections()) {
            i.update();
        }
        // Phase 2: move all vehicles
        for (var road : network.getRoads()) {
            for (Lane lane : road.getLanes()) {
                for (Vehicle v : lane.getVehicles()) {
                    v.move();
                }
            }
        }
        notifyObservers();
    }

    public void run() {
        running = true;
        // TODO: run step() in a loop at tickRate using a Thread or Timer
    }

    public void reset() {
        running = false;
        // TODO: clear vehicles and reset state
    }

    public void addObserver(SimulationObserver observer) {
        observers.add(observer);
    }

    public void notifyObservers() {
        for (SimulationObserver o : observers) {
            o.onSimulationStep();
        }
    }

    public RoadNetwork getNetwork() { return network; }
    public boolean isRunning() { return running; }
}
