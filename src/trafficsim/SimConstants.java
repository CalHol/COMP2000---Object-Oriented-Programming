package trafficsim;

/**
 * Central home for magic numbers that used to live scattered across
 * {@code TrafficSensor}, {@code Vehicle}, {@code VehicleSpawner},
 * {@code SimulationDisplay}, and elsewhere. Anyone tuning the simulation
 * should look here first.
 */
public final class SimConstants {

    private SimConstants() {}

    // -- world geometry ------------------------------------------------------
    public static final int  LANE_WIDTH       = 18;   // width of one lane, world units
    public static final int  ROAD_HALF        = LANE_WIDTH;  // two-lane road → half-width == LANE_WIDTH
    public static final int  INT_HALF         = ROAD_HALF + 4;

    // -- sensor / stopping distances ----------------------------------------
    public static final double INTERSECTION_TILE_RADIUS = 24.0; // centre-to-edge clearance zone
    public static final double STOP_LINE_RADIUS         = 52.0; // stop before entering a junction
    public static final double SIGHT_RANGE              = 250.0; // ignore hazards too far ahead to matter
    public static final double MIN_BUMPER_GAP           =  4.0; // extra space after vehicle lengths are removed
    // -- spawner -------------------------------------------------------------
    public static final int    SPAWN_CAP         = 40;
    public static final double SPAWN_PROBABILITY = 0.06;
    public static final double SPAWN_ENTRY_CLEARANCE = 36.0; // bus length + normal gap

    // -- adaptive signals ----------------------------------------------------
    public static final int    ADAPTIVE_QUEUE_HIGH     = 4;   // vehicles queued to trigger extension
    public static final int    ADAPTIVE_EXTEND_TICKS   = 12;  // extra green ticks per extension
    public static final int    ADAPTIVE_MAX_EXTENSIONS = 3;

}
