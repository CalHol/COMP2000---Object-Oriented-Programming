package trafficsim.model.road;

import trafficsim.model.light.TrafficLight;

import java.util.ArrayList;
import java.util.List;

/**
 * A node where roads meet. Sealed so callers can pattern-match exhaustively on
 * the two kinds ({@link SignalisedIntersection}, {@link Roundabout}) and the
 * compiler catches any missing branch.
 */
public abstract sealed class Intersection
        permits SignalisedIntersection, Roundabout {

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

    /** Advance any owned state (e.g. a traffic light). No-op for roundabouts. */
    public abstract void update();

    /** True if this intersection is signal-controlled (has a {@link TrafficLight}). */
    public abstract boolean hasSignal();

    /** Returns null for non-signalised intersections. */
    public TrafficLight getLight() { return null; }
}
