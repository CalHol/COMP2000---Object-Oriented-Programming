package trafficsim.model.road;

import trafficsim.util.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Road {

    private final int x1, y1, x2, y2;
    private final List<Lane> lanes = new ArrayList<>();

    public Road(int x1, int y1, int x2, int y2, List<Direction> laneDirections) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        for (Direction d : laneDirections) {
            Lane lane = new Lane(d);
            lane.setRoad(this);
            lanes.add(lane);
        }
    }

    public List<Lane> getLanes() { return Collections.unmodifiableList(lanes); }
    public int getX1() { return x1; }
    public int getY1() { return y1; }
    public int getX2() { return x2; }
    public int getY2() { return y2; }

    public boolean isHorizontal() { return y1 == y2; }
    public boolean isVertical() { return x1 == x2; }

}
