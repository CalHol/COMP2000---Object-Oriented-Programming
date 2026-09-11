package trafficsim.model.road;

import trafficsim.model.light.TrafficLight;

import java.util.ArrayList;
import java.util.List;

/**
 * A node where roads meet. The current simulation uses signalised
 * intersections only.
 */
public abstract sealed class Intersection
        permits SignalisedIntersection {

    protected final int x, y;
    protected final List<Road> connectedRoads = new ArrayList<>();

    protected Intersection(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public List<Road> getConnectedRoads() { return connectedRoads; }
    public void connect(Road r) { connectedRoads.add(r); }

    /** Advance the traffic light state. */
    public abstract void update();

    public abstract TrafficLight getLight();
}
