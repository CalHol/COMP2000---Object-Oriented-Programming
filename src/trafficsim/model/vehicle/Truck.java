package trafficsim.model.vehicle;

import trafficsim.util.Direction;
import trafficsim.strategy.TurnStrategy;

public final class Truck extends Vehicle {

    private final double cargoWeight;

    public Truck(double x, double y, Direction direction, TurnStrategy turnStrategy, double cargoWeight) {
        super(x, y, 4.0, 28.0, direction, turnStrategy);
        this.cargoWeight = cargoWeight;
        double penalty = Math.min(0.4, cargoWeight / 10000.0);
        this.accelStep = Math.max(0.1, 0.5 - penalty);
    }

    public double getCargoWeight() { return cargoWeight; }

    @Override
    public <R> R accept(VehicleVisitor<R> visitor) {
        return visitor.visitTruck(this);
    }
}
