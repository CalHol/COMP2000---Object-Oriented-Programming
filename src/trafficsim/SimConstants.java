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
    public static final int  SIDEWALK_H       = ROAD_HALF + 5;
    public static final int  INT_HALF         = ROAD_HALF + 4;

    // -- sensor / stopping distances ----------------------------------------
    public static final double INTERSECTION_TILE_RADIUS = 24.0;
    public static final double STOP_LINE_RADIUS         = 52.0;
    public static final double SIGHT_RANGE              = 250.0;
    public static final double MIN_BUMPER_GAP           =  4.0;
    public static final double EV_PULLOVER_RANGE        = 80.0;   // detect siren-on EV within
    public static final double EV_PULLOVER_NUDGE        =  4.0;   // perpendicular offset

    // -- vehicle physics -----------------------------------------------------
    public static final int    TURN_COOLDOWN_TICKS = 40;
    public static final double RING_CRUISE_SPEED   = 2.5;
    public static final double RING_ENTRY_ARC_CLEARANCE = Math.PI / 5.0; // 36° — wider than the in-ring min-gap

    // -- spawner -------------------------------------------------------------
    public static final int    SPAWN_CAP         = 42;
    public static final double SPAWN_PROBABILITY = 0.14;

    // -- adaptive signals ----------------------------------------------------
    public static final int    ADAPTIVE_QUEUE_HIGH     = 4;   // vehicles queued to trigger extension
    public static final int    ADAPTIVE_EXTEND_TICKS   = 12;  // extra green ticks per extension
    public static final int    ADAPTIVE_MAX_EXTENSIONS = 3;

    // -- metrics -------------------------------------------------------------
    public static final int    HISTORY_SAMPLES = 120; // ticks of rolling history in HUD

    // -- day/night cycle -----------------------------------------------------
    public static final int    CYCLE_TICKS = 2400; // one full day
}
