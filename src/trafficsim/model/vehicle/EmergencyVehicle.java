package trafficsim.model.vehicle;

import trafficsim.util.Direction;
import trafficsim.strategy.TurnStrategy;

public final class EmergencyVehicle extends Vehicle {

    private boolean sirenOn;

    public EmergencyVehicle(double x, double y, Direction direction, TurnStrategy turnStrategy) {
        super(x, y, 9.0, 22.0, direction, turnStrategy);
        this.sirenOn = true;
        this.accelStep = 1.0;
        this.slowdownProbability = 0.0; // sirens beat physics
    }

    @Override
    protected boolean stopsAtRedLight() {
        return !sirenOn;
    }

    /** Emergency vehicles have zero random-slowdown chance regardless of profile. */
    @Override
    protected double effectiveSlowdownProbability() {
        return 0.0;
    }

    public boolean isSirenOn() { return sirenOn; }
    public void setSirenOn(boolean sirenOn) { this.sirenOn = sirenOn; }

    @Override
    public <R> R accept(VehicleVisitor<R> visitor) {
        return visitor.visitEmergency(this);
    }
}
