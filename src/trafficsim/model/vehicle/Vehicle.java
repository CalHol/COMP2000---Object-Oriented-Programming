package trafficsim.model.vehicle;

import trafficsim.engine.SensorReading;
import trafficsim.util.Direction;
import trafficsim.model.road.Lane;
import trafficsim.model.road.Road;

import java.util.Random;

public abstract sealed class Vehicle
        permits Car, Truck, Bus, EmergencyVehicle {

    protected double x, y;
    protected double speed;
    protected final double maxSpeed;
    protected final double length; // physical length in world units — sensor uses this
    protected final Direction direction;

    protected double accelStep = 0.25;
    private DriverProfile profile = DriverProfile.NORMAL;

    private Lane lane;

    private static Random noise = new Random();

    /** Allows a caller to make driver slowdown noise deterministic. */
    public static void seedNoise(long seed) { noise = new Random(seed); }

    protected Vehicle(double x, double y, double maxSpeed, double length,
                      Direction direction) {
        if (direction == null) {
            throw new IllegalArgumentException("Direction is required");
        }
        this.x = x;
        this.y = y;
        this.speed = 0;
        this.maxSpeed = maxSpeed;
        this.length = length;
        this.direction = direction;
    }

    /** Accelerate or brake according to the current sensor snapshot, then move straight. */
    public void move(SensorReading r) {
        // Keep one unit spare: a zero-distance reading means the vehicle must stop now.
        double obstacle = r.effectiveStopDistance() - 1.0;
        // The closest hazard limits speed, but a vehicle can never exceed its own maximum.
        double target = Math.min(maxSpeed, Math.max(0, obstacle));
        // Driver profiles change acceleration, not the physical safety distance.
        double effectiveAccel = accelStep * profile.accelMultiplier();

        if (target < speed) {
            speed = target;
        } else if (speed < target) {
            speed = Math.min(target, speed + effectiveAccel);
        }

        double p = effectiveSlowdownProbability();
        if (noise.nextDouble() < p && speed > 0) {
            speed = Math.max(0, speed - 0.12);
        }

        // Direction is fixed, so this is straight-line movement: position += direction vector × speed.
        x += direction.dx() * speed;
        y += direction.dy() * speed;
    }

    /** Subclasses may override to opt out of profile-driven slowdown (see EmergencyVehicle). */
    protected double effectiveSlowdownProbability() {
        return profile.slowdownProbability();
    }

    protected void brake() {
        speed = 0;
    }

    /** Default: cars, trucks and buses stop for red lights. Overridden by emergency vehicles. */
    protected boolean stopsAtRedLight() {
        return true;
    }
    public boolean respectsRedLight() { return stopsAtRedLight(); }

    // -- back-reference plumbing --------------------------------------------

    /** Called from {@code Lane.addVehicle} — do not call directly. */
    public void attachTo(Lane lane) { this.lane = lane; }
    public Lane getLane() { return lane; }
    public Road getRoad() { return lane == null ? null : lane.getRoad(); }

    // -- driver profile ------------------------------------------------------

    public void setDriverProfile(DriverProfile profile) {
        this.profile = profile == null ? DriverProfile.NORMAL : profile;
    }

    public DriverProfile getDriverProfile() { return profile; }

    // -- getters -------------------------------------------------------------

    public double getSpeed() { return speed; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getLength() { return length; }
    public Direction getDirection() { return direction; }
}

