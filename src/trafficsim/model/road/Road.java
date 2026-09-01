package trafficsim.model.road;

import trafficsim.util.Direction;

import java.util.ArrayList;
import java.util.List;

public class Road {

    private final int x1, y1, x2, y2;
    private final int speedLimit;
    private final List<Lane> lanes = new ArrayList<>();

    public Road(int x1, int y1, int x2, int y2, int speedLimit, List<Direction> laneDirections) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.speedLimit = speedLimit;
        for (Direction d : laneDirections) {
            Lane lane = new Lane(d);
            lane.setRoad(this);
            lanes.add(lane);
        }
    }

    public List<Lane> getLanes() { return lanes; }
    public int getSpeedLimit() { return speedLimit; }
    public int getX1() { return x1; }
    public int getY1() { return y1; }
    public int getX2() { return x2; }
    public int getY2() { return y2; }

    public boolean isHorizontal() { return y1 == y2; }
    public boolean isVertical() { return x1 == x2; }

    public int length() {
        int dx = x2 - x1;
        int dy = y2 - y1;
        return (int) Math.round(Math.sqrt(dx * dx + dy * dy));
    }

    /** Find the first lane whose direction matches {@code dir}, or {@code null}. */
    public Lane laneFor(Direction dir) {
        for (Lane lane : lanes) {
            if (lane.getDirection() == dir) return lane;
        }
        return null;
    }
}
