package trafficsim.model.vehicle;

import trafficsim.util.Direction;

public final class Truck extends Vehicle {

    public Truck(double x, double y, Direction direction, double cargoWeight) {
        super(x, y, 2.0, 28.0, direction);
        // Heavier cargo lowers acceleration, capped so a truck can still pull away.
        double penalty = Math.min(0.2, cargoWeight / 20000.0);
        this.accelStep = Math.max(0.05, 0.25 - penalty);
    }

}
