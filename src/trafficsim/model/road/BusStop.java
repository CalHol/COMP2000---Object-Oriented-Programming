package trafficsim.model.road;

import trafficsim.util.Direction;

import java.util.Objects;

public class BusStop {
    private final int x;
    private final int y;
    private final String name;
    private final Direction direction;

    public BusStop(int x, int y, String name, Direction direction) {
        this.name = Objects.requireNonNull(
                name,
                "Bus stop name cannot be null.");

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "Bus stop name cannot be blank.");
        }

        this.x = x;
        this.y = y;
        this.direction = Objects.requireNonNull(
                direction,
                "Bus stop direction cannot be null.");
    }

    public int[] getPosition() {
        return new int[] {x, y};
    }

    public String getName() {
        return name;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean serves(Lane lane) {
        if (lane == null || lane.getRoad() == null || lane.getDirection() != direction) {
            return false;
        }
        Road road = lane.getRoad();
        if (road.isHorizontal()) {
            // A horizontal stop must sit on its direction's shifted lane centre and within road ends.
            double laneY = road.getY1() + direction.leftY() * Lane.LANE_HALF_WIDTH;
            return Math.abs(y - laneY) < 0.5
                    && x >= Math.min(road.getX1(), road.getX2())
                    && x <= Math.max(road.getX1(), road.getX2());
        }
        // Same test for vertical roads, where the lane shift is on the x-axis.
        double laneX = road.getX1() + direction.leftX() * Lane.LANE_HALF_WIDTH;
        return Math.abs(x - laneX) < 0.5
                && y >= Math.min(road.getY1(), road.getY2())
                && y <= Math.max(road.getY1(), road.getY2());
    }
}
