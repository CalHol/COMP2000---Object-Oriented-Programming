package trafficsim.model.light;

import trafficsim.util.LightPhase;

public interface LightState {
    void update(TrafficLight light);
    LightPhase getPhase();
}
