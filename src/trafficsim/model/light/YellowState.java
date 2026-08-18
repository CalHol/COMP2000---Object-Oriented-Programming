package trafficsim.model.light;

import trafficsim.util.LightPhase;

// TODO: [Pair D] — Addrita + Cam
public class YellowState implements LightState {
    private static final int YELLOW_DURATION = 3;

    @Override
    public void update(TrafficLight light) {
        // TODO: if timer >= YELLOW_DURATION, transition to RedState
    }

    @Override
    public LightPhase getPhase() {
        return LightPhase.YELLOW;
    }
}
