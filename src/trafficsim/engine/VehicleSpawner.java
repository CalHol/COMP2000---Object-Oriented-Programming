package trafficsim.engine;

import trafficsim.util.Direction;
import trafficsim.model.road.Lane;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.strategy.RandomTurnStrategy;
import trafficsim.strategy.StraightPreferredTurnStrategy;
import trafficsim.strategy.TurnStrategy;
import trafficsim.strategy.WeightedRandom;
import trafficsim.model.vehicle.Bus;
import trafficsim.model.vehicle.Car;
import trafficsim.model.vehicle.DriverProfile;
import trafficsim.model.vehicle.EmergencyVehicle;
import trafficsim.model.vehicle.Truck;
import trafficsim.model.vehicle.Vehicle;

import java.awt.Rectangle;
import java.util.Random;
import java.util.function.Function;

/**
 * Keeps the network populated. Each tick, with probability
 * {@code spawnProbability}, drops one new vehicle onto an edge lane. Three
 * weighted mixes drive variety:
 * <ul>
 *   <li>Vehicle type — Car 70 / Truck 15 / Bus 10 / Emergency 5</li>
 *   <li>Driver profile — Normal 60 / Aggressive 25 / Cautious 15</li>
 *   <li>Turn strategy — Random 50 / StraightPreferred 50</li>
 * </ul>
 */
public class VehicleSpawner {

    private final Random rng;
    private final double spawnProbability;

    private final WeightedRandom<Function<SpawnContext, Vehicle>> vehicleMix = new WeightedRandom<>();
    private final WeightedRandom<DriverProfile> profileMix = new WeightedRandom<>();
    private final WeightedRandom<TurnStrategy> strategyMix = new WeightedRandom<>();

    public VehicleSpawner() {
        this(new Random(1337), 0.14);
    }

    public VehicleSpawner(Random rng, double spawnProbability) {
        this.rng = rng;
        this.spawnProbability = spawnProbability;

        // Big vehicles persist longer than short ones — weight their spawns lower
        // so the steady-state mix stays interesting.
        vehicleMix.add(ctx -> new Car(ctx.x, ctx.y, ctx.dir, ctx.strategy), 75);
        vehicleMix.add(ctx -> new Truck(ctx.x, ctx.y, ctx.dir, ctx.strategy, 2500 + rng.nextInt(3000)), 12);
        vehicleMix.add(ctx -> new Bus(ctx.x, ctx.y, ctx.dir, ctx.strategy), 6);
        vehicleMix.add(ctx -> new EmergencyVehicle(ctx.x, ctx.y, ctx.dir, ctx.strategy), 7);

        profileMix.add(DriverProfile.NORMAL, 60);
        profileMix.add(DriverProfile.AGGRESSIVE, 25);
        profileMix.add(DriverProfile.CAUTIOUS, 15);

        // Most drivers just want to get somewhere — favour the straight-preferred strategy.
        strategyMix.add(new RandomTurnStrategy(), 25);
        strategyMix.add(new StraightPreferredTurnStrategy(), 75);
    }

    public void tick(RoadNetwork network) {
        if (rng.nextDouble() > spawnProbability) return;

        Rectangle b = network.getBounds();
        for (int attempt = 0; attempt < 8; attempt++) {
            Road road = network.getRoads().get(rng.nextInt(network.getRoads().size()));
            for (Lane lane : road.getLanes()) {
                Direction d = lane.getDirection();
                int[] entry = laneEntryPoint(road, d);
                if (!onBoundary(entry, b)) continue;
                if (laneIsCrowdedNearEntry(lane, entry)) continue;

                TurnStrategy strat = strategyMix.pick();
                Vehicle v = vehicleMix.pick().apply(new SpawnContext(entry[0], entry[1], d, strat));
                v.setDriverProfile(profileMix.pick());
                lane.addVehicle(v);
                return;
            }
        }
    }

    public void despawnOffMap(RoadNetwork network) {
        Rectangle b = network.getBounds();
        b.grow(10, 10);
        for (Road road : network.getRoads()) {
            for (Lane lane : road.getLanes()) {
                lane.getVehicles().removeIf(v -> !b.contains(v.getX(), v.getY()));
            }
        }
    }

    private static int[] laneEntryPoint(Road road, Direction dir) {
        int cx, cy;
        switch (dir) {
            case EAST  -> { cx = Math.min(road.getX1(), road.getX2()); cy = midY(road); }
            case WEST  -> { cx = Math.max(road.getX1(), road.getX2()); cy = midY(road); }
            case SOUTH -> { cx = midX(road); cy = Math.min(road.getY1(), road.getY2()); }
            case NORTH -> { cx = midX(road); cy = Math.max(road.getY1(), road.getY2()); }
            default    -> throw new IllegalStateException();
        }
        // Offset perpendicular-right of travel direction so we spawn onto our lane centreline.
        return new int[] {
                cx + dir.rightX() * trafficsim.model.road.Lane.LANE_HALF_WIDTH,
                cy + dir.rightY() * trafficsim.model.road.Lane.LANE_HALF_WIDTH
        };
    }

    private static int midX(Road r) { return (r.getX1() + r.getX2()) / 2; }
    private static int midY(Road r) { return (r.getY1() + r.getY2()) / 2; }

    private static boolean onBoundary(int[] p, Rectangle b) {
        int slack = 2;
        return Math.abs(p[0] - b.x) <= slack
            || Math.abs(p[0] - (b.x + b.width)) <= slack
            || Math.abs(p[1] - b.y) <= slack
            || Math.abs(p[1] - (b.y + b.height)) <= slack;
    }

    private static boolean laneIsCrowdedNearEntry(Lane lane, int[] entry) {
        // 32 = enough for the longest vehicle (Bus = 24) plus MIN_GAP + half-length buffer.
        for (Vehicle v : lane.getVehicles()) {
            if (Math.hypot(v.getX() - entry[0], v.getY() - entry[1]) < 32) return true;
        }
        return false;
    }

    private record SpawnContext(double x, double y, Direction dir, TurnStrategy strategy) {}
}
