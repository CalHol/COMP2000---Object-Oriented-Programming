package trafficsim.model.road;

import java.util.ArrayList;
import java.util.List;

// TODO: [Pair B] — Callum + JUBRIL
public class RoadNetwork {
    private List<Road> roads = new ArrayList<>();
    private List<Intersection> intersections = new ArrayList<>();

    public void addRoad(Road road) { roads.add(road); }
    public void addIntersection(Intersection intersection) { intersections.add(intersection); }
    public List<Road> getRoads() { return roads; }
    public List<Intersection> getIntersections() { return intersections; }
}
