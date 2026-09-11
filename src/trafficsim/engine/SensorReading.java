package trafficsim.engine;

/**
 * Immutable snapshot of what a vehicle can "see" for the current tick.
 * Computed in the read-only sensor phase of {@link SimulationEngine#step()}
 * and consumed by {@code Vehicle.move(SensorReading)}.
 *
 * <p>Distances are in world units along the vehicle's direction of travel;
 * {@link Double#POSITIVE_INFINITY} means "no obstacle within sight".
 */
public record SensorReading(
        double distToVehicleAhead,
        double distToRedStop) {

    public static SensorReading clear() {
        return new SensorReading(
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY);
    }

    public double effectiveStopDistance() {
        return Math.min(distToVehicleAhead, distToRedStop);
    }
}
