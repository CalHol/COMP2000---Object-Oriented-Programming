package trafficsim.engine;

import trafficsim.SimConstants;
import trafficsim.util.Axis;
import trafficsim.util.LightPhase;
import trafficsim.model.road.Intersection;
import trafficsim.model.road.Lane;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.vehicle.EmergencyVehicle;
import trafficsim.model.vehicle.Vehicle;

/**
 * Pure read-only sensor: given a vehicle and the world, produce a
 * {@link SensorReading} describing what's in front of the vehicle.
 * It takes no locks and mutates nothing, so every vehicle can read the same tick snapshot.
 */
final class TrafficSensor {

    private TrafficSensor() {}

    static SensorReading read(Vehicle v, RoadNetwork network) {
        Lane lane = v.getLane();
        Road road = v.getRoad();
        if (lane == null || road == null) {
            return SensorReading.clear();
        }

        double dVehicle = nearestVehicleAhead(v, lane);
        double dSignal = v.respectsRedLight()
                ? nearestSignalStopAhead(v, network)
                : Double.POSITIVE_INFINITY;
        double dBlocked = v.respectsRedLight()
                ? nearestBlockedIntersection(v, network)
                : Double.POSITIVE_INFINITY;
        double dOccupied = nearestOccupiedIntersectionStop(v, network);
        double dEntryPriority = nearestConflictingEntryPriorityStop(v, network);
        double dIntersectionPriority = nearestEmergencyPriorityStop(v, network);
        // One SensorReading stores only the nearest reason to stop at an intersection.
        double dStop = Math.min(Math.min(dSignal, dBlocked),
                Math.min(Math.min(dOccupied, dEntryPriority), dIntersectionPriority));
        return new SensorReading(dVehicle, dStop);
    }

    private static double nearestVehicleAhead(Vehicle self, Lane lane) {
        double best = Double.POSITIVE_INFINITY;
        double selfHalf = self.getLength() / 2.0;
        // EVs with siren get a much tighter safety margin so they don't crawl behind traffic —
        // they still detect obstacles (so no collisions), but the "min gap" shrinks.
        boolean isEV = self instanceof EmergencyVehicle ev && ev.isSirenOn();
        double gap = isEV ? 1.0 : SimConstants.MIN_BUMPER_GAP;
        for (Vehicle other : lane.getVehicles()) {
            if (other == self) continue;
            double centreToCentre = signedDistanceAhead(self, other.getX(), other.getY());
            // Convert centre distance to free bumper-to-bumper space, including a safety gap.
            double noseToTail = centreToCentre - selfHalf - (other.getLength() / 2.0) - gap;
            if (centreToCentre > 0 && noseToTail < best) {
                best = Math.max(0.0, noseToTail);
            }
        }
        return best;
    }

    private static double nearestSignalStopAhead(Vehicle self, RoadNetwork network) {
        double best = Double.POSITIVE_INFINITY;
        for (Intersection i : network.getIntersections()) {
            if (!i.getConnectedRoads().contains(self.getRoad())) continue;
            if (i.getLight().phaseFor(self.getDirection()) == LightPhase.GREEN) continue;
            // The stop line is STOP_LINE_RADIUS units before the junction centre.
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
            if (!i.getConnectedRoads().contains(self.getRoad())) continue;
            // Signal stops already hold vehicles while this approach is not green.
            if (i.getLight().phaseFor(self.getDirection()) != LightPhase.GREEN) continue;
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

    /**
     * Every vehicle waits before entering if perpendicular traffic is still
     * inside the junction. Vehicles already inside continue and clear it.
     */
    private static double nearestOccupiedIntersectionStop(
            Vehicle self, RoadNetwork network) {
        double best = Double.POSITIVE_INFINITY;
        for (Intersection i : network.getIntersections()) {
            if (!i.getConnectedRoads().contains(self.getRoad())) continue;
            double signed = signedDistanceAhead(self, i.getX(), i.getY());
            // A vehicle that crossed the stop line but has not entered the physical
            // junction can still stop if a conflict appears at the last moment.
            if (signed <= 0 || signed > SimConstants.SIGHT_RANGE
                    || isInsideIntersection(self, i)) continue;
            if (!hasConflictingVehicleInside(self, i)) continue;

            double d = Math.max(0.0, signed - SimConstants.STOP_LINE_RADIUS);
            if (d < best) best = d;
        }
        return best;
    }

    /**
     * Enforces emergency priority without giving emergency vehicles collision immunity.
     * Vehicles already inside an intersection are never stopped by this rule.
     */
    private static double nearestEmergencyPriorityStop(Vehicle self, RoadNetwork network) {
        boolean selfHasSiren = self instanceof EmergencyVehicle ev && ev.isSirenOn();
        double best = Double.POSITIVE_INFINITY;
        for (Intersection i : network.getIntersections()) {
            if (!i.getConnectedRoads().contains(self.getRoad())) continue;
            double signed = signedDistanceAhead(self, i.getX(), i.getY());
            if (signed <= 0 || signed > SimConstants.SIGHT_RANGE
                    || isInsideIntersection(self, i)) continue;

            boolean mustYield = selfHasSiren
                    ? hasHigherPriorityConflictingEmergency(self, i, signed)
                    : hasApproachingConflictingEmergency(self, i);
            if (!mustYield) continue;

            double d = Math.max(0.0, signed - SimConstants.STOP_LINE_RADIUS);
            if (d < best) best = d;
        }
        return best;
    }

    private static boolean hasConflictingVehicleInside(Vehicle self, Intersection intersection) {
        for (Road road : intersection.getConnectedRoads()) {
            for (Lane lane : road.getLanes()) {
                if (Axis.of(lane.getDirection()) == Axis.of(self.getDirection())) continue;
                for (Vehicle other : lane.getVehicles()) {
                    if (other == self) continue;
                    // Count a vehicle as occupying the junction until its rear has cleared it.
                    double occupiedRadius = SimConstants.INTERSECTION_TILE_RADIUS
                            + other.getLength() / 2.0;
                    if (Math.hypot(other.getX() - intersection.getX(),
                            other.getY() - intersection.getY()) <= occupiedRadius) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isInsideIntersection(Vehicle vehicle, Intersection intersection) {
        double occupiedRadius = SimConstants.INTERSECTION_TILE_RADIUS
                + vehicle.getLength() / 2.0;
        return Math.hypot(vehicle.getX() - intersection.getX(),
                vehicle.getY() - intersection.getY()) <= occupiedRadius;
    }

    /**
     * Prevent two perpendicular vehicles from crossing the junction boundary in
     * the same tick. For normal traffic, the currently green axis wins. Emergency
     * priority is handled separately below.
     */
    private static double nearestConflictingEntryPriorityStop(
            Vehicle self, RoadNetwork network) {
        if (self instanceof EmergencyVehicle ev && ev.isSirenOn()) {
            return Double.POSITIVE_INFINITY;
        }
        double best = Double.POSITIVE_INFINITY;
        for (Intersection intersection : network.getIntersections()) {
            if (!intersection.getConnectedRoads().contains(self.getRoad())) continue;
            double signed = signedDistanceAhead(self, intersection.getX(), intersection.getY());
            if (signed <= 0 || signed > SimConstants.SIGHT_RANGE
                    || isInsideIntersection(self, intersection)) continue;
            if (intersection.getLight().phaseFor(self.getDirection()) == LightPhase.GREEN) continue;

            for (Road road : intersection.getConnectedRoads()) {
                for (Lane lane : road.getLanes()) {
                    if (Axis.of(lane.getDirection()) == Axis.of(self.getDirection())) continue;
                    if (intersection.getLight().phaseFor(lane.getDirection()) != LightPhase.GREEN) continue;
                    for (Vehicle other : lane.getVehicles()) {
                        if (other instanceof EmergencyVehicle) continue;
                        if (!willEnterNextTick(other, intersection)) continue;
                        double d = Math.max(0.0, signed - SimConstants.STOP_LINE_RADIUS);
                        if (d < best) best = d;
                    }
                }
            }
        }
        return best;
    }

    private static boolean willEnterNextTick(Vehicle vehicle, Intersection intersection) {
        if (isInsideIntersection(vehicle, intersection)) return false;
        double signed = signedDistanceAhead(vehicle, intersection.getX(), intersection.getY());
        if (signed <= 0) return false;
        double occupiedRadius = SimConstants.INTERSECTION_TILE_RADIUS
                + vehicle.getLength() / 2.0;
        double distanceFromCentre = Math.hypot(vehicle.getX() - intersection.getX(),
                vehicle.getY() - intersection.getY());
        // One unit also covers a vehicle that will accelerate from a near stop.
        return distanceFromCentre - occupiedRadius <= Math.max(1.0, vehicle.getSpeed());
    }

    private static boolean hasApproachingConflictingEmergency(
            Vehicle self, Intersection intersection) {
        for (Road road : intersection.getConnectedRoads()) {
            for (Lane lane : road.getLanes()) {
                if (Axis.of(lane.getDirection()) == Axis.of(self.getDirection())) continue;
                for (Vehicle other : lane.getVehicles()) {
                    if (!(other instanceof EmergencyVehicle ev) || !ev.isSirenOn()) continue;
                    double distance = signedDistanceAhead(ev,
                            intersection.getX(), intersection.getY());
                    if (distance >= -SimConstants.INTERSECTION_TILE_RADIUS
                            && distance <= SimConstants.SIGHT_RANGE
                            && pathToIntersectionIsClear(ev, lane, distance)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasHigherPriorityConflictingEmergency(
            Vehicle self, Intersection intersection, double selfDistance) {
        Axis selfAxis = Axis.of(self.getDirection());
        for (Road road : intersection.getConnectedRoads()) {
            for (Lane lane : road.getLanes()) {
                if (Axis.of(lane.getDirection()) == selfAxis) continue;
                for (Vehicle other : lane.getVehicles()) {
                    if (!(other instanceof EmergencyVehicle emergency)
                            || !emergency.isSirenOn()) continue;
                    double otherDistance = signedDistanceAhead(
                            emergency, intersection.getX(), intersection.getY());
                    if (otherDistance < -SimConstants.INTERSECTION_TILE_RADIUS
                            || otherDistance > SimConstants.SIGHT_RANGE
                            || !pathToIntersectionIsClear(emergency, lane, otherDistance)) continue;
                    if (otherDistance < selfDistance - 1.0) return true;
                    if (Math.abs(otherDistance - selfDistance) <= 1.0
                            && selfAxis == Axis.VERTICAL) return true;
                }
            }
        }
        return false;
    }

    private static boolean pathToIntersectionIsClear(
            EmergencyVehicle emergency, Lane lane, double intersectionDistance) {
        if (intersectionDistance <= 0) return true;
        for (Vehicle other : lane.getVehicles()) {
            if (other == emergency) continue;
            double ahead = signedDistanceAhead(
                    emergency, other.getX(), other.getY());
            if (ahead > 0 && ahead < intersectionDistance) return false;
        }
        return true;
    }

    private static double signedDistanceAhead(Vehicle self, double px, double py) {
        double dx = px - self.getX();
        double dy = py - self.getY();
        // Dot product: positive means ahead, negative means behind, measured along travel direction.
        return dx * self.getDirection().dx() + dy * self.getDirection().dy();
    }

}
