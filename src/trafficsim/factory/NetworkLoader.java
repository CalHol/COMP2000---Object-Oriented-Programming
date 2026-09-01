package trafficsim.factory;

import trafficsim.exception.InvalidNetworkException;
import trafficsim.util.Axis;
import trafficsim.model.light.TrafficLight;
import trafficsim.util.Direction;
import trafficsim.model.road.Intersection;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.road.Roundabout;
import trafficsim.model.road.SignalisedIntersection;
import trafficsim.strategy.RandomTurnStrategy;
import trafficsim.strategy.TurnStrategy;
import trafficsim.model.vehicle.Bus;
import trafficsim.model.road.BusStop;
import trafficsim.model.vehicle.Car;
import trafficsim.model.vehicle.EmergencyVehicle;
import trafficsim.model.vehicle.Truck;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Factory for {@link RoadNetwork} instances. Two entry points:
 * <ul>
 *   <li>{@link #buildDefault()} — hand-crafted 2×2 city block grid for the
 *       "just run it" demo.</li>
 *   <li>{@link #loadFromFile(String)} — parse the text format documented in
 *       {@code networks/grid.txt}.</li>
 * </ul>
 * <p>Both entry points wire {@link Intersection}s to their {@link Road}s
 * geometrically: an intersection is "connected" to every road whose line passes
 * through the intersection's centre.
 */
public final class NetworkLoader {

    private NetworkLoader() {}

    public static RoadNetwork loadFromFile(String path) {
        RoadNetwork network = new RoadNetwork();
        int intersectionIndex = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            int ln = 0;
            while ((line = br.readLine()) != null) {
                ln++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");
                switch (parts[0]) {
                    case "ROAD" -> {
                        if (parts.length != 7) {
                            throw new InvalidNetworkException("line " + ln + ": ROAD expects 6 args");
                        }
                        List<Direction> dirs = Arrays.stream(parts[6].split(","))
                                .map(Direction::valueOf).toList();
                        network.addRoad(new Road(
                                Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                                Integer.parseInt(parts[3]), Integer.parseInt(parts[4]),
                                Integer.parseInt(parts[5]), dirs));
                    }
                    case "INTERSECTION" -> {
                        if (parts.length != 6) {
                            throw new InvalidNetworkException("line " + ln + ": INTERSECTION expects 5 args");
                        }
                        Axis start = (intersectionIndex++ % 2 == 0) ? Axis.HORIZONTAL : Axis.VERTICAL;
                        TrafficLight light = new TrafficLight(
                                Integer.parseInt(parts[3]),
                                Integer.parseInt(parts[4]),
                                Integer.parseInt(parts[5]),
                                start);
                        network.addIntersection(new SignalisedIntersection(
                                Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[2]),
                                light));
                    }
                    case "ROUNDABOUT" -> {
                        if (parts.length != 5) {
                            throw new InvalidNetworkException("line " + ln + ": ROUNDABOUT expects 4 args (x y outerR innerR)");
                        }
                        intersectionIndex++;
                        network.addIntersection(new Roundabout(
                                Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[2]),
                                Integer.parseInt(parts[3]),
                                Integer.parseInt(parts[4])));
                    }
                    default -> throw new InvalidNetworkException("line " + ln + ": unknown token " + parts[0]);
                }
            }
        } catch (IOException e) {
            throw new InvalidNetworkException("cannot read " + path, e);
        } catch (IllegalArgumentException e) {
            throw new InvalidNetworkException("bad enum/number: " + e.getMessage(), e);
        }
        if (network.getRoads().isEmpty()) {
            throw new InvalidNetworkException("network has no roads");
        }
        wireIntersections(network);
        return network;
    }

    /** 3 EW × 3 NS grid → 9 nodes (8 signalised + 1 centre roundabout), sized for a 1300×1000 map. */
    public static RoadNetwork buildDefault() {
        RoadNetwork network = new RoadNetwork();
        TurnStrategy strategy = new RandomTurnStrategy();

        int[] ys = {180, 500, 820};
        int[] xs = {250, 650, 1050};

        for (int y : ys) network.addRoad(new Road(60, y, 1240, y, 60, List.of(Direction.EAST, Direction.WEST)));
        for (int x : xs) network.addRoad(new Road(x, 60, x, 940, 60, List.of(Direction.NORTH, Direction.SOUTH)));

        int i = 0;
        int[][] timings = {
                {50, 6, 3}, {40, 6, 3}, {55, 6, 3},
                {45, 6, 3}, {60, 6, 3}, {35, 6, 3},
                {50, 6, 3}, {45, 6, 3}, {55, 6, 3},
        };
        for (int y : ys) {
            for (int x : xs) {
                // Centre intersection becomes a ROUNDABOUT — the demo now shows both kinds.
                if (x == 650 && y == 500) {
                    // Big central roundabout — outer 72, inner 20; lanes offset ±9 so the ring midR (46) is well clear.
                    network.addIntersection(new Roundabout(x, y, 72, 20));
                } else {
                    int[] t = timings[i];
                    Axis start = (i % 2 == 0) ? Axis.HORIZONTAL : Axis.VERTICAL;
                    TrafficLight light = new TrafficLight(t[0], t[1], t[2], start);
                    network.addIntersection(new SignalisedIntersection(x, y, light));
                }
                i++;
            }
        }

        wireIntersections(network);
        seedInitialVehicles(network, strategy);
        return network;
    }

    /** Connect each intersection to every road whose line passes through it. */
    private static void wireIntersections(RoadNetwork network) {
        for (Intersection ix : network.getIntersections()) {
            for (Road r : network.getRoads()) {
                if (r.isHorizontal() && r.getY1() == ix.getY()
                        && Math.min(r.getX1(), r.getX2()) <= ix.getX()
                        && ix.getX() <= Math.max(r.getX1(), r.getX2())) {
                    ix.connect(r);
                } else if (r.isVertical() && r.getX1() == ix.getX()
                        && Math.min(r.getY1(), r.getY2()) <= ix.getY()
                        && ix.getY() <= Math.max(r.getY1(), r.getY2())) {
                    ix.connect(r);
                }
            }
        }
    }

    private static void seedInitialVehicles(RoadNetwork network, TurnStrategy strategy) {
        Road ew1 = network.getRoads().get(0); // y=180
        Road ew2 = network.getRoads().get(1); // y=500
        Road ns1 = network.getRoads().get(3); // x=250
        Road ns2 = network.getRoads().get(4); // x=650

        seed(ew1.laneFor(Direction.EAST), 80,   180, Direction.EAST,  strategy, "car");
        seed(ew1.laneFor(Direction.EAST), 150,  180, Direction.EAST,  strategy, "truck");
        seed(ew2.laneFor(Direction.WEST), 1220, 500, Direction.WEST,  strategy, "car");

        double[] busPos = ew2.laneFor(Direction.EAST).snapToLaneCentre(80, 500);
        Bus bus = new Bus(busPos[0], busPos[1], Direction.EAST, strategy);
        double[] stopPos = ew2.laneFor(Direction.EAST).snapToLaneCentre(430, 500);
        bus.addStop(new BusStop((int) stopPos[0], (int) stopPos[1], "Central"));
        ew2.laneFor(Direction.EAST).addVehicle(bus);

        seed(ns1.laneFor(Direction.SOUTH), 250, 80,  Direction.SOUTH, strategy, "emergency");
        seed(ns2.laneFor(Direction.NORTH), 650, 920, Direction.NORTH, strategy, "car");
    }

    private static void seed(trafficsim.model.road.Lane lane, int cx, int cy,
                             Direction dir, TurnStrategy strategy, String kind) {
        double[] pos = lane.snapToLaneCentre(cx, cy);
        var v = switch (kind) {
            case "truck"     -> new Truck(pos[0], pos[1], dir, strategy, 3500);
            case "emergency" -> new EmergencyVehicle(pos[0], pos[1], dir, strategy);
            default          -> new Car(pos[0], pos[1], dir, strategy);
        };
        lane.addVehicle(v);
    }
}
