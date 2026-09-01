package trafficsim.model.road;

import trafficsim.model.vehicle.Vehicle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Signal-free intersection where vehicles actually circulate CCW (as viewed
 * from above) around a central island. Vehicles that enter the ring leave
 * their lane and join {@link #getInRing()}; the engine ticks them along an
 * arc rather than in a straight line, then transfers them onto their chosen
 * exit lane.
 *
 * <p>Yield-to-circulating: a new vehicle may only enter if no in-ring vehicle
 * lies within ARC_ENTRY_CLEARANCE radians of the entry angle for that
 * approach — see {@link #entryIsClear(double)}.
 */
public final class Roundabout extends Intersection {

    private static final double ARC_ENTRY_CLEARANCE = trafficsim.SimConstants.RING_ENTRY_ARC_CLEARANCE;

    private final int outerRadius;
    private final int innerRadius;
    private final List<Vehicle> inRing = new CopyOnWriteArrayList<>();

    public Roundabout(int x, int y, int outerRadius, int innerRadius) {
        super(x, y);
        this.outerRadius = outerRadius;
        this.innerRadius = innerRadius;
    }

    public int getOuterRadius() { return outerRadius; }
    public int getInnerRadius() { return innerRadius; }

    public double midRadius() { return (outerRadius + innerRadius) / 2.0; }

    public List<Vehicle> getInRing() { return inRing; }
    public void addToRing(Vehicle v) { inRing.add(v); }
    public void removeFromRing(Vehicle v) { inRing.remove(v); }

    /** True if no in-ring vehicle is within ARC_ENTRY_CLEARANCE of the entry angle. */
    public boolean entryIsClear(double entryAngle) {
        for (Vehicle v : inRing) {
            double diff = normalizeAngleDelta(v.getRingAngle() - entryAngle);
            if (Math.abs(diff) < ARC_ENTRY_CLEARANCE) return false;
        }
        return true;
    }

    public static double normalizeAngleDelta(double d) {
        while (d > Math.PI) d -= 2 * Math.PI;
        while (d < -Math.PI) d += 2 * Math.PI;
        return d;
    }

    @Override
    public void update() {
        // no signal to advance — flow is self-regulating
    }

    @Override
    public boolean hasSignal() {
        return false;
    }
}
