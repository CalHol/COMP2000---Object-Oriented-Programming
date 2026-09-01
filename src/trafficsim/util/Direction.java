package trafficsim.util;

import java.util.List;

public enum Direction {
    NORTH, SOUTH, EAST, WEST;

    public Direction opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST  -> WEST;
            case WEST  -> EAST;
        };
    }

    public List<Direction> perpendiculars() {
        return switch (this) {
            case NORTH, SOUTH -> List.of(EAST, WEST);
            case EAST,  WEST  -> List.of(NORTH, SOUTH);
        };
    }

    public int dx() { return this == EAST ? 1 : this == WEST ? -1 : 0; }
    public int dy() { return this == SOUTH ? 1 : this == NORTH ? -1 : 0; }

    /**
     * Perpendicular "right" direction — used to offset a vehicle onto the correct
     * side of the road for right-hand-drive traffic. In screen coordinates (y grows
     * downward) the right of EAST is SOUTH, right of NORTH is EAST, etc.
     */
    public int rightX() {
        return switch (this) {
            case EAST, WEST -> 0;
            case NORTH -> 1;
            case SOUTH -> -1;
        };
    }
    public int rightY() {
        return switch (this) {
            case NORTH, SOUTH -> 0;
            case EAST -> 1;
            case WEST -> -1;
        };
    }
}
