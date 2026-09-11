package trafficsim.model.light;

import trafficsim.util.LightPhase;

public class GreenState implements LightState {
    @Override
    public void update(TrafficLight light) {
        if (light.getTimer() >= light.getGreenDuration()) {
            light.setState(new YellowState());
        }
    }

    @Override
    public LightPhase getPhase() {
        return LightPhase.GREEN;
    }
}
