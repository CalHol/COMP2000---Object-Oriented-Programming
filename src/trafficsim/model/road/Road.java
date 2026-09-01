package trafficsim.model.road;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// Pair B — Callum + Jubril
public class Road {
    private final int x1;
    private final int y1;
    private final int x2;
    private final int y2;
    private final int speedLimit;
    private final List<Lane> lanes;

    public Road(int x1, int y1, int x2, int y2, int speedLimit) {
        if (x1 == x2 && y1 == y2) {
            throw new IllegalArgumentException(
                    "A road must have different start and end points.");
        }

        if (speedLimit <= 0) {
            throw new IllegalArgumentException(
                    "Speed limit must be greater than zero.");
        }

        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.speedLimit = speedLimit;
        this.lanes = new ArrayList<>();
    }

    public void addLane(Lane lane) {
        Objects.requireNonNull(lane, "Lane cannot be null.");

        if (lanes.contains(lane)) {
            throw new IllegalArgumentException(
                    "The same lane cannot be added to a road twice.");
        }

        lanes.add(lane);
    }

    public List<Lane> getLanes() {
        return Collections.unmodifiableList(lanes);
    }

    public int getSpeedLimit() {
        return speedLimit;
    }

    public int getX1() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }
}