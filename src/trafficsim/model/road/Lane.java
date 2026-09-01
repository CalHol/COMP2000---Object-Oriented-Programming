package trafficsim.model.road;

import trafficsim.model.vehicle.Vehicle;
import trafficsim.util.Direction;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class Lane {
    /**
     * Half-width of a single lane in world units.
     */
    public static final int LANE_HALF_WIDTH = 9;

    private final Direction direction;
    private final CopyOnWriteArrayList<Vehicle> vehicles =
            new CopyOnWriteArrayList<>();

    private Road road;

    public Lane(Direction direction) {
        this.direction = Objects.requireNonNull(
                direction,
                "Lane direction cannot be null.");
    }

    /**
     * Converts a position on the road centreline into a position
     * at the centre of this lane.
     */
    public double[] snapToLaneCentre(
            double roadCentreX,
            double roadCentreY) {

        return new double[] {
            roadCentreX
                    + direction.rightX() * LANE_HALF_WIDTH,
            roadCentreY
                    + direction.rightY() * LANE_HALF_WIDTH
        };
    }

    public Direction getDirection() {
        return direction;
    }

    public void addVehicle(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Vehicle cannot be null.");

        if (!vehicles.addIfAbsent(vehicle)) {
            throw new IllegalArgumentException(
                    "The same vehicle cannot be added to a lane twice.");
        }

        vehicle.attachTo(this);
    }

    public void removeVehicle(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Vehicle cannot be null.");
        vehicles.remove(vehicle);
    }

    public List<Vehicle> getVehicles() {
        return Collections.unmodifiableList(vehicles);
    }

    void setRoad(Road road) {
        this.road = Objects.requireNonNull(
                road,
                "Road cannot be null.");
    }

    public Road getRoad() {
        return road;
    }
}