package trafficsim.engine;

import trafficsim.model.road.Road;
import trafficsim.model.vehicle.Vehicle;
import java.util.List;
import java.util.Map;

// TODO: [Pair A] — Henry + TBD
public class Statistics {

    public double averageSpeed(List<Vehicle> vehicles) {
        // TODO: use Stream to compute average speed
        return vehicles.stream()
                .mapToDouble(Vehicle::getSpeed)
                .average()
                .orElse(0.0);
    }

    public long stoppedCount(List<Vehicle> vehicles) {
        // TODO: use Stream to count vehicles with speed == 0
        return vehicles.stream()
                .filter(v -> v.getSpeed() == 0)
                .count();
    }

    public Map<Road, Long> congestionByRoad(List<Road> roads) {
        // TODO: use Stream to count vehicles per road
        return null;
    }
}
