package trafficsim.model.road;

import trafficsim.model.vehicle.Vehicle;
import trafficsim.util.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class Lane {
    /**
     * Half-width of a single lane in world units.
     */
    public static final int LANE_HALF_WIDTH = 9;

    private final Direction direction;
    private final List<Vehicle> vehicles = new ArrayList<>();

    private Road road;

    public Lane(Direction direction) {
        this.direction = Objects.requireNonNull(
                direction,
                "Lane direction cannot be null.");
    }

    public Direction getDirection() {
        return direction;
    }

    public void addVehicle(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Vehicle cannot be null.");

        if (vehicles.contains(vehicle)) {
            throw new IllegalArgumentException(
                    "The same vehicle cannot be added to a lane twice.");
        }

        vehicles.add(vehicle);
        vehicle.attachTo(this);
    }

    public void removeVehiclesIf(Predicate<Vehicle> condition) {
        vehicles.removeIf(Objects.requireNonNull(condition, "Condition cannot be null."));
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
