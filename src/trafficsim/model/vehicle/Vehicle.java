package trafficsim.model.vehicle;

import trafficsim.strategy.TurnStrategy;
import trafficsim.util.Direction;

// TODO: [Pair C] — Ben + Jacob
public abstract class Vehicle {
    private int x;
    private int y;
    private double speed;
    private double maxSpeed;
    private Direction direction;
    private TurnStrategy turnStrategy;

    public Vehicle(int x, int y, double maxSpeed, Direction direction, TurnStrategy turnStrategy) {
        this.x = x;
        this.y = y;
        this.maxSpeed = maxSpeed;
        this.speed = 0;
        this.direction = direction;
        this.turnStrategy = turnStrategy;
    }

    public abstract void move();

    public void brake() {
        // TODO: reduce speed toward 0
    }

    protected Direction chooseTurn(java.util.List<Direction> available) {
        return turnStrategy.chooseDirection(available);
    }

    protected boolean stopsAtRedLight() {
        return true;
    }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = Math.min(speed, maxSpeed); }
    public double getMaxSpeed() { return maxSpeed; }
    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }
}
