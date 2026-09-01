package trafficsim.model.vehicle;

import trafficsim.util.Direction;
import trafficsim.strategy.TurnStrategy;

public final class Car extends Vehicle {

    public Car(double x, double y, Direction direction, TurnStrategy turnStrategy) {
        super(x, y, 6.0, 18.0, direction, turnStrategy);
    }

    @Override
    public <R> R accept(VehicleVisitor<R> visitor) {
        return visitor.visitCar(this);
    }
}
