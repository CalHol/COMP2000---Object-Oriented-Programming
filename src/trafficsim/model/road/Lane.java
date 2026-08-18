package trafficsim.model.road;

import trafficsim.model.vehicle.Vehicle;
import trafficsim.util.Direction;
import java.util.ArrayList;
import java.util.List;

// TODO: [Pair B] — Callum + TBD
public class Lane {
    private Direction direction;
    private List<Vehicle> vehicles = new ArrayList<>();

    public Lane(Direction direction) {
        this.direction = direction;
    }

    public void addVehicle(Vehicle vehicle) {
        vehicles.add(vehicle);
    }

    public void removeVehicle(Vehicle vehicle) {
        vehicles.remove(vehicle);
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public Direction getDirection() {
        return direction;
    }
}
