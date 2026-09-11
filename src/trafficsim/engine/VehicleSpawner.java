package trafficsim.engine;

import trafficsim.SimConstants;
import trafficsim.util.Direction;
import trafficsim.model.road.Lane;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.strategy.WeightedRandom;
import trafficsim.model.vehicle.Bus;
import trafficsim.model.vehicle.Car;
import trafficsim.model.vehicle.DriverProfile;
import trafficsim.model.vehicle.EmergencyVehicle;
import trafficsim.model.vehicle.Truck;
import trafficsim.model.vehicle.Vehicle;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

/**
 * Keeps the network populated. Each tick, with probability
 * {@code spawnProbability}, drops one new vehicle onto an edge lane. Three
 * weighted mixes drive variety:
 * <ul>
 *   <li>Vehicle type — Car 75 / Truck 12 / Bus 6 / Emergency 7</li>
 *   <li>Driver profile — Normal 60 / Aggressive 25 / Cautious 15</li>
 * </ul>
 */
public class VehicleSpawner {

    private final Random rng;
    private final double spawnProbability;

    private final WeightedRandom<Function<SpawnContext, Vehicle>> vehicleMix;
    private final WeightedRandom<DriverProfile> profileMix;

    public VehicleSpawner(Random rng, double spawnProbability) {
        this.rng = Objects.requireNonNull(rng, "Random source cannot be null");
        if (spawnProbability < 0.0 || spawnProbability > 1.0) {
            throw new IllegalArgumentException("Spawn probability must be between 0 and 1");
        }
        this.spawnProbability = spawnProbability;
        this.vehicleMix = new WeightedRandom<>(rng);
        this.profileMix = new WeightedRandom<>(rng);

        // Big vehicles persist longer than short ones — weight their spawns lower
        // so the steady-state mix stays interesting.
        vehicleMix.add(ctx -> new Car(ctx.x, ctx.y, ctx.dir), 75);
        vehicleMix.add(ctx -> new Truck(ctx.x, ctx.y, ctx.dir, 2500 + rng.nextInt(3000)), 12);
        vehicleMix.add(ctx -> new Bus(ctx.x, ctx.y, ctx.dir), 6);
        vehicleMix.add(ctx -> new EmergencyVehicle(ctx.x, ctx.y, ctx.dir), 7);

        profileMix.add(DriverProfile.NORMAL, 60);
        profileMix.add(DriverProfile.AGGRESSIVE, 25);
        profileMix.add(DriverProfile.CAUTIOUS, 15);

    }

    public void tick(RoadNetwork network) {
        // A new roll every tick creates irregular arrivals instead of a fixed interval.
        if (rng.nextDouble() > spawnProbability) return;
        spawnOne(network);
    }

    public boolean spawnOne(RoadNetwork network) {
        Rectangle b = network.getBounds();
        List<SpawnPoint> candidates = new ArrayList<>();
        for (Road road : network.getRoads()) {
            for (Lane lane : road.getLanes()) {
                Direction d = lane.getDirection();
                int[] entry = laneEntryPoint(road, d);
                if (onBoundary(entry, b)) candidates.add(new SpawnPoint(lane, entry));
            }
        }

        // Shuffle all boundary lanes so no road or direction is favoured.
        Collections.shuffle(candidates, rng);
        for (SpawnPoint candidate : candidates) {
            if (laneIsCrowdedNearEntry(candidate.lane, candidate.entry)) continue;
            Direction d = candidate.lane.getDirection();
            Vehicle v = vehicleMix.pick().apply(
                    new SpawnContext(candidate.entry[0], candidate.entry[1], d));
            v.setDriverProfile(profileMix.pick());
            if (v instanceof Bus bus) {
                for (var stop : network.getBusStops()) {
                    if (stop.serves(candidate.lane)) bus.addStop(stop);
                }
            }
            candidate.lane.addVehicle(v);
            return true;
        }
        return false;
    }

    public void despawnOffMap(RoadNetwork network) {
        Rectangle b = network.getBounds();
        b.grow(10, 10);
        for (Road road : network.getRoads()) {
            for (Lane lane : road.getLanes()) {
                lane.removeVehiclesIf(v -> !b.contains(v.getX(), v.getY()));
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
        // Australian traffic uses the left side of the road in the direction of travel.
        // Shift the road centre point onto the left-side lane for this direction.
        return new int[] {
                cx + dir.leftX() * Lane.LANE_HALF_WIDTH,
                cy + dir.leftY() * Lane.LANE_HALF_WIDTH
        };
    }

    private static int midX(Road r) { return (r.getX1() + r.getX2()) / 2; }
    private static int midY(Road r) { return (r.getY1() + r.getY2()) / 2; }

    private static boolean onBoundary(int[] p, Rectangle b) {
        int slack = 2; // permits small lane-centre offsets at the edge of the bounding box
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

    private record SpawnPoint(Lane lane, int[] entry) {}
    private record SpawnContext(double x, double y, Direction dir) {}
}
