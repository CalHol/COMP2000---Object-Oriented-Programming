package trafficsim.model.road;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class RoadNetwork {

    private final List<Road> roads = new ArrayList<>();
    private final List<Intersection> intersections = new ArrayList<>();

    public void addRoad(Road road) { roads.add(road); }
    public void addIntersection(Intersection intersection) { intersections.add(intersection); }

    public List<Road> getRoads() { return roads; }
    public List<Intersection> getIntersections() { return intersections; }

    /** Bounding box of every road endpoint — used for spawn/despawn edge detection. */
    public Rectangle getBounds() {
        if (roads.isEmpty()) return new Rectangle(0, 0, 0, 0);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Road r : roads) {
            minX = Math.min(minX, Math.min(r.getX1(), r.getX2()));
            minY = Math.min(minY, Math.min(r.getY1(), r.getY2()));
            maxX = Math.max(maxX, Math.max(r.getX1(), r.getX2()));
            maxY = Math.max(maxY, Math.max(r.getY1(), r.getY2()));
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }
}
