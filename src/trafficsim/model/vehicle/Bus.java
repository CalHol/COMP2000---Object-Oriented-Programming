package trafficsim.model.vehicle;

import trafficsim.model.road.BusStop;
import trafficsim.strategy.TurnStrategy;
import trafficsim.util.Direction;
import java.util.ArrayList;
import java.util.List;

// TODO: [Pair C] — Ben + Jacob
public class Bus extends Vehicle {
    private int passengerCount;
    private List<BusStop> stops = new ArrayList<>();

    public Bus(int x, int y, double maxSpeed, Direction direction, TurnStrategy turnStrategy) {
        super(x, y, maxSpeed, direction, turnStrategy);
    }

    @Override
    public void move() {
        // TODO: advance position; stop at BusStop positions
    }

    public void pickUpPassengers() {
        // TODO: increment passengerCount when at a BusStop
    }

    public int getPassengerCount() { return passengerCount; }
    public List<BusStop> getStops() { return stops; }
    public void addStop(BusStop stop) { stops.add(stop); }
}
