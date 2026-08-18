package trafficsim.model.light;

import trafficsim.util.LightPhase;

// TODO: [Pair D] — Addrita + Cam
public class TrafficLight {
    private LightState state;
    private int timer;
    private int greenDuration;
    private int redDuration;

    public TrafficLight(int greenDuration, int redDuration) {
        this.greenDuration = greenDuration;
        this.redDuration = redDuration;
        this.timer = 0;
        this.state = new RedState();
    }

    public void update() {
        // TODO: delegate to state.update(this) to advance the light
        state.update(this);
        timer++;
    }

    public LightPhase getPhase() {
        return state.getPhase();
    }

    public boolean isGreen() {
        return state.getPhase() == LightPhase.GREEN;
    }

    public void setState(LightState state) {
        this.state = state;
        this.timer = 0;
    }

    public int getTimer() { return timer; }
    public int getGreenDuration() { return greenDuration; }
    public int getRedDuration() { return redDuration; }
}
