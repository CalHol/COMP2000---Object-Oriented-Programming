package trafficsim.engine;

import trafficsim.model.road.Lane;
import trafficsim.model.road.Road;
import trafficsim.model.vehicle.Vehicle;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// [Pair A] Henry
public class Statistics {

    /** Mean speed across every vehicle in the list, or 0 if the list is empty. */
    public double averageSpeed(List<Vehicle> vehicles) {
        return vehicles.stream()
                .mapToDouble(Vehicle::getSpeed)
                .average()
                .orElse(0.0);
    }

    /** Number of vehicles that are currently stopped (speed == 0). */
    public long stoppedCount(List<Vehicle> vehicles) {
        return vehicles.stream()
                .filter(v -> v.getSpeed() == 0)
                .count();
    }

    /** Vehicle count per road, summed across all its lanes. */
    public Map<Road, Long> congestionByRoad(List<Road> roads) {
        return roads.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        this::vehiclesOnRoad
                ));
    }

    /** Road with the most vehicles right now, or empty if roads is empty. */
    public java.util.Optional<Road> busiestRoad(List<Road> roads) {
        return roads.stream()
                .max(java.util.Comparator.comparingLong(this::vehiclesOnRoad));
    }

    private long vehiclesOnRoad(Road road) {
        return road.getLanes().stream()
                .mapToLong(lane -> lane.getVehicles().size())
                .sum();
    }
}
