package trafficsim.engine;

import trafficsim.util.Direction;
import trafficsim.model.road.Intersection;
import trafficsim.model.road.Lane;
import trafficsim.model.road.Road;

/**
 * Immutable snapshot of what a vehicle can "see" for the current tick.
 * Computed in the read-only sensor phase of {@link SimulationEngine#step()}
 * and consumed by {@code Vehicle.move(SensorReading)}.
 *
 * <p>Distances are in world units along the vehicle's direction of travel;
 * {@link Double#POSITIVE_INFINITY} means "no obstacle within sight".
 * {@code currentIntersection} is non-null iff {@code atIntersection} is true.
 */
public record SensorReading(
        double distToVehicleAhead,
        double distToRedStop,
        Road currentRoad,
        Lane currentLane,
        Direction directionOfTravel,
        boolean atIntersection,
        Intersection currentIntersection) {

    public static SensorReading clear(Road road, Lane lane, Direction dir) {
        return new SensorReading(
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                road, lane, dir, false, null);
    }

    public double effectiveStopDistance() {
        return Math.min(distToVehicleAhead, distToRedStop);
    }
}
