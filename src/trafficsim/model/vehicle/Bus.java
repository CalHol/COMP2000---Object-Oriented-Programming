package trafficsim.model.vehicle;

import trafficsim.model.road.BusStop;
import trafficsim.strategy.TurnStrategy;
import trafficsim.util.Direction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Bus extends Vehicle {
    private int passengerCount;
    private final List<BusStop> stops = new ArrayList<>();
    private BusStop lastServedStop;
    private int dwellingTicks;

    public Bus(int x, int y, double maxSpeed, Direction direction, TurnStrategy turnStrategy) {
        super(x, y, maxSpeed, direction, turnStrategy);
    }

    @Override
    public void move() {
        move(getMaxSpeed());
    }

    @Override
    public void move(double desiredSpeed) {
        if (dwellingTicks > 0) {
            dwellingTicks--;
            super.move(0.0);
            return;
        }

        if (lastServedStop != null && distanceTo(lastServedStop) > 45.0) {
            lastServedStop = null;
        }
        for (BusStop stop : stops) {
            if (stop != lastServedStop && distanceTo(stop) < 5.0) {
                lastServedStop = stop;
                dwellingTicks = 24;
                pickUpPassengers();
                super.move(0.0);
                return;
            }
        }
        super.move(desiredSpeed);
    }

    public void pickUpPassengers() {
        passengerCount += 1 + (getX() + getY()) % 5;
    }

    public int getPassengerCount() { return passengerCount; }
    public List<BusStop> getStops() { return Collections.unmodifiableList(stops); }
    public void addStop(BusStop stop) {
        if (stop == null) {
            throw new IllegalArgumentException("Bus stop cannot be null");
        }
        stops.add(stop);
    }

    @Override
    protected double getAcceleration() {
        return 0.11;
    }

    @Override
    public void reset() {
        super.reset();
        passengerCount = 0;
        lastServedStop = null;
        dwellingTicks = 0;
    }

    private double distanceTo(BusStop stop) {
        int[] position = stop.getPosition();
        return Math.hypot(getPreciseX() - position[0], getPreciseY() - position[1]);
    }
}
