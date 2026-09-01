package trafficsim.model.road;

// TODO: [Pair B] — Callum + JUBRIL
public class BusStop {
    private int x;
    private int y;
    private String name;

    public BusStop(int x, int y, String name) {
        this.x = x;
        this.y = y;
        this.name = name;
    }

    public int[] getPosition() {
        // TODO: return position as int[] {x, y}
        return new int[]{x, y};
    }

    public String getName() {
        return name;
    }
}
