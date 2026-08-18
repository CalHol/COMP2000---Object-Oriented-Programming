package trafficsim.model.light;

import trafficsim.util.LightPhase;

// TODO: [Pair D] — Addrita + Cam
public interface LightState {
    void update(TrafficLight light);
    LightPhase getPhase();
}
