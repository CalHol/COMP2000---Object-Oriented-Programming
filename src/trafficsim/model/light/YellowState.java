package trafficsim.model.light;

import trafficsim.util.LightPhase;

public class YellowState implements LightState {
    @Override
    public void update(TrafficLight light) {
        if (light.getTimer() >= light.getYellowDuration()) {
            light.setState(new RedState());
        }
    }

    @Override
    public LightPhase getPhase() {
        return LightPhase.YELLOW;
    }
}
