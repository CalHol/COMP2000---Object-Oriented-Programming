package trafficsim.model.road;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RoadNetwork {
    private final List<Road> roads = new ArrayList<>();
    private final List<Intersection> intersections =
            new ArrayList<>();

    public void addRoad(Road road) {
        Objects.requireNonNull(road, "Road cannot be null.");

        if (roads.contains(road)) {
            throw new IllegalArgumentException(
                    "The same road cannot be added twice.");
        }

        roads.add(road);
    }

    public void addIntersection(Intersection intersection) {
        Objects.requireNonNull(
                intersection,
                "Intersection cannot be null.");

        if (intersections.contains(intersection)) {
            throw new IllegalArgumentException(
                    "The same intersection cannot be added twice.");
        }

        intersections.add(intersection);
    }

    public List<Road> getRoads() {
        return Collections.unmodifiableList(roads);
    }

    public List<Intersection> getIntersections() {
        return Collections.unmodifiableList(intersections);
    }

    /**
     * Calculates the bounding box containing every road endpoint.
     * This is used for vehicle spawn and despawn edge detection.
     */
    public Rectangle getBounds() {
        if (roads.isEmpty()) {
            return new Rectangle(0, 0, 0, 0);
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Road road : roads) {
            minX = Math.min(
                    minX,
                    Math.min(road.getX1(), road.getX2()));

            minY = Math.min(
                    minY,
                    Math.min(road.getY1(), road.getY2()));

            maxX = Math.max(
                    maxX,
                    Math.max(road.getX1(), road.getX2()));

            maxY = Math.max(
                    maxY,
                    Math.max(road.getY1(), road.getY2()));
        }

        return new Rectangle(
                minX,
                minY,
                maxX - minX,
                maxY - minY);
    }
}