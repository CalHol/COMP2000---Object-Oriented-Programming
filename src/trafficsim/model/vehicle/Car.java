package trafficsim.model.vehicle;

import trafficsim.strategy.TurnStrategy;
import trafficsim.util.Direction;

public class Car extends Vehicle {
    public Car(int x, int y, double maxSpeed, Direction direction, TurnStrategy turnStrategy) {
        super(x, y, maxSpeed, direction, turnStrategy);
    }

    @Override
    public void move() {
        move(getMaxSpeed());
    }
}
