package trafficsim.model.road;

import trafficsim.SimConstants;
import trafficsim.util.Axis;
import trafficsim.util.LightPhase;
import trafficsim.model.light.TrafficLight;
import trafficsim.model.vehicle.Vehicle;

/**
 * 4-way stop-and-go intersection controlled by a {@link TrafficLight}, with
 * light-touch adaptive green extension: if a burst of vehicles is queued on
 * the currently-green axis, extend the green phase a few ticks before yellow.
 */
public final class SignalisedIntersection extends Intersection {

    private final TrafficLight light;
    private int extensionsThisPhase = 0;

    public SignalisedIntersection(int x, int y, TrafficLight light) {
        super(x, y);
        this.light = light;
    }

    @Override
    public void update() {
        light.update();
        maybeExtendGreen();
    }

    @Override
    public boolean hasSignal() {
        return true;
    }

    @Override
    public TrafficLight getLight() {
        return light;
    }

    /**
     * If we're near the end of a green phase and there's a busy queue on that
     * axis, reset the timer to buy a few extra ticks. Capped by
     * {@link SimConstants#ADAPTIVE_MAX_EXTENSIONS} per phase so the perpendicular
     * axis eventually gets its turn.
     */
    private void maybeExtendGreen() {
        if (light.getPhase() != LightPhase.GREEN) {
            extensionsThisPhase = 0;
            return;
        }
        int remaining = light.getGreenDuration() - light.getTimer();
        if (remaining > 4) return; // only extend near end of phase
        if (extensionsThisPhase >= SimConstants.ADAPTIVE_MAX_EXTENSIONS) return;
        int queued = queuedOnGreenAxis();
        if (queued < SimConstants.ADAPTIVE_QUEUE_HIGH) return;
        // Rewind the timer by the extension length so the state machine sees "still fresh green".
        light.extendCurrentPhase(SimConstants.ADAPTIVE_EXTEND_TICKS);
        extensionsThisPhase++;
    }

    /** Count vehicles queued on connected roads whose direction matches the green axis. */
    private int queuedOnGreenAxis() {
        Axis green = light.getGreenAxis();
        int count = 0;
        for (Road road : connectedRoads) {
            for (Lane lane : road.getLanes()) {
                if (Axis.of(lane.getDirection()) != green) continue;
                for (Vehicle v : lane.getVehicles()) {
                    if (v.getSpeed() > 0.5) continue;
                    double dx = v.getX() - x, dy = v.getY() - y;
                    if (Math.hypot(dx, dy) < 180) count++; // scaled for enlarged map
                }
            }
        }
        return count;
    }
}
