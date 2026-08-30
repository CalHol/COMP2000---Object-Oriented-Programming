package trafficsim.model.vehicle;

import trafficsim.strategy.TurnStrategy;
import trafficsim.util.Direction;

public class Truck extends Vehicle {
    private double cargoWeight;

    public Truck(int x, int y, double maxSpeed, Direction direction, TurnStrategy turnStrategy, double cargoWeight) {
        super(x, y, maxSpeed, direction, turnStrategy);
        if (cargoWeight < 0.0 || !Double.isFinite(cargoWeight)) {
            throw new IllegalArgumentException("Cargo weight cannot be negative");
        }
        this.cargoWeight = cargoWeight;
    }

    @Override
    public void move() {
        move(getMaxSpeed());
    }

    @Override
    protected double getAcceleration() {
        return Math.max(0.05, 0.14 - cargoWeight / 100000.0);
    }

    public double getCargoWeight() { return cargoWeight; }
}
