package trafficsim.model.road;

import trafficsim.model.vehicle.Vehicle;
import trafficsim.util.Direction;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Lane {

    /**
     * Half-width of a single lane in world units. A road with EAST + WEST lanes
     * is 2 × LANE_HALF_WIDTH wide of asphalt; vehicles are offset by this amount
     * perpendicular-right of the road centreline for right-hand-drive traffic.
     */
    public static final int LANE_HALF_WIDTH = 9;

    private final Direction direction;
    private final List<Vehicle> vehicles = new CopyOnWriteArrayList<>();
    private Road road;

    public Lane(Direction direction) {
        this.direction = direction;
    }

    /**
     * Given a point on the road centreline, return the corresponding point in
     * <em>this</em> lane's centre. Used by callers that know the centreline
     * position (spawner, seed, turn transfer) so vehicles never overlap the
     * dashed yellow line.
     */
    public double[] snapToLaneCentre(double roadCentreX, double roadCentreY) {
        return new double[] {
                roadCentreX + direction.rightX() * LANE_HALF_WIDTH,
                roadCentreY + direction.rightY() * LANE_HALF_WIDTH
        };
    }

    public Direction getDirection() {
        return direction;
    }

    public void addVehicle(Vehicle v) {
        vehicles.add(v);
        v.attachTo(this);
    }

    public void removeVehicle(Vehicle v) {
        vehicles.remove(v);
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    void setRoad(Road road) {
        this.road = road;
    }

    public Road getRoad() {
        return road;
    }
}
