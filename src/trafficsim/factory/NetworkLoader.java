package trafficsim.factory;

import trafficsim.exception.InvalidNetworkException;
import trafficsim.util.Axis;
import trafficsim.model.light.TrafficLight;
import trafficsim.util.Direction;
import trafficsim.model.road.Intersection;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.road.SignalisedIntersection;
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
 * <li>{@link #buildDefault()} — hand-crafted 2 x 2 city-block grid,
 * formed by three east-west and three north-south roads with nine
 * junction locations, for the "just run it" demo.</li>
 * <li>{@link #loadFromFile(String)} — parse the text format documented in
 * {@code networks/grid.txt}.</li>
 * </ul>
 * <p>
 * Both entry points wire {@link Intersection}s to their {@link Road}s
 * geometrically: an intersection is "connected" to every road whose line passes
 * through the intersection's centre.
 */
public final class NetworkLoader {

    private NetworkLoader() {
    }

    public static RoadNetwork loadFromFile(String path) {
        if (path == null || path.isBlank()) {
            throw new InvalidNetworkException(
                    "Network file path cannot be null or blank.");
        }
        RoadNetwork network = new RoadNetwork();
        int intersectionIndex = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            int ln = 0;
            while ((line = br.readLine()) != null) {
                ln++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                String[] parts = line.split("\\s+");
                switch (parts[0]) {
                    case "ROAD" -> {
                        if (parts.length != 6) {
                            throw new InvalidNetworkException("line " + ln + ": ROAD expects 5 args");
                        }
                        List<Direction> dirs = Arrays.stream(parts[5].split(","))
                                .map(Direction::valueOf).toList();
                        network.addRoad(new Road(
                                Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                                Integer.parseInt(parts[3]), Integer.parseInt(parts[4]),
                                dirs));
                    }
                    case "INTERSECTION" -> {
                        if (parts.length != 6) {
                            throw new InvalidNetworkException("line " + ln + ": INTERSECTION expects 5 args");
                        }
                        // Alternate starting axes so neighbouring lights are staggered.
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
                    case "BUS_STOP" -> {
                        if (parts.length != 5) {
                            throw new InvalidNetworkException(
                                    "line " + ln + ": BUS_STOP expects 4 args");
                        }
                        BusStop stop = new BusStop(
                                Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[2]),
                                parts[4].replace('_', ' '),
                                Direction.valueOf(parts[3]));
                        if (network.getRoads().stream()
                                .flatMap(road -> road.getLanes().stream())
                                .noneMatch(stop::serves)) {
                            throw new InvalidNetworkException(
                                    "line " + ln + ": bus stop is not on its direction's lane");
                        }
                        network.addBusStop(stop);
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

    /**
     * 3 EW × 3 NS grid with nine signalised intersections, sized for the
     * original 1100×700 display.
     */
    public static RoadNetwork buildDefault() {
        RoadNetwork network = new RoadNetwork();

        int[] ys = { 170, 350, 530 };
        int[] xs = { 250, 550, 850 };

        for (int y : ys)
            network.addRoad(new Road(55, y, 1045, y, List.of(Direction.EAST, Direction.WEST)));
        for (int x : xs)
            network.addRoad(new Road(x, 105, x, 625, List.of(Direction.NORTH, Direction.SOUTH)));

        int i = 0;
        int[][] timings = {
                { 50, 30, 3 }, { 40, 30, 3 }, { 55, 30, 3 },
                { 45, 30, 3 }, { 60, 30, 3 }, { 35, 30, 3 },
                { 50, 30, 3 }, { 45, 30, 3 }, { 55, 30, 3 },
        };
        for (int y : ys) {
            for (int x : xs) {
                int[] t = timings[i];
                Axis start = (i % 2 == 0) ? Axis.HORIZONTAL : Axis.VERTICAL;
                TrafficLight light = new TrafficLight(t[0], t[1], t[2], start);
                network.addIntersection(new SignalisedIntersection(x, y, light));
                i++;
            }
        }

        wireIntersections(network);
        addDefaultBusStops(network);
        seedInitialVehicles(network);
        return network;
    }

    private static void addDefaultBusStops(RoadNetwork network) {
        network.addBusStop(new BusStop(430, 341, "Central Station", Direction.EAST));
        network.addBusStop(new BusStop(700, 179, "Museum", Direction.WEST));
        network.addBusStop(new BusStop(559, 430, "University", Direction.SOUTH));
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

    private static void seedInitialVehicles(RoadNetwork network) {
        Road ew1 = network.getRoads().get(0); // y=170
        Road ew2 = network.getRoads().get(1); // y=350
        Road ns1 = network.getRoads().get(3); // x=250
        Road ns2 = network.getRoads().get(4); // x=550

        seed(ew1.laneFor(Direction.EAST), 80, 170, Direction.EAST, "car");
        seed(ew1.laneFor(Direction.EAST), 150, 170, Direction.EAST, "truck");
        seed(ew2.laneFor(Direction.WEST), 1020, 350, Direction.WEST, "car");

        double[] busPos = ew2.laneFor(Direction.EAST).snapToLaneCentre(80, 350);
        Bus bus = new Bus(busPos[0], busPos[1], Direction.EAST);
        for (BusStop stop : network.getBusStops()) {
            if (stop.serves(ew2.laneFor(Direction.EAST))) bus.addStop(stop);
        }
        ew2.laneFor(Direction.EAST).addVehicle(bus);

        seed(ns1.laneFor(Direction.SOUTH), 250, 120, Direction.SOUTH, "emergency");
        seed(ns2.laneFor(Direction.NORTH), 550, 600, Direction.NORTH, "car");
    }

    private static void seed(trafficsim.model.road.Lane lane, int cx, int cy,
            Direction dir, String kind) {
        double[] pos = lane.snapToLaneCentre(cx, cy);
        var v = switch (kind) {
            case "truck" -> new Truck(pos[0], pos[1], dir, 3500);
            case "emergency" -> new EmergencyVehicle(pos[0], pos[1], dir);
            default -> new Car(pos[0], pos[1], dir);
        };
        lane.addVehicle(v);
    }
}
