package trafficsim.model.road;

import trafficsim.model.light.TrafficLight;
import java.util.ArrayList;
import java.util.List;

// TODO: [Pair D] — Addrita + Cam
public class Intersection {
    private int x;
    private int y;
    private TrafficLight light;
    private List<Road> connectedRoads = new ArrayList<>();

    public Intersection(int x, int y, TrafficLight light) {
        this.x = x;
        this.y = y;
        this.light = light;
    }

    public void update() {
        light.update();
    }

    public TrafficLight getLight() { return light; }
    public int getX() { return x; }
    public int getY() { return y; }
    public void addRoad(Road road) { connectedRoads.add(road); }
    public List<Road> getConnectedRoads() { return connectedRoads; }
}
