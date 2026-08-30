package trafficsim.model.vehicle;

import trafficsim.strategy.TurnStrategy;
import trafficsim.util.Direction;

import java.util.List;

public abstract class Vehicle {
    private final double initialX;
    private final double initialY;
    private double x;
    private double y;
    private double speed;
    private final double maxSpeed;
    private Direction direction;
    private final TurnStrategy turnStrategy;
    private double distanceTravelled;

    public Vehicle(int x, int y, double maxSpeed, Direction direction, TurnStrategy turnStrategy) {
        if (maxSpeed <= 0.0 || !Double.isFinite(maxSpeed)) {
            throw new IllegalArgumentException("Maximum speed must be positive and finite");
        }
        if (direction == null || turnStrategy == null) {
            throw new IllegalArgumentException("Direction and turn strategy are required");
        }
        this.initialX = x;
        this.initialY = y;
        this.x = x;
        this.y = y;
        this.maxSpeed = maxSpeed;
        this.speed = 0;
        this.direction = direction;
        this.turnStrategy = turnStrategy;
    }

    public abstract void move();

    /**
     * Overloaded movement operation used by the engine when traffic requires a
     * vehicle to travel below its maximum speed.
     */
    public void move(double desiredSpeed) {
        double target = Math.max(0.0, Math.min(desiredSpeed, maxSpeed));
        double acceleration = getAcceleration();
        if (speed < target) {
            speed = Math.min(target, speed + acceleration);
        } else if (speed > target) {
            speed = Math.max(target, speed - getBrakingRate());
        }
        advance();
    }

    public void brake() {
        speed = Math.max(0.0, speed - getBrakingRate());
    }

    protected Direction chooseTurn(List<Direction> available) {
        return turnStrategy.chooseDirection(available);
    }

    public boolean stopsAtRedLight() {
        return true;
    }

    protected double getAcceleration() {
        return 0.16;
    }

    protected double getBrakingRate() {
        return 0.38;
    }

    protected final void advance() {
        double travelledX = 0.0;
        double travelledY = 0.0;
        switch (direction) {
            case NORTH:
                travelledY = -speed;
                break;
            case SOUTH:
                travelledY = speed;
                break;
            case EAST:
                travelledX = speed;
                break;
            case WEST:
                travelledX = -speed;
                break;
            default:
                throw new IllegalStateException("Unhandled direction " + direction);
        }
        x += travelledX;
        y += travelledY;
        distanceTravelled += Math.abs(travelledX) + Math.abs(travelledY);
    }

    public void reset() {
        x = initialX;
        y = initialY;
        speed = 0.0;
        distanceTravelled = 0.0;
    }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = Math.max(0.0, Math.min(speed, maxSpeed)); }
    public double getMaxSpeed() { return maxSpeed; }
    public int getX() { return (int) Math.round(x); }
    public int getY() { return (int) Math.round(y); }
    public double getPreciseX() { return x; }
    public double getPreciseY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setPosition(double x, double y) { this.x = x; this.y = y; }
    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) {
        if (direction == null) {
            throw new IllegalArgumentException("Direction cannot be null");
        }
        this.direction = direction;
    }
    public double getDistanceTravelled() { return distanceTravelled; }
}
