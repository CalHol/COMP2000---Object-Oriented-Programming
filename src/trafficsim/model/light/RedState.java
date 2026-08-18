package trafficsim.model.light;

import trafficsim.util.LightPhase;

// TODO: [Pair D] — Addrita + Cam
public class RedState implements LightState {
    @Override
    public void update(TrafficLight light) {
        // TODO: if timer >= redDuration, transition to GreenState
    }

    @Override
    public LightPhase getPhase() {
        return LightPhase.RED;
    }
}
