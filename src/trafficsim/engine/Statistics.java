package trafficsim.engine;

import trafficsim.model.vehicle.Vehicle;

import java.util.List;

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
}
