package trafficsim.model.vehicle;

import trafficsim.SimConstants;
import trafficsim.engine.SensorReading;
import trafficsim.util.Direction;
import trafficsim.model.road.Lane;
import trafficsim.model.road.Road;
import trafficsim.model.road.Roundabout;
import trafficsim.strategy.TurnStrategy;

import java.util.Random;

public abstract sealed class Vehicle
        permits Car, Truck, Bus, EmergencyVehicle {

    protected double x, y;
    protected double speed;
    protected final double maxSpeed;
    protected final double length; // physical length in world units — sensor uses this
    protected Direction direction;
    protected final TurnStrategy turnStrategy;

    protected double accelStep = 0.5;
    protected double slowdownProbability = 0.10;
    private DriverProfile profile = DriverProfile.NORMAL;

    private Lane lane;

    private Lane pendingLane;
    private Direction pendingDirection;
    /** ticks-since-last-turn — used to avoid re-turning within the same intersection tile. */
    private int turnCooldown;
    private int turnCount;

    // -- roundabout state ----------------------------------------------------
    private Roundabout activeRing;
    private double ringAngle;         // current position around ring centre (radians)
    private Direction ringExit;       // direction to leave the ring heading
    private double ringArcTraveled;   // radians traversed since entering
    private double ringTargetArc;     // radians we need to traverse before exiting

    // -- signal turn state (smooth Bezier arc) ------------------------------
    private boolean turningAtSignal;
    private double p0x, p0y, p1x, p1y, p2x, p2y;   // quadratic Bezier control points
    private double turnProgress;                    // 0..1
    private double turnLength;                      // approx arc length for pacing speed
    private Direction turnFinalDirection;
    private Lane turnFinalLane;
    private trafficsim.model.road.Intersection turnIntersection;

    // -- smoothed orientation (independent of discrete direction) -----------
    private double orientation; // radians, screen-coord (y-down)

    private static Random noise = new Random(42);
    private static final int TURN_COOLDOWN_TICKS = SimConstants.TURN_COOLDOWN_TICKS;
    private static final double RING_CRUISE_SPEED = SimConstants.RING_CRUISE_SPEED;

    /** For deterministic testing — swaps the shared RNG. Call from Main before starting. */
    public static void seedNoise(long seed) { noise = new Random(seed); }

    protected Vehicle(double x, double y, double maxSpeed, double length,
                      Direction direction, TurnStrategy turnStrategy) {
        this.x = x;
        this.y = y;
        this.speed = 0;
        this.maxSpeed = maxSpeed;
        this.length = length;
        this.direction = direction;
        this.turnStrategy = turnStrategy;
        this.orientation = canonicalAngle(direction);
    }

    /**
     * Nagel–Schreckenberg-style step, obstacle-aware. See
     * {@code 02-Design Patterns/Emergent Behaviour.md} in the vault.
     */
    public void move(SensorReading r) {
        if (turningAtSignal) { moveAlongTurn(); return; }
        double obstacle = r.effectiveStopDistance() - 1.0; // 1 unit safety buffer
        double target = Math.min(maxSpeed, Math.max(0, obstacle));
        // If we've decided to turn AND we're within sight of the intersection, ease off
        // to a comfortable turn speed instead of arriving hot. Real drivers slow before
        // the corner, not on it.
        boolean approachingTurn = pendingDirection != null && r.currentIntersection() != null;
        if (approachingTurn) {
            target = Math.min(target, RING_CRUISE_SPEED + 0.5); // ~3 units
        } else if (r.atIntersection() && pendingDirection == null && turnCooldown == 0) {
            // even if we haven't picked a turn yet, ease off approaching any intersection
            target = Math.min(target, maxSpeed * 0.7);
        }
        double effectiveAccel = accelStep * profile.accelMultiplier();

        if (target < speed) {
            speed = target;
        } else if (speed < target) {
            speed = Math.min(target, speed + effectiveAccel);
        }

        double p = effectiveSlowdownProbability();
        if (noise.nextDouble() < p && speed > 0) {
            speed = Math.max(0, speed - 1.0);
        }

        x += direction.dx() * speed;
        y += direction.dy() * speed;

        if (turnCooldown > 0) turnCooldown--;
        if (r.atIntersection() && turnStrategy != null && turnCooldown == 0) {
            considerTurn(r);
        }
    }

    /** Subclasses may override to opt out of profile-driven slowdown (see EmergencyVehicle). */
    protected double effectiveSlowdownProbability() {
        return profile.slowdownProbability();
    }

    /**
     * Decide whether to turn and record the intent in {@code pendingLane}/
     * {@code pendingDirection}. Actual lane transfer happens in the engine's
     * serial phase so we never mutate a lane from a parallel context.
     */
    private void considerTurn(SensorReading r) {
        if (r.currentIntersection() == null) return;
        // options = straight + both perpendiculars; engine resolves availability
        var options = new java.util.ArrayList<Direction>();
        options.add(direction);
        options.addAll(direction.perpendiculars());
        Direction chosen = turnStrategy.chooseDirection(options);
        if (chosen == direction) return;
        pendingDirection = chosen;
    }

    public void brake() {
        speed = 0;
    }

    /** Default: cars, trucks and buses stop for red lights. Overridden by emergency vehicles. */
    protected boolean stopsAtRedLight() {
        return true;
    }
    public boolean respectsRedLight() { return stopsAtRedLight(); }

    /** Visitor accept — each concrete subclass dispatches to the matching visit method. */
    public abstract <R> R accept(VehicleVisitor<R> visitor);

    // -- back-reference plumbing --------------------------------------------

    /** Called from {@code Lane.addVehicle} — do not call directly. */
    public void attachTo(Lane lane) { this.lane = lane; }
    public Lane getLane() { return lane; }
    public Road getRoad() { return lane == null ? null : lane.getRoad(); }

    public Lane consumePendingLane() {
        Lane p = pendingLane; pendingLane = null; return p;
    }
    public Direction consumePendingDirection() {
        Direction p = pendingDirection; pendingDirection = null; return p;
    }
    /** Non-consuming peek — used by the display for turn-signal rendering. */
    public Direction peekPendingDirection() { return pendingDirection; }
    public void setPendingLane(Lane l) { this.pendingLane = l; }
    public boolean isBraking() { return speed < 0.05; }

    // -- driver profile ------------------------------------------------------

    public void setDriverProfile(DriverProfile profile) {
        this.profile = profile == null ? DriverProfile.NORMAL : profile;
    }

    public DriverProfile getDriverProfile() { return profile; }

    /** Force a new position — used by the engine after a turn transfer. Resets speed to 0. */
    public void repositionAt(double newX, double newY) {
        this.x = newX; this.y = newY; this.speed = 0;
    }

    /** Called by the engine when a turn transfer completes — starts the re-turn cooldown. */
    public void markTurned() { this.turnCooldown = TURN_COOLDOWN_TICKS; this.turnCount++; }

    // -- fluid signal-turn state --------------------------------------------

    public boolean isTurningAtSignal() { return turningAtSignal; }
    public Lane getTurnFinalLane() { return turnFinalLane; }
    public Direction getTurnFinalDirection() { return turnFinalDirection; }
    public trafficsim.model.road.Intersection getTurnIntersection() { return turnIntersection; }

    /**
     * Kick off a fluid Bezier turn. Vehicle leaves its lane and traces an arc
     * from its current position through the intersection centre to the
     * destination lane. Called by the engine on turn commit.
     */
    public void startSignalTurn(trafficsim.model.road.Intersection at, Direction newDir, Lane newLane) {
        this.turningAtSignal = true;
        this.turnProgress = 0;
        this.turnIntersection = at;
        this.turnFinalDirection = newDir;
        this.turnFinalLane = newLane;

        p0x = x; p0y = y;
        // Control point at intersection centre with a slight offset toward the exit for a smoother curve
        p1x = at.getX() + newDir.dx() * 4;
        p1y = at.getY() + newDir.dy() * 4;
        // End point: comfortably past the intersection tile in the new direction, on the new lane centreline
        p2x = at.getX() + newDir.dx() * 34 + newDir.rightX() * Lane.LANE_HALF_WIDTH;
        p2y = at.getY() + newDir.dy() * 34 + newDir.rightY() * Lane.LANE_HALF_WIDTH;
        // Rough arc length for speed pacing (chord + 20%)
        turnLength = Math.hypot(p2x - p0x, p2y - p0y) * 1.2;
        speed = Math.min(speed, 2.5);
    }

    /** Advance one tick along the Bezier arc. Called from Vehicle.move when turningAtSignal is true. */
    private void moveAlongTurn() {
        if (turnLength < 1) turnLength = 40;
        double step = Math.max(0.5, speed) / turnLength;
        turnProgress += step;
        if (turnProgress > 1) turnProgress = 1;

        double t = turnProgress, u = 1 - t;
        x = u * u * p0x + 2 * u * t * p1x + t * t * p2x;
        y = u * u * p0y + 2 * u * t * p1y + t * t * p2y;
        // Tangent = derivative of Bezier
        double tx = 2 * u * (p1x - p0x) + 2 * t * (p2x - p1x);
        double ty = 2 * u * (p1y - p0y) + 2 * t * (p2y - p1y);
        orientation = Math.atan2(ty, tx);
        direction = nearestCardinal(tx, ty);
        if (speed < 2.5) speed = Math.min(2.5, speed + 0.25);
    }

    /** Called by engine when the Bezier turn is complete. Clears state. */
    public void finishSignalTurn() {
        this.turningAtSignal = false;
        this.turnIntersection = null;
        this.turnFinalLane = null;
        this.turnFinalDirection = null;
    }

    public boolean turnComplete() { return turningAtSignal && turnProgress >= 1.0; }
    public double getOrientation() { return orientation; }

    // -- roundabout circulation ---------------------------------------------

    public boolean isInRing() { return activeRing != null; }
    public Roundabout getActiveRing() { return activeRing; }
    public Direction getRingExit() { return ringExit; }
    public double getRingAngle() { return ringAngle; }
    public boolean hasReachedRingExit() { return activeRing != null && ringArcTraveled >= ringTargetArc; }

    /**
     * Enter a roundabout. The entry angle is the canonical position on the ring
     * for {@code approachDir}; the target arc is the CCW sweep required to reach
     * {@code exit}. Called by {@code SimulationEngine.performTransfers}.
     */
    public void enterRing(Roundabout ring, Direction approachDir, Direction exit) {
        this.activeRing = ring;
        this.ringExit = exit;
        // Enter at the actual angle where the vehicle already is (from its lane offset).
        // Falls back to the canonical entry angle if we're inexplicably on top of the centre.
        double actualAngle;
        double dx = x - ring.getX(), dy = y - ring.getY();
        if (Math.hypot(dx, dy) < 1.0) actualAngle = entryAngleFor(approachDir);
        else actualAngle = Math.atan2(dy, dx);
        this.ringAngle = actualAngle;
        this.ringArcTraveled = 0;
        double sweep = normalizePositive(ringAngle - exitAngleFor(exit));
        this.ringTargetArc = sweep < 0.15 ? 2 * Math.PI : sweep; // U-turn → full loop
        double midR = ring.midRadius();
        this.x = ring.getX() + midR * Math.cos(ringAngle);
        this.y = ring.getY() + midR * Math.sin(ringAngle);
        this.direction = tangentDirection(ringAngle);
        this.speed = Math.min(speed, 1.5);
    }

    public void exitRing() {
        this.activeRing = null;
        this.ringExit = null;
        this.turnCooldown = TURN_COOLDOWN_TICKS;
        this.turnCount++;
    }

    /** Advance one tick along the arc. Returns true if we've reached the exit angle. */
    public boolean moveInRing() {
        double midR = activeRing.midRadius();
        // Look ahead on the arc for the nearest ring vehicle and enforce a min arc gap.
        double minGapArc = (length + 6) / midR;          // radians equivalent to ~1 vehicle length + gap
        double nearestAheadArc = Double.POSITIVE_INFINITY;
        for (Vehicle other : activeRing.getInRing()) {
            if (other == this) continue;
            // arc distance ahead (CCW motion = angle decreasing → other is ahead if their angle < ours after normalization)
            double delta = ringAngle - other.ringAngle;
            while (delta < 0) delta += 2 * Math.PI;
            if (delta > 0 && delta < nearestAheadArc) nearestAheadArc = delta;
        }
        double target = RING_CRUISE_SPEED;
        if (nearestAheadArc < minGapArc * 2) {
            // Slow proportionally to how close we are
            double gapFrac = Math.max(0, (nearestAheadArc - minGapArc) / minGapArc);
            target = RING_CRUISE_SPEED * gapFrac;
        }
        if (speed < target) speed = Math.min(target, speed + 0.25);
        else speed = target;

        double stepArc = speed / midR;
        ringAngle -= stepArc;
        if (ringAngle < -Math.PI) ringAngle += 2 * Math.PI;
        ringArcTraveled += stepArc;
        x = activeRing.getX() + midR * Math.cos(ringAngle);
        y = activeRing.getY() + midR * Math.sin(ringAngle);
        direction = tangentDirection(ringAngle);
        orientation = Math.atan2(-Math.cos(ringAngle), Math.sin(ringAngle));
        return ringArcTraveled >= ringTargetArc;
    }

    /** Canonical entry angle on the ring for a given approach direction. */
    public static double entryAngleFor(Direction approach) {
        return switch (approach) {
            case EAST  -> Math.PI;
            case WEST  -> 0;
            case SOUTH -> -Math.PI / 2;
            case NORTH -> Math.PI / 2;
        };
    }

    public static double exitAngleFor(Direction exit) {
        return switch (exit) {
            case EAST  -> 0;
            case SOUTH -> Math.PI / 2;
            case WEST  -> Math.PI;
            case NORTH -> -Math.PI / 2;
        };
    }

    private static double normalizePositive(double a) {
        while (a < 0) a += 2 * Math.PI;
        while (a >= 2 * Math.PI) a -= 2 * Math.PI;
        return a;
    }

    /** Nearest cardinal direction to the tangent at ringAngle (CCW motion). */
    private static Direction tangentDirection(double a) {
        double tx = Math.sin(a);
        double ty = -Math.cos(a);
        return nearestCardinal(tx, ty);
    }

    private static Direction nearestCardinal(double dx, double dy) {
        if (Math.abs(dx) > Math.abs(dy)) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dy > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    public static double canonicalAngle(Direction d) {
        return switch (d) {
            case EAST -> 0;
            case SOUTH -> Math.PI / 2;
            case WEST -> Math.PI;
            case NORTH -> -Math.PI / 2;
        };
    }

    // -- getters -------------------------------------------------------------

    public double getSpeed() { return speed; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getLength() { return length; }
    public Direction getDirection() { return direction; }
    public void setDirection(Direction d) { this.direction = d; this.orientation = canonicalAngle(d); }
    public double getMaxSpeed() { return maxSpeed; }
    public int getTurnCount() { return turnCount; }
    public TurnStrategy getTurnStrategy() { return turnStrategy; }
}
