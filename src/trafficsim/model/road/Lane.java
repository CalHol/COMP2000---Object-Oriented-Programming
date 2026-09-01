package trafficsim.model.road;

import trafficsim.model.vehicle.Vehicle;
import trafficsim.util.Direction;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

// TODO: [Pair B] — Callum + JUBRIL
public class Lane {
    private final Direction direction;
    private final CopyOnWriteArrayList<Vehicle> vehicles =
            new CopyOnWriteArrayList<>();

    public Lane(Direction direction) {
        this.direction = Objects.requireNonNull(
                direction,
                "Lane direction cannot be null.");
    }

    public void addVehicle(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Vehicle cannot be null.");

        if (!vehicles.addIfAbsent(vehicle)) {
            throw new IllegalArgumentException(
                    "The same vehicle cannot be added to a lane twice.");
        }
    }

    public void removeVehicle(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Vehicle cannot be null.");
        vehicles.remove(vehicle);
    }

    public List<Vehicle> getVehicles() {
        return Collections.unmodifiableList(vehicles);
    }

    public Direction getDirection() {
        return direction;
    }
}