package trafficsim.model.light;

import trafficsim.util.LightPhase;

// TODO: [Pair D] — Addrita + Cam
public class GreenState implements LightState {
    @Override
    public void update(TrafficLight light) {
        // TODO: if timer >= greenDuration, transition to YellowState
    }

    @Override
    public LightPhase getPhase() {
        return LightPhase.GREEN;
    }
}
