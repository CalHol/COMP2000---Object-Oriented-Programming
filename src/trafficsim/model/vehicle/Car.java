package trafficsim.model.vehicle;

import trafficsim.strategy.TurnStrategy;
import trafficsim.util.Direction;

// TODO: [Pair C] — Ben + Jacob
public class Car extends Vehicle {
    public Car(int x, int y, double maxSpeed, Direction direction, TurnStrategy turnStrategy) {
        super(x, y, maxSpeed, direction, turnStrategy);
    }

    @Override
    public void move() {
        // TODO: advance position by speed in current direction
    }
}
