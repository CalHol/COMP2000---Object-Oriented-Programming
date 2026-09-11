package trafficsim.model.light;

import trafficsim.util.LightPhase;

/**
 * All-red safety pause. On expiry, swap the green axis (so the perpendicular
 * direction gets its turn) and start a fresh GREEN.
 */
public class RedState implements LightState {
    @Override
    public void update(TrafficLight light) {
        if (light.getTimer() >= light.getRedDuration()) {
            light.swapAxis();
            light.setState(new GreenState());
        }
    }

    @Override
    public LightPhase getPhase() {
        return LightPhase.RED;
    }
}
