package Road;

public class Road {
    private int x1, y1, x2, y2;
    private int speedLimit;
    private Lane[] lanes;

public Road(int x1, int y1, int x2, int y2, int speedLimit, Lane[] lanes) {
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
    this.speedLimit = speedLimit;
    this.lanes = lanes;
}

public class Lane {
    public int getAxis() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }
}

public Lane[] getLanes() {
    if (lanes == null) {
        throw new IllegalStateException("Lanes have not been initialized.");
    }
    return lanes;
}

public int getSpeedLimit() {
    return speedLimit;
}}

