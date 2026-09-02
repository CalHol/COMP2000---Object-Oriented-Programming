package trafficsim.model.road;

import java.util.Objects;

public class BusStop {
    private final int x;
    private final int y;
    private final String name;

    public BusStop(int x, int y, String name) {
        this.name = Objects.requireNonNull(
                name,
                "Bus stop name cannot be null.");

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "Bus stop name cannot be blank.");
        }

        this.x = x;
        this.y = y;
    }

    public int[] getPosition() {
        return new int[] {x, y};
    }

    public String getName() {
        return name;
    }
}