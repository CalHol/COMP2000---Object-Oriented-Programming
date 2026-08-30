package trafficsim.model.vehicle;

import trafficsim.strategy.TurnStrategy;
import trafficsim.util.Direction;

public class EmergencyVehicle extends Vehicle {
    private boolean sirenOn;

    public EmergencyVehicle(int x, int y, double maxSpeed, Direction direction, TurnStrategy turnStrategy, boolean sirenOn) {
        super(x, y, maxSpeed, direction, turnStrategy);
        this.sirenOn = sirenOn;
    }

    @Override
    public void move() {
        move(getMaxSpeed());
    }

    @Override
    public boolean stopsAtRedLight() {
        return false;
    }

    @Override
    protected double getAcceleration() {
        return 0.28;
    }

    public boolean isSirenOn() { return sirenOn; }
    public void setSirenOn(boolean sirenOn) { this.sirenOn = sirenOn; }
}
