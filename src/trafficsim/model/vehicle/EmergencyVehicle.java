package trafficsim.model.vehicle;

import trafficsim.strategy.TurnStrategy;
import trafficsim.util.Direction;

// TODO: [Pair C] — Ben + Jacob
public class EmergencyVehicle extends Vehicle {
    private boolean sirenOn;

    public EmergencyVehicle(int x, int y, double maxSpeed, Direction direction, TurnStrategy turnStrategy, boolean sirenOn) {
        super(x, y, maxSpeed, direction, turnStrategy);
        this.sirenOn = sirenOn;
    }

    @Override
    public void move() {
        // TODO: advance at full speed regardless of traffic light
    }

    @Override
    protected boolean stopsAtRedLight() {
        return false;
    }

    public boolean isSirenOn() { return sirenOn; }
    public void setSirenOn(boolean sirenOn) { this.sirenOn = sirenOn; }
}
