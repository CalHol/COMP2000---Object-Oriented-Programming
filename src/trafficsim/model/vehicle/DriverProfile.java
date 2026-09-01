package trafficsim.model.vehicle;

/**
 * Per-vehicle personality knob. Modulates the base acceleration and Nagel–
 * Schreckenberg slowdown probability of a {@link Vehicle}. Attached at spawn
 * time by {@code VehicleSpawner} so no two vehicles feel identical, which is
 * what makes jams form unevenly.
 *
 * <ul>
 *   <li>{@link #AGGRESSIVE} — accelerates hard, rarely slows randomly.</li>
 *   <li>{@link #NORMAL}     — the baseline profile from the UML.</li>
 *   <li>{@link #CAUTIOUS}   — slow to accelerate, frequent random slowdowns.</li>
 * </ul>
 */
public enum DriverProfile {
    AGGRESSIVE(1.6, 0.03),
    NORMAL    (1.0, 0.10),
    CAUTIOUS  (0.7, 0.20);

    private final double accelMultiplier;
    private final double slowdownProbability;

    DriverProfile(double accelMultiplier, double slowdownProbability) {
        this.accelMultiplier = accelMultiplier;
        this.slowdownProbability = slowdownProbability;
    }

    public double accelMultiplier() { return accelMultiplier; }
    public double slowdownProbability() { return slowdownProbability; }
}
