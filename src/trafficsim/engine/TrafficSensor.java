package trafficsim.engine;

import trafficsim.SimConstants;
import trafficsim.util.LightPhase;
import trafficsim.model.road.Intersection;
import trafficsim.model.road.Lane;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.road.Roundabout;
import trafficsim.model.vehicle.Vehicle;

/**
 * Pure read-only sensor: given a vehicle and the world, produce a
 * {@link SensorReading} describing what's in front of the vehicle.
 * Callable from a parallel stream — takes no locks, mutates nothing.
 */
final class TrafficSensor {

    private TrafficSensor() {}

    static SensorReading read(Vehicle v, RoadNetwork network) {
        Lane lane = v.getLane();
        Road road = v.getRoad();
        if (lane == null || road == null) {
            return SensorReading.clear(road, lane, v.getDirection());
        }

        double dVehicle = nearestVehicleAhead(v, lane);
        double dRed = v.respectsRedLight()
                ? nearestRedStopAhead(v, network)
                : Double.POSITIVE_INFINITY;
        double dYield = nearestRoundaboutYield(v, network);
        double dBlocked = v.respectsRedLight()
                ? nearestBlockedIntersection(v, network)
                : Double.POSITIVE_INFINITY;
        double dEV = evYieldDistance(v); // non-EVs slow when a siren-on EV is in the same lane
        double dStop = min4(dRed, dYield, dBlocked, dEV);
        Intersection here = intersectionAt(v, network);

        return new SensorReading(
                dVehicle, dStop, road, lane, v.getDirection(),
                here != null, here);
    }

    private static double nearestVehicleAhead(Vehicle self, Lane lane) {
        double best = Double.POSITIVE_INFINITY;
        double selfHalf = self.getLength() / 2.0;
        // EVs with siren get a much tighter safety margin so they don't crawl behind traffic —
        // they still detect obstacles (so no collisions), but the "min gap" shrinks.
        boolean isEV = self instanceof trafficsim.model.vehicle.EmergencyVehicle ev && ev.isSirenOn();
        double gap = isEV ? 1.0 : SimConstants.MIN_BUMPER_GAP;
        for (Vehicle other : lane.getVehicles()) {
            if (other == self) continue;
            double centreToCentre = signedDistanceAhead(self, other.getX(), other.getY());
            double noseToTail = centreToCentre - selfHalf - (other.getLength() / 2.0) - gap;
            if (noseToTail > 0 && noseToTail < best) best = noseToTail;
        }
        return best;
    }

    /**
     * If a siren-on EV is behind this vehicle in the same lane and within
     * {@link SimConstants#EV_PULLOVER_RANGE}, return a small distance so we
     * effectively brake and make room. Non-EV vehicles only.
     */
    static double evYieldDistance(Vehicle self) {
        if (self instanceof trafficsim.model.vehicle.EmergencyVehicle) return Double.POSITIVE_INFINITY;
        Lane lane = self.getLane();
        if (lane == null) return Double.POSITIVE_INFINITY;
        for (Vehicle other : lane.getVehicles()) {
            if (!(other instanceof trafficsim.model.vehicle.EmergencyVehicle ev)) continue;
            if (!ev.isSirenOn()) continue;
            if (other == self) continue;
            // EV behind us in our travel direction means it's ahead in the OPPOSITE direction
            // — we want to yield if EV is close in either direction along the lane.
            double d = Math.hypot(other.getX() - self.getX(), other.getY() - self.getY());
            if (d < SimConstants.EV_PULLOVER_RANGE) return 3.0; // hold at almost-stopped
        }
        return Double.POSITIVE_INFINITY;
    }

    private static double nearestRedStopAhead(Vehicle self, RoadNetwork network) {
        double best = Double.POSITIVE_INFINITY;
        for (Intersection i : network.getIntersections()) {
            if (!i.hasSignal()) continue;
            if (i.getLight().phaseFor(self.getDirection()) != LightPhase.RED) continue;
            double d = signedDistanceAhead(self, i.getX(), i.getY()) - SimConstants.STOP_LINE_RADIUS;
            if (d > 0 && d < best && d < SimConstants.SIGHT_RANGE) best = d;
        }
        return best;
    }

    /**
     * "Don't block the box" — if there's a stopped vehicle in our lane just past
     * the near-side of an intersection AND we can't fit past it, hold back on
     * this side of the crosswalk.
     */
    private static double nearestBlockedIntersection(Vehicle self, RoadNetwork network) {
        double best = Double.POSITIVE_INFINITY;
        for (Intersection i : network.getIntersections()) {
            if (!i.hasSignal()) continue;
            // ignore reds — that check already applies
            if (i.getLight().phaseFor(self.getDirection()) == LightPhase.RED) continue;
            double signed = signedDistanceAhead(self, i.getX(), i.getY());
            if (signed <= 0 || signed > SimConstants.SIGHT_RANGE) continue;
            // Is there a vehicle stopped just past the intersection, within one car-length
            // beyond the far side? If yes, this box is blocked — hold on this side of the crosswalk.
            if (downstreamIsBlocked(self, i)) {
                double d = signed - SimConstants.STOP_LINE_RADIUS;
                if (d > 0 && d < best) best = d;
            }
        }
        return best;
    }

    private static boolean downstreamIsBlocked(Vehicle self, Intersection intersection) {
        // A vehicle in our lane whose position is *past* the intersection AND close to it AND stopped.
        Lane lane = self.getLane();
        if (lane == null) return false;
        for (Vehicle other : lane.getVehicles()) {
            if (other == self) continue;
            if (other.getSpeed() > 1.0) continue; // moving = not blocking
            double d = signedDistanceAhead(self, other.getX(), other.getY());
            // Just past the intersection = between one intersection-radius past centre
            // and two intersection-radii past centre.
            double intersectionAheadDist = signedDistanceAhead(self, intersection.getX(), intersection.getY());
            double diff = d - intersectionAheadDist;
            if (diff > 0 && diff < SimConstants.INT_HALF * 2 + other.getLength()) return true;
        }
        return false;
    }

    private static double nearestRoundaboutYield(Vehicle self, RoadNetwork network) {
        double best = Double.POSITIVE_INFINITY;
        for (Intersection i : network.getIntersections()) {
            if (!(i instanceof Roundabout ring)) continue;
            double signed = signedDistanceAhead(self, ring.getX(), ring.getY());
            if (signed <= 0 || signed > SimConstants.SIGHT_RANGE) continue;
            if (signed < ring.getOuterRadius() + 6) continue;
            if (ringIsBlocked(self, ring)) {
                double d = signed - ring.getOuterRadius() - 4;
                if (d > 0 && d < best) best = d;
            }
        }
        return best;
    }

    private static boolean ringIsBlocked(Vehicle self, Roundabout ring) {
        double entryAngle = Vehicle.entryAngleFor(self.getDirection());
        return !ring.entryIsClear(entryAngle);
    }

    private static Intersection intersectionAt(Vehicle v, RoadNetwork network) {
        for (Intersection i : network.getIntersections()) {
            double r = (i instanceof Roundabout ring) ? ring.getOuterRadius() : SimConstants.INTERSECTION_TILE_RADIUS;
            if (Math.hypot(i.getX() - v.getX(), i.getY() - v.getY()) <= r) return i;
        }
        return null;
    }

    private static double signedDistanceAhead(Vehicle self, double px, double py) {
        double dx = px - self.getX();
        double dy = py - self.getY();
        return dx * self.getDirection().dx() + dy * self.getDirection().dy();
    }

    private static double min4(double a, double b, double c, double d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }
}
