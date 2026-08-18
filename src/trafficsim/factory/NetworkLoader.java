package trafficsim.factory;

import trafficsim.exception.InvalidNetworkException;
import trafficsim.model.light.TrafficLight;
import trafficsim.model.road.*;
import trafficsim.util.Direction;

// TODO: [Pair B] — Callum + TBD
public class NetworkLoader {

    public RoadNetwork loadFromFile(String path) throws InvalidNetworkException {
        // TODO: parse a file and build a RoadNetwork; throw InvalidNetworkException on bad input
        throw new InvalidNetworkException("File loading not yet implemented: " + path);
    }

    public RoadNetwork buildDefault() {
        RoadNetwork network = new RoadNetwork();

        Road road = new Road(0, 250, 800, 250, 60);
        road.addLane(new Lane(Direction.EAST));
        road.addLane(new Lane(Direction.WEST));
        network.addRoad(road);

        Intersection intersection = new Intersection(400, 250, new TrafficLight(10, 10));
        intersection.addRoad(road);
        network.addIntersection(intersection);

        return network;
    }
}
