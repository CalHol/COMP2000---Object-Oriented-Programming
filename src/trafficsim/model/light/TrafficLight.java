package trafficsim.model.light;

import trafficsim.util.Axis;
import trafficsim.util.Direction;
import trafficsim.util.LightPhase;

/**
 * Axis-based signalised intersection. One {@link Axis} is "the green axis" at
 * any moment; the perpendicular axis is RED. When the green axis's state cycles
 * GREEN → YELLOW → (brief all-)RED, the light swaps axis and starts a new
 * GREEN on the perpendicular direction.
 *
 * <p>Rendering queries {@link #phaseFor(Direction)} to know what colour to
 * show each approach; {@link trafficsim.engine.TrafficSensor} uses the same
 * accessor to decide whether a vehicle heading in some direction should stop.
 */
public class TrafficLight {

    private Axis greenAxis;
    private LightState state;
    private int timer;
    private final int greenDuration;
    private final int yellowDuration;
    private final int redDuration; // brief all-red pause between axis swaps

    public TrafficLight(int greenDuration, int yellowDuration, int redDuration) {
        this.greenDuration = greenDuration;
        this.yellowDuration = yellowDuration;
        this.redDuration = redDuration;
        this.timer = 0;
        this.greenAxis = Axis.HORIZONTAL;
        this.state = new GreenState();
    }

    /** Construct with a starting axis — used to stagger neighbouring intersections. */
    public TrafficLight(int greenDuration, int yellowDuration, int redDuration, Axis startingAxis) {
        this(greenDuration, yellowDuration, redDuration);
        this.greenAxis = startingAxis;
    }

    public void update() {
        timer++;
        state.update(this);
    }

    /** Phase the given axis is currently showing. */
    public LightPhase phaseFor(Axis axis) {
        return axis == greenAxis ? state.getPhase() : LightPhase.RED;
    }

    public LightPhase phaseFor(Direction d) {
        return phaseFor(Axis.of(d));
    }

    /** Overall phase of the currently-owning axis (kept for backward compat). */
    public LightPhase getPhase() { return state.getPhase(); }

    public Axis getGreenAxis() { return greenAxis; }
    public boolean isGreen(Direction d) { return phaseFor(d) == LightPhase.GREEN; }

    // -- package-private helpers used by state classes -----------------------
    void setState(LightState next) { this.state = next; this.timer = 0; }
    void swapAxis() { this.greenAxis = greenAxis.other(); }

    public int getTimer() { return timer; }
    public int getGreenDuration() { return greenDuration; }
    public int getYellowDuration() { return yellowDuration; }
    public int getRedDuration() { return redDuration; }

    /** Adaptive extension — rewind the timer so the state machine stays in-phase longer. */
    public void extendCurrentPhase(int extraTicks) {
        this.timer = Math.max(0, timer - extraTicks);
    }
}
