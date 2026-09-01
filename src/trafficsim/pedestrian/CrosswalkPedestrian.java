package trafficsim.pedestrian;

import trafficsim.util.Axis;
import trafficsim.util.LightPhase;
import trafficsim.util.Direction;
import trafficsim.model.road.Intersection;

import java.awt.Color;

/**
 * A pedestrian that crosses a street at a specific intersection approach.
 * Only actually walks while the perpendicular vehicle axis is RED (i.e. their
 * "walk" phase). Otherwise stands still on the near-side sidewalk.
 *
 * <p>Non-signalised intersections (roundabouts) are skipped — pedestrians
 * don't cross those.
 */
public class CrosswalkPedestrian {

    private final Intersection intersection;
    /** Which axis the pedestrian is crossing — perpendicular to the road they walk over. */
    private final Axis crossingAxis;
    private final double speed;
    private final Color shirt;
    private final Color pants;
    private int direction; // ±1 — flipped when we reach either curb

    private double t; // progress from 0 (near side) to 1 (far side)
    private double x, y;

    public CrosswalkPedestrian(Intersection intersection, Axis crossingAxis, double startSide,
                               double speed, int direction, Color shirt, Color pants) {
        this.intersection = intersection;
        this.crossingAxis = crossingAxis;
        this.speed = speed;
        this.direction = direction;
        this.shirt = shirt;
        this.pants = pants;
        this.t = startSide;
        recomputePosition();
    }

    public void step() {
        // If we're already mid-cross, COMMIT to finishing regardless of light — real
        // pedestrians don't freeze in the middle of the road when the light changes.
        // Bump speed while crossing so we clear before the phase changes.
        if (isCrossing()) {
            t += direction * (speed * 3.0);
        } else if (canWalk()) {
            t += direction * speed;
        } else {
            return; // wait at the curb
        }
        if (t <= 0 || t >= 1) {
            direction = -direction;
            t = Math.max(0, Math.min(1, t));
        }
        recomputePosition();
    }

    private boolean canWalk() {
        if (!intersection.hasSignal()) return true; // no signal — always walk
        // We cross a road on the crossingAxis; the crossing is safe when that axis is RED.
        return intersection.getLight().phaseFor(crossingAxis) == LightPhase.RED;
    }

    /**
     * The crosswalk is a segment perpendicular to {@code crossingAxis},
     * spanning the road width, positioned just outside the intersection tile
     * on the far side of the approach.
     */
    private void recomputePosition() {
        int cx = intersection.getX();
        int cy = intersection.getY();
        int stripe = 32; // matches SimulationDisplay's crosswalk depth
        // Choose which side: we place on the "west/north" crosswalk arbitrarily.
        if (crossingAxis == Axis.HORIZONTAL) {
            // crossing a horizontal road → walking N/S. Stripe runs vertically on the west side.
            x = cx - stripe;
            y = cy - 18 + t * 36; // t=0 at north shoulder, t=1 at south shoulder
        } else {
            x = cx - 18 + t * 36;
            y = cy - stripe;
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public Color getShirt() { return shirt; }
    public Color getPants() { return pants; }
    public boolean isCrossing() { return t > 0.05 && t < 0.95; }
    public Intersection getIntersection() { return intersection; }
    public Axis getCrossingAxis() { return crossingAxis; }
}
