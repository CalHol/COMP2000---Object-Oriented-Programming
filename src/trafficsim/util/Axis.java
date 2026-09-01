package trafficsim.util;

import trafficsim.util.Direction;

/** Which axis a traffic-light phase applies to. Perpendicular axes alternate green/red. */
public enum Axis {
    HORIZONTAL, VERTICAL;

    public static Axis of(Direction d) {
        return switch (d) {
            case EAST, WEST   -> HORIZONTAL;
            case NORTH, SOUTH -> VERTICAL;
        };
    }

    public Axis other() {
        return this == HORIZONTAL ? VERTICAL : HORIZONTAL;
    }
}
