package trafficsim.model.road;

public class BusStop {

    private final int x, y;
    private final String name;

    public BusStop(int x, int y, String name) {
        this.x = x;
        this.y = y;
        this.name = name;
    }

    public int[] getPosition() {
        return new int[] { x, y };
    }

    public String getName() { return name; }
}
