package trafficsim.model.vehicle;

import trafficsim.util.Direction;

public final class EmergencyVehicle extends Vehicle {

    public EmergencyVehicle(double x, double y, Direction direction) {
        super(x, y, 4.5, 22.0, direction);
        this.accelStep = 0.5;
    }

    @Override
    protected boolean stopsAtRedLight() {
        return false;
    }

    /** Emergency vehicles have zero random-slowdown chance regardless of profile. */
    @Override
    protected double effectiveSlowdownProbability() {
        return 0.0;
    }

    /** Emergency vehicles in this simulation always operate with their siren on. */
    public boolean isSirenOn() { return true; }
}
