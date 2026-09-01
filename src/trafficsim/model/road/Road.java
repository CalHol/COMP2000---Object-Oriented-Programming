package trafficsim.model.road;

import java.util.ArrayList;
import java.util.List;

// TODO: [Pair B] — Callum + JUBRIL
public class Road {
    private int x1, y1, x2, y2;
    private int speedLimit;
    private List<Lane> lanes = new ArrayList<>();

    public Road(int x1, int y1, int x2, int y2, int speedLimit) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.speedLimit = speedLimit;
    }

    public void addLane(Lane lane) {
        lanes.add(lane);
    }

    public List<Lane> getLanes() { return lanes; }
    public int getSpeedLimit() { return speedLimit; }
    public int getX1() { return x1; }
    public int getY1() { return y1; }
    public int getX2() { return x2; }
    public int getY2() { return y2; }
}
