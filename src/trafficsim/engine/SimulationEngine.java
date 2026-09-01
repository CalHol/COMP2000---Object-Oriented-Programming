package trafficsim.engine;

import trafficsim.model.road.Intersection;
import trafficsim.model.road.Lane;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.vehicle.Vehicle;
import java.util.ArrayList;
import java.util.List;

// [Pair A] Henry
public class SimulationEngine {
    private RoadNetwork network;
    private List<SimulationObserver> observers = new ArrayList<>();
    private int tickRate;
    private boolean running;
    private Thread loopThread;

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
        for (Road road : network.getRoads()) {
            for (Lane lane : road.getLanes()) {
                // copy to avoid ConcurrentModificationException if a vehicle
                // leaves/enters a lane during its own move()
                List<Vehicle> snapshot = new ArrayList<>(lane.getVehicles());
                for (Vehicle v : snapshot) {
                    v.move();
                }
            }
        }
        notifyObservers();
    }

    public void run() {
        if (running) return;
        running = true;

        // tickRate is ticks per second, so each step waits 1000/tickRate ms
        final long tickMs = 1000L / Math.max(1, tickRate);

        loopThread = new Thread(() -> {
            while (running) {
                long start = System.currentTimeMillis();
                step();
                long elapsed = System.currentTimeMillis() - start;
                long sleep = tickMs - elapsed;
                if (sleep > 0) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "sim-loop");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    public void stop() {
        running = false;
        if (loopThread != null) {
            loopThread.interrupt();
        }
    }

    public void reset() {
        stop();
        for (Road road : network.getRoads()) {
            for (Lane lane : road.getLanes()) {
                // remove all vehicles from every lane
                new ArrayList<>(lane.getVehicles())
                        .forEach(lane::removeVehicle);
            }
        }
        notifyObservers();
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
    public int getTickRate() { return tickRate; }
}
