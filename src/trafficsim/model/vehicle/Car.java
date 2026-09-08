package trafficsim.model.vehicle;

import trafficsim.util.Direction;

public final class Car extends Vehicle {

    public Car(double x, double y, Direction direction) {
        super(x, y, 3.0, 18.0, direction);
    }
}
