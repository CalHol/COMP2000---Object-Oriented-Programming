package trafficsim.engine;

import trafficsim.model.road.Road;
import trafficsim.model.vehicle.Vehicle;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Statistics {

    private Statistics() {}

    public static double averageSpeed(List<Vehicle> vehicles) {
        return vehicles.stream()
                .mapToDouble(Vehicle::getSpeed)
                .average()
                .orElse(0.0);
    }

    public static long stoppedCount(List<Vehicle> vehicles) {
        return vehicles.stream()
                .filter(v -> v.getSpeed() == 0.0)
                .count();
    }

    public static Map<Road, Long> congestionByRoad(Map<Road, List<Vehicle>> vehiclesByRoad) {
        return vehiclesByRoad.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (long) e.getValue().size()));
    }
}
