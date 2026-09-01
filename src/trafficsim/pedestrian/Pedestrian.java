package trafficsim.pedestrian;

import java.awt.Color;
import java.awt.Rectangle;

/**
 * A pedestrian walks a fixed rectangular sidewalk loop around one city block.
 * Position is parametric — {@code t} is the arc-length traversed along the
 * perimeter, wrapping at the perimeter length. Speeds vary per pedestrian so
 * the crowd doesn't march in lockstep.
 */
public class Pedestrian {

    private final Rectangle loop;
    private final double perimeter;
    private double t;
    private final double speed;
    private final Color shirt;
    private final Color pants;
    private final int direction;
    private double x, y;

    public Pedestrian(Rectangle loop, double startT, double speed, int direction, Color shirt, Color pants) {
        this.loop = loop;
        this.perimeter = 2.0 * (loop.width + loop.height);
        this.t = ((startT % perimeter) + perimeter) % perimeter;
        this.speed = speed;
        this.direction = direction < 0 ? -1 : 1;
        this.shirt = shirt;
        this.pants = pants;
        recomputePosition();
    }

    public void step() {
        t = ((t + direction * speed) % perimeter + perimeter) % perimeter;
        recomputePosition();
    }

    private void recomputePosition() {
        int w = loop.width, h = loop.height;
        int x0 = loop.x, y0 = loop.y;
        double u = t;
        if (u < w) { x = x0 + u; y = y0; }
        else if (u < w + h) { x = x0 + w; y = y0 + (u - w); }
        else if (u < 2.0 * w + h) { x = x0 + w - (u - w - h); y = y0 + h; }
        else { x = x0; y = y0 + h - (u - 2.0 * w - h); }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public Color getShirt() { return shirt; }
    public Color getPants() { return pants; }
    public double getSpeed() { return speed; }
}
