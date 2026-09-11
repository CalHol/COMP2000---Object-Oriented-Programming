package trafficsim.util;

public enum Direction {
    NORTH, SOUTH, EAST, WEST;

    public int dx() { return this == EAST ? 1 : this == WEST ? -1 : 0; }
    public int dy() { return this == SOUTH ? 1 : this == NORTH ? -1 : 0; }

    /** Perpendicular left direction for Australian traffic (screen y grows downward). */
    public int leftX() {
        return switch (this) {
            case EAST, WEST -> 0;
            case NORTH -> -1;
            case SOUTH -> 1;
        };
    }
    public int leftY() {
        return switch (this) {
            case NORTH, SOUTH -> 0;
            case EAST -> -1;
            case WEST -> 1;
        };
    }
}
