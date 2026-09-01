package trafficsim.model.vehicle;

import trafficsim.engine.SensorReading;
import trafficsim.model.road.BusStop;
import trafficsim.util.Direction;
import trafficsim.strategy.TurnStrategy;

import java.util.ArrayList;
import java.util.List;

public final class Bus extends Vehicle {

    private int passengerCount;
    private final List<BusStop> stops = new ArrayList<>();
    private static final int CAPACITY = 40;
    private static final double STOP_RADIUS = 8.0;

    public Bus(double x, double y, Direction direction, TurnStrategy turnStrategy) {
        super(x, y, 5.0, 32.0, direction, turnStrategy);
    }

    @Override
    public void move(SensorReading r) {
        // Pause at any stop within STOP_RADIUS — even if nothing else blocks us.
        for (BusStop stop : stops) {
            int[] p = stop.getPosition();
            if (Math.hypot(p[0] - x, p[1] - y) < STOP_RADIUS) {
                pickUpPassengers();
                brake();
                return;
            }
        }
        super.move(r);
    }

    public void pickUpPassengers() {
        int pickup = Math.min(3, CAPACITY - passengerCount);
        passengerCount += Math.max(0, pickup);
    }

    public void addStop(BusStop stop) { stops.add(stop); }
    public int getPassengerCount() { return passengerCount; }
    public List<BusStop> getStops() { return stops; }

    @Override
    public <R> R accept(VehicleVisitor<R> visitor) {
        return visitor.visitBus(this);
    }
}
