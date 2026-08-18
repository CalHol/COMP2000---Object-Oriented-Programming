package trafficsim.model.vehicle;

import trafficsim.strategy.TurnStrategy;
import trafficsim.util.Direction;

// TODO: [Pair C] — Ben + Jacob
public class Truck extends Vehicle {
    private double cargoWeight;

    public Truck(int x, int y, double maxSpeed, Direction direction, TurnStrategy turnStrategy, double cargoWeight) {
        super(x, y, maxSpeed, direction, turnStrategy);
        this.cargoWeight = cargoWeight;
    }

    @Override
    public void move() {
        // TODO: advance position; cargo weight should affect acceleration
    }

    public double getCargoWeight() { return cargoWeight; }
}
