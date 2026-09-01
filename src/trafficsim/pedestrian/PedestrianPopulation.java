package trafficsim.pedestrian;

import trafficsim.util.Axis;
import trafficsim.model.road.Intersection;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.road.Roundabout;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Populates the sidewalk loops around each city block with {@link Pedestrian}s
 * and each signalised intersection with a couple of {@link CrosswalkPedestrian}s.
 */
public class PedestrianPopulation {

    private static final Color[] SHIRT = {
            new Color(0xE8, 0x66, 0x66), new Color(0x66, 0x8A, 0xE8),
            new Color(0x6E, 0xC8, 0x76), new Color(0xE8, 0xC0, 0x50),
            new Color(0xC8, 0x76, 0xE8), new Color(0xE8, 0xE8, 0xE8),
            new Color(0x40, 0x40, 0x48), new Color(0xF0, 0x88, 0x40)
    };
    private static final Color[] PANTS = {
            new Color(0x30, 0x30, 0x38), new Color(0x46, 0x36, 0x2A),
            new Color(0x2C, 0x3C, 0x60), new Color(0x60, 0x54, 0x40)
    };

    private static final int SIDEWALK_INSET = 3;

    private final List<Pedestrian> loopers = new ArrayList<>();
    private final List<CrosswalkPedestrian> crossers = new ArrayList<>();

    public PedestrianPopulation(RoadNetwork network, int perBlock) {
        Random r = new Random(2024);
        var blocks = computeBlockLoops(network);
        for (int b = 0; b < blocks.size(); b++) {
            Rectangle loop = blocks.get(b);
            double perimeter = 2.0 * (loop.width + loop.height);
            for (int i = 0; i < perBlock; i++) {
                double startT = r.nextDouble() * perimeter;
                double speed = 0.35 + r.nextDouble() * 0.75;
                int dir = r.nextBoolean() ? 1 : -1;
                loopers.add(new Pedestrian(loop, startT, speed, dir,
                        SHIRT[r.nextInt(SHIRT.length)], PANTS[r.nextInt(PANTS.length)]));
            }
            // Blocks whose index matches the display's PARK slot get extra walkers on the cross paths.
            // Display roster: {PARK, HOSPITAL, TOWER, ANCHOR_STORE} → PARK is block 0.
            if (b % 4 == 0) {
                int cx = loop.x + loop.width / 2;
                int cy = loop.y + loop.height / 2;
                // Two extra walkers on the vertical path (top-of-park to fountain to bottom)
                loopers.add(new Pedestrian(
                        new Rectangle(cx - 2, loop.y + 4, 4, loop.height - 8),
                        r.nextDouble() * loop.height,
                        0.4 + r.nextDouble() * 0.5,
                        r.nextBoolean() ? 1 : -1,
                        SHIRT[r.nextInt(SHIRT.length)], PANTS[r.nextInt(PANTS.length)]));
                // Two extra on the horizontal path
                loopers.add(new Pedestrian(
                        new Rectangle(loop.x + 4, cy - 2, loop.width - 8, 4),
                        r.nextDouble() * loop.width,
                        0.4 + r.nextDouble() * 0.5,
                        r.nextBoolean() ? 1 : -1,
                        SHIRT[r.nextInt(SHIRT.length)], PANTS[r.nextInt(PANTS.length)]));
            }
        }
        // 2 crosswalk pedestrians per signalised intersection (one per axis)
        for (Intersection ix : network.getIntersections()) {
            if (!ix.hasSignal()) continue;
            crossers.add(new CrosswalkPedestrian(ix, Axis.HORIZONTAL, r.nextDouble(),
                    0.015 + r.nextDouble() * 0.008, r.nextBoolean() ? 1 : -1,
                    SHIRT[r.nextInt(SHIRT.length)], PANTS[r.nextInt(PANTS.length)]));
            crossers.add(new CrosswalkPedestrian(ix, Axis.VERTICAL, r.nextDouble(),
                    0.015 + r.nextDouble() * 0.008, r.nextBoolean() ? 1 : -1,
                    SHIRT[r.nextInt(SHIRT.length)], PANTS[r.nextInt(PANTS.length)]));
        }
    }

    public void stepAll() {
        for (Pedestrian p : loopers) p.step();
        for (CrosswalkPedestrian p : crossers) p.step();
    }

    public List<Pedestrian> getAll() { return loopers; }
    public List<CrosswalkPedestrian> getCrossers() { return crossers; }

    private static List<Rectangle> computeBlockLoops(RoadNetwork network) {
        var hs = network.getRoads().stream().filter(Road::isHorizontal)
                .sorted((a, b) -> Integer.compare(a.getY1(), b.getY1())).toList();
        var vs = network.getRoads().stream().filter(Road::isVertical)
                .sorted((a, b) -> Integer.compare(a.getX1(), b.getX1())).toList();

        int sidewalkH = trafficsim.SimConstants.SIDEWALK_H;
        List<Rectangle> out = new ArrayList<>();
        for (int j = 0; j < hs.size() - 1; j++) {
            for (int i = 0; i < vs.size() - 1; i++) {
                int baseMargin = sidewalkH - SIDEWALK_INSET;
                int x = vs.get(i).getX1() + baseMargin;
                int y = hs.get(j).getY1() + baseMargin;
                int w = vs.get(i + 1).getX1() - vs.get(i).getX1() - 2 * baseMargin;
                int h = hs.get(j + 1).getY1() - hs.get(j).getY1() - 2 * baseMargin;
                Rectangle loop = new Rectangle(x, y, w, h);
                // If any corner of this loop lies inside a Roundabout's outer radius, shrink the
                // loop enough that none do — pedestrians can't walk through the ring's grass.
                loop = shrinkAwayFromRoundabouts(loop, network);
                out.add(loop);
            }
        }
        return out;
    }

    private static Rectangle shrinkAwayFromRoundabouts(Rectangle rect, RoadNetwork network) {
        int shrink = 0;
        int[][] corners = {
                { rect.x, rect.y },
                { rect.x + rect.width, rect.y },
                { rect.x, rect.y + rect.height },
                { rect.x + rect.width, rect.y + rect.height }
        };
        for (Intersection i : network.getIntersections()) {
            if (!(i instanceof Roundabout ring)) continue;
            for (int[] c : corners) {
                double d = Math.hypot(c[0] - ring.getX(), c[1] - ring.getY());
                if (d < ring.getOuterRadius() + 4) {
                    // needed shrink to push corner outside the ring
                    int extra = (int) Math.ceil(ring.getOuterRadius() + 4 - d);
                    if (extra > shrink) shrink = extra;
                }
            }
        }
        if (shrink == 0) return rect;
        return new Rectangle(rect.x + shrink, rect.y + shrink,
                Math.max(1, rect.width - 2 * shrink),
                Math.max(1, rect.height - 2 * shrink));
    }
}
