package trafficsim.view;

import trafficsim.SimConstants;
import trafficsim.engine.SimulationEngine;
import trafficsim.engine.SimulationObserver;
import trafficsim.util.Axis;
import trafficsim.util.LightPhase;
import trafficsim.util.Direction;
import trafficsim.model.road.Intersection;
import trafficsim.model.road.Lane;
import trafficsim.model.road.Road;
import trafficsim.model.road.Roundabout;
import trafficsim.pedestrian.CrosswalkPedestrian;
import trafficsim.pedestrian.Pedestrian;
import trafficsim.engine.Statistics;
import trafficsim.model.vehicle.Bus;
import trafficsim.model.vehicle.EmergencyVehicle;
import trafficsim.model.vehicle.Truck;
import trafficsim.model.vehicle.Vehicle;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * SimCity 1-era top-down renderer with day/night, per-road congestion overlay,
 * brake lights, turn signals, ring chevrons, mixed block layouts, in-city
 * trees, and bus-stop shelters.
 */
public class SimulationDisplay extends JPanel implements SimulationObserver {

    // -- palette (daylight; night applied as tint) ---------------------------
    private static final Color GRASS       = new Color(0x4A, 0x69, 0x4A);
    private static final Color GRASS_NOISE = new Color(0x00, 0x00, 0x00, 20);
    private static final Color SIDEWALK    = new Color(0xC8, 0xC6, 0xBE);
    private static final Color ASPHALT     = new Color(0x33, 0x33, 0x36);
    private static final Color SHOULDER    = new Color(0xEE, 0xEE, 0xE8);
    private static final Color CENTRE      = new Color(0xE8, 0xC2, 0x3C);
    private static final Color STOP_LINE   = new Color(0xF6, 0xF6, 0xF0);
    private static final Color LANE_ARROW  = new Color(0xF4, 0xF4, 0xEE, 220);

    private static final Color[] BLOCK_GROUND = {
            new Color(0x62, 0x66, 0x62), new Color(0x6A, 0x6E, 0x6A),
            new Color(0x5C, 0x60, 0x5C), new Color(0x64, 0x68, 0x64)
    };
    private static final Color PARK_GROUND = new Color(0x3E, 0x66, 0x40);
    private static final Color[] BUILDING = {
            new Color(0x8A, 0x76, 0x60), new Color(0x9E, 0x8A, 0x74),
            new Color(0x76, 0x86, 0x8E), new Color(0x6E, 0x62, 0x54),
            new Color(0x8E, 0x8C, 0x92), new Color(0x74, 0x82, 0x76),
            new Color(0xA0, 0x86, 0x66), new Color(0x82, 0x7C, 0x72),
            new Color(0x9C, 0x94, 0x8A)
    };
    private static final Color[] ROOFS = {
            new Color(0x36, 0x2E, 0x28), new Color(0x40, 0x38, 0x30),
            new Color(0x2C, 0x2A, 0x26), new Color(0x48, 0x3C, 0x32)
    };
    private static final Color WINDOW = new Color(0xF6, 0xE6, 0xA6, 200);
    private static final Color WINDOW_LIT = new Color(0xFF, 0xE0, 0x60, 240);
    private static final Color BUILDING_SHADOW = new Color(0, 0, 0, 60);

    private static final Color TREE_TRUNK   = new Color(0x38, 0x22, 0x14);
    private static final Color TREE_LEAVES  = new Color(0x36, 0x58, 0x38);
    private static final Color TREE_LEAVES2 = new Color(0x2A, 0x48, 0x2C);
    private static final Color TREE_SHADOW  = new Color(0, 0, 0, 50);

    private static final Color BULB_DIM = new Color(0x1A, 0x1A, 0x1E);
    private static final Color BULB_RED = new Color(0xE8, 0x5C, 0x5C);
    private static final Color BULB_YEL = new Color(0xE8, 0xC8, 0x3C);
    private static final Color BULB_GRN = new Color(0x50, 0xD8, 0x64);
    private static final Color SIGNAL_HOUSING = new Color(0x14, 0x14, 0x18);
    private static final Color SIGNAL_POLE = new Color(0x2A, 0x2A, 0x2E);

    private static final Color CAR_A     = new Color(0xE6, 0xE6, 0xEC);
    private static final Color CAR_B     = new Color(0xD6, 0x4A, 0x4A);
    private static final Color CAR_C     = new Color(0x4A, 0x6A, 0xA8);
    private static final Color CAR_D     = new Color(0x38, 0x9E, 0x60);
    private static final Color TRUCK_COL = new Color(0x7A, 0x9C, 0xD8);
    private static final Color BUS_COL   = new Color(0xEA, 0xB4, 0x40);
    private static final Color EV_A      = new Color(0xE8, 0x50, 0x50);
    private static final Color EV_B      = new Color(0x4A, 0x82, 0xE8);
    private static final Color VEHICLE_SHADOW = new Color(0, 0, 0, 90);
    private static final Color WINDSHIELD = new Color(0x18, 0x22, 0x2E, 200);
    private static final Color BRAKE_LIGHT = new Color(0xE8, 0x20, 0x20);
    private static final Color HEADLIGHT = new Color(0xFF, 0xF4, 0xA6, 210);
    private static final Color TURN_BLINK = new Color(0xF0, 0xB4, 0x38);

    private static final Color BUS_SHELTER_ROOF = new Color(0x40, 0x50, 0x64);
    private static final Color BUS_SHELTER_GLASS = new Color(0x9C, 0xB4, 0xC8, 160);

    // -- geometry ------------------------------------------------------------
    private static final int LANE_WIDTH   = SimConstants.LANE_WIDTH;
    private static final int ROAD_HALF    = SimConstants.ROAD_HALF;
    private static final int SIDEWALK_H   = SimConstants.SIDEWALK_H;
    private static final int INT_HALF     = SimConstants.INT_HALF;

    private final SimulationEngine engine;
    private boolean highlightEmergency = false;
    private boolean showCongestionOverlay = false;

    public SimulationDisplay(SimulationEngine engine) {
        this.engine = engine;
        setBackground(GRASS);
        setPreferredSize(new Dimension(1320, 1010));
    }

    public void setHighlightEmergency(boolean b) { this.highlightEmergency = b; repaint(); }
    public void setShowCongestionOverlay(boolean b) { this.showCongestionOverlay = b; repaint(); }

    @Override
    public void onSimulationStep() { SwingUtilities.invokeLater(this::repaint); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        try {
            drawGrassTexture(g2);
            drawTrees(g2);
            drawCityBlocks(g2);
            drawSidewalks(g2);
            drawInCityTrees(g2);   // moved here — was hidden under block ground before
            drawAsphalt(g2);
            drawTunnelPortals(g2);
            drawShouldersAndCentres(g2);
            if (showCongestionOverlay) drawCongestionOverlay(g2);
            drawLaneArrows(g2);
            drawIntersectionTiles(g2);
            drawCrosswalks(g2);
            drawRoundabouts(g2);
            drawSignals(g2);
            drawBusShelters(g2);
            drawVehicles(g2);
            drawPedestrians(g2);
            drawNightTint(g2);
            drawStreetLampsAndHeadlights(g2);
            drawHud(g2);
        } finally { g2.dispose(); }
    }

    // -- environment ---------------------------------------------------------

    private void drawGrassTexture(Graphics2D g2) {
        Random r = new Random(9001);
        g2.setColor(GRASS_NOISE);
        for (int i = 0; i < 900; i++) g2.fillRect(r.nextInt(getWidth()), r.nextInt(getHeight()), 2, 2);
    }

    private void drawTrees(Graphics2D g2) {
        var b = engine.getNetwork().getBounds();
        Random r = new Random(4242);
        int ox = b.x - 30, oy = b.y - 30, ow = b.width + 60, oh = b.height + 60;
        for (int i = 0; i < 120; i++) {
            double t = r.nextDouble() * 4;
            int tx, ty;
            if (t < 1) { tx = ox + r.nextInt(ow); ty = oy + r.nextInt(24); }
            else if (t < 2) { tx = ox + r.nextInt(ow); ty = oy + oh - r.nextInt(24); }
            else if (t < 3) { tx = ox + r.nextInt(24); ty = oy + r.nextInt(oh); }
            else { tx = ox + ow - r.nextInt(24); ty = oy + r.nextInt(oh); }
            drawTree(g2, tx, ty, r, 7 + r.nextInt(4));
        }
    }

    /** Street trees dotted along block edges — inside the city, not just on the outer margin. */
    private void drawInCityTrees(Graphics2D g2) {
        List<Road> hs = horizontals();
        List<Road> vs = verticals();
        if (hs.size() < 2 || vs.size() < 2) return;
        Random r = new Random(31337);
        for (int j = 0; j < hs.size() - 1; j++) {
            for (int i = 0; i < vs.size() - 1; i++) {
                int x0 = vs.get(i).getX1() + SIDEWALK_H - 2;
                int y0 = hs.get(j).getY1() + SIDEWALK_H - 2;
                int x1 = vs.get(i + 1).getX1() - SIDEWALK_H + 2;
                int y1 = hs.get(j + 1).getY1() + SIDEWALK_H - 2;
                int y2 = hs.get(j + 1).getY1() - SIDEWALK_H + 2;
                // Trees along the top and bottom edges of each block
                for (int t = x0 + 16; t < x1 - 8; t += 26) {
                    if (r.nextDouble() < 0.55) drawTree(g2, t, y0 + 1, r, 6 + r.nextInt(3));
                    if (r.nextDouble() < 0.55) drawTree(g2, t, y2, r, 6 + r.nextInt(3));
                }
                // Along the left and right edges
                for (int t = y0 + 16; t < y2 - 8; t += 24) {
                    if (r.nextDouble() < 0.4) drawTree(g2, x0 + 1, t, r, 6 + r.nextInt(3));
                    if (r.nextDouble() < 0.4) drawTree(g2, x1, t, r, 6 + r.nextInt(3));
                }
            }
        }
    }

    private void drawTree(Graphics2D g2, int x, int y, Random r, int size) {
        g2.setColor(TREE_SHADOW);
        g2.fillOval(x - size / 2 + 2, y - size / 2 + 3, size + 1, size + 1);
        g2.setColor(TREE_TRUNK);
        g2.fillRect(x - 1, y, 2, 3);
        g2.setColor(r.nextBoolean() ? TREE_LEAVES : TREE_LEAVES2);
        g2.fillOval(x - size / 2, y - size / 2, size, size);
    }

    private void drawCityBlocks(Graphics2D g2) {
        List<Road> hRoads = horizontals();
        List<Road> vRoads = verticals();
        if (hRoads.size() < 2 || vRoads.size() < 2) return;

        int blockIndex = 0;
        for (int j = 0; j < hRoads.size() - 1; j++) {
            for (int i = 0; i < vRoads.size() - 1; i++) {
                int x = vRoads.get(i).getX1() + SIDEWALK_H;
                int y = hRoads.get(j).getY1() + SIDEWALK_H;
                int w = vRoads.get(i + 1).getX1() - vRoads.get(i).getX1() - 2 * SIDEWALK_H;
                int h = hRoads.get(j + 1).getY1() - hRoads.get(j).getY1() - 2 * SIDEWALK_H;

                // Hand-picked block roster so all 6 styles are represented across the 4 blocks
                // (rotating each play for variety).
                BlockStyle[] roster = {
                        BlockStyle.PARK, BlockStyle.HOSPITAL,
                        BlockStyle.TOWER, BlockStyle.ANCHOR_STORE
                };
                BlockStyle style = roster[blockIndex % roster.length];
                paintBlock(g2, x, y, w, h, blockIndex, style);
                blockIndex++;
            }
        }
    }

    private enum BlockStyle { GRID, PARK, ANCHOR_STORE, LOW_RISE, HOSPITAL, TOWER }
    private enum LightingProfile { OFFICE, RESIDENTIAL, ASLEEP }

    private void paintBlock(Graphics2D g2, int bx, int by, int bw, int bh, int seed, BlockStyle style) {
        Random r = new Random(seed * 7919L + 17);
        switch (style) {
            case PARK -> paintParkBlock(g2, bx, by, bw, bh, r);
            case ANCHOR_STORE -> paintAnchorBlock(g2, bx, by, bw, bh, r);
            case LOW_RISE -> paintLowRiseBlock(g2, bx, by, bw, bh, r);
            case GRID -> paintGridBlock(g2, bx, by, bw, bh, r);
            case HOSPITAL -> paintHospitalBlock(g2, bx, by, bw, bh, r);
            case TOWER -> paintTowerBlock(g2, bx, by, bw, bh, r);
        }
    }

    private void paintHospitalBlock(Graphics2D g2, int bx, int by, int bw, int bh, Random r) {
        // Grass/paved yard around the building
        g2.setColor(new Color(0x54, 0x60, 0x54));
        g2.fillRect(bx, by, bw, bh);
        int pad = 14;
        // Actual H-shaped hospital: two wings connected by a shorter middle
        int mx = bx + pad, my = by + pad, mw = bw - 2 * pad, mh = bh - 2 * pad;
        Color bodyC = new Color(0xF0, 0xEE, 0xE6);
        Color roofC = new Color(0x50, 0x54, 0x58);
        int wingW = mw / 3;
        int connH = mh / 3;
        // Shadows
        g2.setColor(BUILDING_SHADOW);
        g2.fillRect(mx + 2, my + 3, wingW, mh);
        g2.fillRect(mx + mw - wingW + 2, my + 3, wingW, mh);
        g2.fillRect(mx + wingW + 2, my + (mh - connH) / 2 + 3, mw - 2 * wingW, connH);
        // Left wing
        g2.setColor(bodyC); g2.fillRect(mx, my, wingW, mh);
        g2.setColor(roofC); g2.fillRect(mx, my, wingW, 4);
        // Right wing
        g2.setColor(bodyC); g2.fillRect(mx + mw - wingW, my, wingW, mh);
        g2.setColor(roofC); g2.fillRect(mx + mw - wingW, my, wingW, 4);
        // Central connector
        int cy0 = my + (mh - connH) / 2;
        g2.setColor(bodyC); g2.fillRect(mx + wingW, cy0, mw - 2 * wingW, connH);
        g2.setColor(roofC); g2.fillRect(mx + wingW, cy0, mw - 2 * wingW, 3);
        // Emergency entrance on the connector — red awning
        int awX = mx + mw / 2 - 10, awY = cy0 + connH - 8;
        g2.setColor(new Color(0xD8, 0x2C, 0x2C));
        g2.fillRect(awX, awY, 20, 6);
        g2.setColor(new Color(0xFF, 0xF4, 0xE8));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 8));
        g2.drawString("ER", awX + 4, awY + 5);
        // Rooftop red cross with night halo
        int rcx = mx + mw / 2, rcy = my + mh / 3;
        if (nightAmount() > 0.35) {
            g2.setColor(new Color(0xFF, 0x40, 0x40, 110));
            g2.fillOval(rcx - 20, rcy - 20, 40, 40);
        }
        g2.setColor(new Color(0xD8, 0x2C, 0x2C));
        g2.fillRect(rcx - 3, rcy - 10, 6, 20);
        g2.fillRect(rcx - 10, rcy - 3, 20, 6);
        // Windows on the wings only (hospitals lit 24/7)
        Color w = nightAmount() > 0.4 ? WINDOW_LIT : WINDOW;
        g2.setColor(w);
        for (int wing = 0; wing < 2; wing++) {
            int wxStart = wing == 0 ? mx + 3 : mx + mw - wingW + 3;
            for (int wy = my + 8; wy < my + mh - 4; wy += 6) {
                for (int wx = wxStart; wx < wxStart + wingW - 6; wx += 5) g2.fillRect(wx, wy, 2, 2);
            }
        }
        // Helipad on top-right roof — circle with H
        int hpX = mx + mw - wingW + wingW / 2, hpY = my + wingW / 2 + 6;
        g2.setColor(new Color(0x40, 0x44, 0x48));
        g2.fillOval(hpX - 12, hpY - 12, 24, 24);
        g2.setColor(new Color(0xF4, 0xF4, 0xEC));
        g2.drawOval(hpX - 12, hpY - 12, 24, 24);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        g2.drawString("H", hpX - 4, hpY + 5);
    }

    private void paintTowerBlock(Graphics2D g2, int bx, int by, int bw, int bh, Random r) {
        g2.setColor(BLOCK_GROUND[r.nextInt(BLOCK_GROUND.length)]);
        g2.fillRect(bx, by, bw, bh);
        int pad = 10;
        // Central tall tower
        int tw = (int) (bw * 0.42);
        int th = (int) (bh * 0.72);
        int tx = bx + (bw - tw) / 2;
        int ty = by + (bh - th) / 2;
        g2.setColor(BUILDING_SHADOW);
        g2.fillRect(tx + 4, ty + 6, tw, th);
        g2.setColor(new Color(0x4A, 0x58, 0x74));
        g2.fillRect(tx, ty, tw, th);
        // Glass-window stripes — at night they GLOW brightly (office tower staying lit)
        double n = nightAmount();
        if (n > 0.4) {
            // Halo behind the tower
            g2.setColor(new Color(0xFF, 0xE0, 0x60, 60));
            g2.fillRect(tx - 4, ty - 4, tw + 8, th + 8);
        }
        Color w = n > 0.4 ? new Color(0xFF, 0xE8, 0x74) : new Color(0xB4, 0xC8, 0xE8, 220);
        g2.setColor(w);
        for (int wy = ty + 6; wy < ty + th - 3; wy += 5) {
            for (int wx = tx + 3; wx < tx + tw - 3; wx += 4) g2.fillRect(wx, wy, 2, 3);
        }
        // Antenna spire with a lit blinker at night
        g2.setColor(new Color(0x2A, 0x2A, 0x30));
        g2.fillRect(tx + tw / 2 - 1, ty - 8, 2, 8);
        boolean blink = (engine.getTickCount() / 10) % 2 == 0;
        if (nightAmount() > 0.3 && blink) {
            g2.setColor(new Color(0xFF, 0x60, 0x60, 140));
            g2.fillOval(tx + tw / 2 - 6, ty - 14, 12, 12);
        }
        g2.setColor(blink ? new Color(0xFF, 0x80, 0x80) : new Color(0xE8, 0x50, 0x50));
        g2.fillOval(tx + tw / 2 - 2, ty - 10, 4, 4);
        // Small satellite buildings
        for (int i = 0; i < 3; i++) {
            int sw = 22, sh = 18;
            int sx = bx + pad + i * ((bw - 2 * pad) / 3);
            int sy = by + bh - pad - sh;
            drawBuilding(g2, sx, sy, sw, sh, r, false);
        }
    }

    private void paintGridBlock(Graphics2D g2, int bx, int by, int bw, int bh, Random r) {
        g2.setColor(BLOCK_GROUND[r.nextInt(BLOCK_GROUND.length)]);
        g2.fillRect(bx, by, bw, bh);
        int pad = 8;
        int cols = 2 + r.nextInt(2), rows = 2 + r.nextInt(2);
        int cw = (bw - 2 * pad) / cols, ch = (bh - 2 * pad) / rows;
        for (int cy = 0; cy < rows; cy++) for (int cx = 0; cx < cols; cx++) {
            if (r.nextDouble() < 0.15) continue;
            int mx = 3 + r.nextInt(4), my = 3 + r.nextInt(4);
            int cX = bx + pad + cx * cw, cY = by + pad + cy * ch;
            int w = cw - 2 * mx, h = ch - 2 * my;
            if (w < 8 || h < 8) continue;
            drawBuilding(g2, cX + mx, cY + my, w, h, r, false);
        }
    }

    private void paintParkBlock(Graphics2D g2, int bx, int by, int bw, int bh, Random r) {
        g2.setColor(PARK_GROUND);
        g2.fillRect(bx, by, bw, bh);
        // Grass texture (much sparser than the block-dot pattern that was confusing)
        g2.setColor(new Color(0x30, 0x50, 0x32, 60));
        for (int i = 0; i < 40; i++) {
            int gx = bx + r.nextInt(bw);
            int gy = by + r.nextInt(bh);
            g2.fillRect(gx, gy, 2, 1);
        }
        // Path
        g2.setColor(SIDEWALK);
        g2.fillRect(bx + bw / 2 - 4, by + 6, 8, bh - 12);
        g2.fillRect(bx + 6, by + bh / 2 - 4, bw - 12, 8);
        // Scattered trees — larger and denser now that dots are gone
        for (int i = 0; i < 18; i++) {
            int tx = bx + 12 + r.nextInt(Math.max(1, bw - 24));
            int ty = by + 12 + r.nextInt(Math.max(1, bh - 24));
            if (Math.abs(tx - (bx + bw / 2)) < 12 || Math.abs(ty - (by + bh / 2)) < 12) continue;
            drawTree(g2, tx, ty, r, 10 + r.nextInt(5));
        }
        // A little fountain in the middle with concentric rings
        int fx = bx + bw / 2, fy = by + bh / 2;
        g2.setColor(SIDEWALK);
        g2.fillOval(fx - 10, fy - 10, 20, 20);
        g2.setColor(new Color(0x5E, 0x8C, 0xC8));
        g2.fillOval(fx - 8, fy - 8, 16, 16);
        g2.setColor(new Color(0xAE, 0xCA, 0xE8));
        g2.fillOval(fx - 3, fy - 3, 6, 6);
    }

    private void paintAnchorBlock(Graphics2D g2, int bx, int by, int bw, int bh, Random r) {
        g2.setColor(BLOCK_GROUND[r.nextInt(BLOCK_GROUND.length)]);
        g2.fillRect(bx, by, bw, bh);
        int pad = 10;
        // One big anchor building
        int aw = bw - 2 * pad, ah = (int) (bh * 0.65) - pad;
        drawBuilding(g2, bx + pad, by + pad, aw, ah, r, true);
        // A row of small shops below
        int sy = by + pad + ah + 6;
        int sh = bh - (sy - by) - pad;
        int shops = 3;
        int sw = (aw - (shops - 1) * 4) / shops;
        for (int i = 0; i < shops; i++) {
            drawBuilding(g2, bx + pad + i * (sw + 4), sy, sw, sh, r, false);
        }
    }

    private void paintLowRiseBlock(Graphics2D g2, int bx, int by, int bw, int bh, Random r) {
        g2.setColor(BLOCK_GROUND[r.nextInt(BLOCK_GROUND.length)]);
        g2.fillRect(bx, by, bw, bh);
        int pad = 8, gap = 4;
        int cols = 3 + r.nextInt(2);
        int cw = (bw - 2 * pad - (cols - 1) * gap) / cols;
        int h = bh - 2 * pad;
        for (int i = 0; i < cols; i++) {
            int cX = bx + pad + i * (cw + gap);
            int cH = (int) (h * (0.65 + r.nextDouble() * 0.3));
            drawBuilding(g2, cX, by + pad + (h - cH), cw, cH, r, r.nextDouble() < 0.35);
        }
    }

    private void drawBuilding(Graphics2D g2, int x, int y, int w, int h, Random r, boolean big) {
        Color body = BUILDING[r.nextInt(BUILDING.length)];
        Color roof = ROOFS[r.nextInt(ROOFS.length)];
        g2.setColor(BUILDING_SHADOW);
        g2.fillRect(x + 2, y + 3, w, h);
        g2.setColor(body);
        g2.fillRect(x, y, w, h);
        // Outline gives every building an edge — much less "flat"
        g2.setColor(new Color(0x1E, 0x1A, 0x14, 110));
        g2.drawRect(x, y, w - 1, h - 1);
        g2.setColor(roof);
        g2.fillRect(x, y, w, 3);
        // Small rooftop feature — 60% AC unit, 20% water tank, 20% nothing
        double roll = r.nextDouble();
        if (w >= 12 && h >= 12) {
            if (roll < 0.6) {
                // AC unit — small grey box near roof edge
                int au = Math.min(6, w / 3);
                g2.setColor(new Color(0x60, 0x62, 0x66));
                g2.fillRect(x + w - au - 3, y + 4, au, 3);
                g2.setColor(new Color(0x2A, 0x2A, 0x2E));
                g2.drawRect(x + w - au - 3, y + 4, au, 3);
            } else if (roll < 0.8) {
                // Water tank — small cylinder
                int wt = Math.min(4, w / 4);
                g2.setColor(new Color(0x8E, 0x86, 0x74));
                g2.fillOval(x + 3, y + 4, wt, wt);
                g2.setColor(new Color(0x2A, 0x2A, 0x2E));
                g2.drawOval(x + 3, y + 4, wt, wt);
            }
        }

        boolean isWarehouse = !big && r.nextDouble() < 0.30;
        if (isWarehouse) {
            g2.setColor(new Color(0x30, 0x38, 0x40, 220));
            g2.fillRect(x + 3, y + h / 2 - 1, w - 6, 3);
            return;
        }

        // Deterministic per-building lighting profile — offices, apartments, or dark.
        double profileRoll = r.nextDouble();
        LightingProfile profile = profileRoll < 0.35 ? LightingProfile.OFFICE
                : profileRoll < 0.72 ? LightingProfile.RESIDENTIAL : LightingProfile.ASLEEP;
        double night = nightAmount();
        double litProb = switch (profile) {
            case OFFICE      -> night > 0.35 ? 0.90 : 0.75; // brightly lit day AND night
            case RESIDENTIAL -> night > 0.35 ? 0.55 : 0.80; // more day than night
            case ASLEEP      -> night > 0.35 ? 0.05 : 0.60; // mostly dark at night
        };
        for (int wy = y + 6; wy < y + h - 3; wy += 5) {
            for (int wx = x + 3; wx < x + w - 3; wx += 5) {
                if (r.nextDouble() < 0.72) {
                    boolean lit = r.nextDouble() < litProb;
                    Color c;
                    if (night > 0.35 && lit) c = WINDOW_LIT;
                    else if (night > 0.35) c = new Color(0x18, 0x18, 0x20);
                    else c = lit ? WINDOW : new Color(0x60, 0x54, 0x40);
                    g2.setColor(c);
                    g2.fillRect(wx, wy, 2, 2);
                }
            }
        }
    }

    /**
     * A stone-arch tunnel portal at each road endpoint that sits near the map edge —
     * so vehicles don't just pop into existence on grass, they enter/exit through
     * something. Only draws if the endpoint is within ~20 units of the network's bounds.
     */
    private void drawTunnelPortals(Graphics2D g2) {
        var b = engine.getNetwork().getBounds();
        for (Road road : engine.getNetwork().getRoads()) {
            drawPortalAt(g2, road, road.getX1(), road.getY1(), b);
            drawPortalAt(g2, road, road.getX2(), road.getY2(), b);
        }
    }

    private void drawPortalAt(Graphics2D g2, Road road, int ex, int ey, java.awt.Rectangle b) {
        boolean atLeft   = Math.abs(ex - b.x) < 20;
        boolean atRight  = Math.abs(ex - (b.x + b.width)) < 20;
        boolean atTop    = Math.abs(ey - b.y) < 20;
        boolean atBottom = Math.abs(ey - (b.y + b.height)) < 20;
        if (!atLeft && !atRight && !atTop && !atBottom) return;

        int stoneHalf = ROAD_HALF + 8; // portal is wider than the asphalt
        int depth = 18;                // how far into the grass the portal extends
        Color stoneOuter = new Color(0x5C, 0x54, 0x4C);
        Color stoneInner = new Color(0x74, 0x6C, 0x60);
        Color mouth = new Color(0x08, 0x08, 0x0C);
        Color mouthGlow = new Color(0xFF, 0xE0, 0x80, 60);

        if (road.isHorizontal()) {
            int px = atLeft ? ex - depth : ex; // start of portal in x
            // Stone frame
            g2.setColor(stoneOuter);
            g2.fillRect(px - 2, ey - stoneHalf - 2, depth + 4, 2 * stoneHalf + 4);
            g2.setColor(stoneInner);
            g2.fillRect(px, ey - stoneHalf, depth, 2 * stoneHalf);
            // Mouth (dark)
            g2.setColor(mouth);
            g2.fillRect(px, ey - ROAD_HALF, depth, 2 * ROAD_HALF);
            // Faint interior glow — hint at cars-heading-into-something
            g2.setColor(mouthGlow);
            g2.fillRect(px, ey - ROAD_HALF, depth, 2 * ROAD_HALF);
            // Cap stones on top+bottom edge
            g2.setColor(new Color(0x3A, 0x34, 0x2C));
            g2.fillRect(px - 2, ey - stoneHalf - 2, depth + 4, 3);
            g2.fillRect(px - 2, ey + stoneHalf - 1, depth + 4, 3);
        } else {
            int py = atTop ? ey - depth : ey;
            g2.setColor(stoneOuter);
            g2.fillRect(ex - stoneHalf - 2, py - 2, 2 * stoneHalf + 4, depth + 4);
            g2.setColor(stoneInner);
            g2.fillRect(ex - stoneHalf, py, 2 * stoneHalf, depth);
            g2.setColor(mouth);
            g2.fillRect(ex - ROAD_HALF, py, 2 * ROAD_HALF, depth);
            g2.setColor(mouthGlow);
            g2.fillRect(ex - ROAD_HALF, py, 2 * ROAD_HALF, depth);
            g2.setColor(new Color(0x3A, 0x34, 0x2C));
            g2.fillRect(ex - stoneHalf - 2, py - 2, 3, depth + 4);
            g2.fillRect(ex + stoneHalf - 1, py - 2, 3, depth + 4);
        }
    }

    private void drawSidewalks(Graphics2D g2) {
        g2.setStroke(new BasicStroke(2 * SIDEWALK_H, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g2.setColor(SIDEWALK);
        for (Road road : engine.getNetwork().getRoads())
            g2.drawLine(road.getX1(), road.getY1(), road.getX2(), road.getY2());
    }

    private void drawAsphalt(Graphics2D g2) {
        g2.setStroke(new BasicStroke(2 * ROAD_HALF, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g2.setColor(ASPHALT);
        for (Road road : engine.getNetwork().getRoads())
            g2.drawLine(road.getX1(), road.getY1(), road.getX2(), road.getY2());
        // Subtle asphalt speckle for texture — deterministic per road
        Random rr = new Random(7);
        g2.setColor(new Color(0, 0, 0, 40));
        for (Road road : engine.getNetwork().getRoads()) {
            int minX = Math.min(road.getX1(), road.getX2());
            int maxX = Math.max(road.getX1(), road.getX2());
            int minY = Math.min(road.getY1(), road.getY2());
            int maxY = Math.max(road.getY1(), road.getY2());
            int specs = Math.max(20, (road.length()) / 14);
            for (int i = 0; i < specs; i++) {
                int x, y;
                if (road.isHorizontal()) {
                    x = minX + rr.nextInt(Math.max(1, maxX - minX));
                    y = road.getY1() - ROAD_HALF + 2 + rr.nextInt(2 * ROAD_HALF - 4);
                } else {
                    x = road.getX1() - ROAD_HALF + 2 + rr.nextInt(2 * ROAD_HALF - 4);
                    y = minY + rr.nextInt(Math.max(1, maxY - minY));
                }
                g2.fillRect(x, y, 1, 1);
            }
        }
    }

    private void drawShouldersAndCentres(Graphics2D g2) {
        Stroke shoulderStroke = new BasicStroke(1.4f);
        Stroke centreStroke = new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                1f, new float[] { 12f, 10f }, 0f);
        g2.setColor(SHOULDER);
        g2.setStroke(shoulderStroke);
        for (Road road : engine.getNetwork().getRoads()) {
            if (road.isHorizontal()) {
                int y = road.getY1();
                g2.drawLine(road.getX1(), y - ROAD_HALF, road.getX2(), y - ROAD_HALF);
                g2.drawLine(road.getX1(), y + ROAD_HALF, road.getX2(), y + ROAD_HALF);
            } else {
                int x = road.getX1();
                g2.drawLine(x - ROAD_HALF, road.getY1(), x - ROAD_HALF, road.getY2());
                g2.drawLine(x + ROAD_HALF, road.getY1(), x + ROAD_HALF, road.getY2());
            }
        }
        g2.setColor(CENTRE);
        g2.setStroke(centreStroke);
        for (Road road : engine.getNetwork().getRoads()) {
            if (road.getLanes().size() < 2) continue;
            g2.drawLine(road.getX1(), road.getY1(), road.getX2(), road.getY2());
        }
    }

    /** Colour tint over each road proportional to its current vehicle count. */
    private void drawCongestionOverlay(Graphics2D g2) {
        Map<Road, Integer> counts = new HashMap<>();
        for (Road r : engine.getNetwork().getRoads()) {
            int c = 0;
            for (Lane l : r.getLanes()) c += l.getVehicles().size();
            counts.put(r, c);
        }
        int max = counts.values().stream().max(Integer::compareTo).orElse(1);
        max = Math.max(1, max);
        Stroke stk = new BasicStroke(2 * ROAD_HALF, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        g2.setStroke(stk);
        for (Road r : engine.getNetwork().getRoads()) {
            float f = counts.get(r) / (float) max;
            int red = (int) (f * 200);
            g2.setColor(new Color(red, 40, 30, 90));
            g2.drawLine(r.getX1(), r.getY1(), r.getX2(), r.getY2());
        }
    }

    private void drawLaneArrows(Graphics2D g2) {
        g2.setColor(LANE_ARROW);
        for (Road road : engine.getNetwork().getRoads()) {
            int[] stops = intersectionStopsAlongRoad(road);
            for (int i = 0; i < stops.length - 1; i++) {
                int a = stops[i], b = stops[i + 1];
                if (b - a < 60) continue;
                int mid = (a + b) / 2;
                for (Lane lane : road.getLanes()) {
                    Direction d = lane.getDirection();
                    double cx, cy;
                    if (road.isHorizontal()) {
                        cx = mid;
                        cy = road.getY1() + d.rightY() * Lane.LANE_HALF_WIDTH;
                    } else {
                        cx = road.getX1() + d.rightX() * Lane.LANE_HALF_WIDTH;
                        cy = mid;
                    }
                    paintArrow(g2, cx, cy, d, 1.0);
                }
            }
        }
    }

    private int[] intersectionStopsAlongRoad(Road road) {
        List<Integer> hits = new ArrayList<>();
        if (road.isHorizontal()) {
            hits.add(Math.min(road.getX1(), road.getX2()));
            for (Intersection i : engine.getNetwork().getIntersections()) {
                if (i.getY() == road.getY1()
                        && i.getX() >= Math.min(road.getX1(), road.getX2())
                        && i.getX() <= Math.max(road.getX1(), road.getX2())) {
                    hits.add(i.getX() - INT_HALF);
                    hits.add(i.getX() + INT_HALF);
                }
            }
            hits.add(Math.max(road.getX1(), road.getX2()));
        } else {
            hits.add(Math.min(road.getY1(), road.getY2()));
            for (Intersection i : engine.getNetwork().getIntersections()) {
                if (i.getX() == road.getX1()
                        && i.getY() >= Math.min(road.getY1(), road.getY2())
                        && i.getY() <= Math.max(road.getY1(), road.getY2())) {
                    hits.add(i.getY() - INT_HALF);
                    hits.add(i.getY() + INT_HALF);
                }
            }
            hits.add(Math.max(road.getY1(), road.getY2()));
        }
        return hits.stream().mapToInt(Integer::intValue).sorted().toArray();
    }

    private void paintArrowAngle(Graphics2D g2, double cx, double cy, double thetaRad, double scale) {
        Polygon arrow = new Polygon(
                new int[] { (int)(-8*scale), (int)(5*scale), (int)(5*scale), (int)(9*scale), (int)(5*scale), (int)(5*scale), (int)(-8*scale) },
                new int[] { (int)(-2*scale), (int)(-2*scale), (int)(-5*scale), 0, (int)(5*scale), (int)(2*scale), (int)(2*scale) },
                7
        );
        AffineTransform saved = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(thetaRad);
        g2.fill(arrow);
        g2.setTransform(saved);
    }

    private void paintArrow(Graphics2D g2, double cx, double cy, Direction dir, double scale) {
        Polygon arrow = new Polygon(
                new int[] { (int)(-8*scale), (int)(5*scale), (int)(5*scale), (int)(9*scale), (int)(5*scale), (int)(5*scale), (int)(-8*scale) },
                new int[] { (int)(-2*scale), (int)(-2*scale), (int)(-5*scale), 0, (int)(5*scale), (int)(2*scale), (int)(2*scale) },
                7
        );
        AffineTransform saved = g2.getTransform();
        g2.translate(cx, cy);
        double theta = switch (dir) {
            case EAST -> 0;
            case SOUTH -> Math.PI / 2;
            case WEST -> Math.PI;
            case NORTH -> -Math.PI / 2;
        };
        g2.rotate(theta);
        g2.fill(arrow);
        g2.setTransform(saved);
    }

    private void drawIntersectionTiles(Graphics2D g2) {
        g2.setColor(ASPHALT);
        for (Intersection ix : engine.getNetwork().getIntersections()) {
            g2.fillRect(ix.getX() - INT_HALF, ix.getY() - INT_HALF, 2 * INT_HALF, 2 * INT_HALF);
        }
    }

    private void drawCrosswalks(Graphics2D g2) {
        g2.setColor(STOP_LINE);
        // Fill the whole road width exactly: 5 bars × 4 thick + 4 gaps × 4 = 36 = 2*ROAD_HALF.
        int n = 5, thick = 4, gap = 4;
        int stripeLength = n * thick + (n - 1) * gap; // total spanning road width — must equal 2*ROAD_HALF
        int depth = 24; // how far the crosswalk pokes into the approach (perpendicular to road)

        for (Intersection ix : engine.getNetwork().getIntersections()) {
            if (ix instanceof Roundabout) continue;
            int cx = ix.getX(), cy = ix.getY();

            // West approach on a horizontal road — bars run E-W, stacked N-S filling road.
            int westStart = cx - INT_HALF - 2 - depth;
            for (int s = 0; s < n; s++) {
                int y = cy - ROAD_HALF + s * (thick + gap);
                g2.fillRect(westStart, y, depth, thick);
            }
            int eastStart = cx + INT_HALF + 2;
            for (int s = 0; s < n; s++) {
                int y = cy - ROAD_HALF + s * (thick + gap);
                g2.fillRect(eastStart, y, depth, thick);
            }

            // North / South approaches — bars run N-S, stacked E-W filling road.
            int northStart = cy - INT_HALF - 2 - depth;
            for (int s = 0; s < n; s++) {
                int x = cx - ROAD_HALF + s * (thick + gap);
                g2.fillRect(x, northStart, thick, depth);
            }
            int southStart = cy + INT_HALF + 2;
            for (int s = 0; s < n; s++) {
                int x = cx - ROAD_HALF + s * (thick + gap);
                g2.fillRect(x, southStart, thick, depth);
            }
        }
    }

    // -- roundabouts ---------------------------------------------------------

    private void drawRoundabouts(Graphics2D g2) {
        for (Intersection ix : engine.getNetwork().getIntersections()) {
            if (!(ix instanceof Roundabout r)) continue;
            int cx = r.getX(), cy = r.getY();
            int outer = r.getOuterRadius(), inner = r.getInnerRadius();

            g2.setColor(ASPHALT);
            g2.fillOval(cx - outer, cy - outer, 2 * outer, 2 * outer);

            int midR = (outer + inner) / 2;
            g2.setColor(new Color(0xEE, 0xEE, 0xE8, 200));
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    1f, new float[] { 8f, 8f }, 0f));
            g2.drawOval(cx - midR, cy - midR, 2 * midR, 2 * midR);

            g2.setColor(new Color(0x3C, 0x54, 0x3C));
            g2.fillOval(cx - inner - 2, cy - inner - 2, 2 * (inner + 2), 2 * (inner + 2));
            g2.setColor(GRASS);
            g2.fillOval(cx - inner, cy - inner, 2 * inner, 2 * inner);

            // Monument on the island — stone plinth with a small statue on top,
            // only if the island is big enough (skip for small roundabouts).
            if (inner >= 12) {
                g2.setColor(new Color(0, 0, 0, 90));
                g2.fillOval(cx - 5, cy + 2, 12, 5);
                g2.setColor(new Color(0xA6, 0xA0, 0x94));
                g2.fillRect(cx - 6, cy - 4, 12, 10);
                g2.setColor(new Color(0x84, 0x7E, 0x74));
                g2.drawRect(cx - 6, cy - 4, 12, 10);
                g2.setColor(new Color(0xC8, 0xC0, 0xB0));
                g2.fillOval(cx - 3, cy - 12, 6, 10);
                g2.setColor(new Color(0x84, 0x7E, 0x74));
                g2.drawOval(cx - 3, cy - 12, 6, 10);
            } else {
                // small ring — just a decorative tree
                g2.setColor(TREE_SHADOW);
                g2.fillOval(cx - 3, cy - 1, 8, 8);
                g2.setColor(TREE_LEAVES);
                g2.fillOval(cx - 4, cy - 5, 8, 8);
            }

            g2.setColor(SHOULDER);
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawOval(cx - outer, cy - outer, 2 * outer, 2 * outer);
            g2.drawOval(cx - inner, cy - inner, 2 * inner, 2 * inner);

            // CCW chevrons on the ring surface — 8 evenly spaced, bright.
            // Each arrow rotates to the CONTINUOUS tangent angle so the diagonal ones
            // are properly slanted (were snapping to the nearest cardinal before).
            g2.setColor(new Color(0xFF, 0xFF, 0xF6, 235));
            for (int k = 0; k < 8; k++) {
                double theta = -k * Math.PI / 4.0;
                int px = (int) (cx + midR * Math.cos(theta));
                int py = (int) (cy + midR * Math.sin(theta));
                double motionAngle = Math.atan2(-Math.cos(theta), Math.sin(theta));
                paintArrowAngle(g2, px, py, motionAngle, 0.9);
            }

            // Yield triangles removed — the ring itself + circulating vehicles are clear enough
        }
    }

    private static Direction tangentDirFor(double a) {
        double tx = Math.sin(a);
        double ty = -Math.cos(a);
        if (Math.abs(tx) > Math.abs(ty)) return tx > 0 ? Direction.EAST : Direction.WEST;
        return ty > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private void drawYieldChevron(Graphics2D g2, int cx, int cy, Direction facing) {
        Polygon tri = new Polygon(new int[] { -6, 6, 0 }, new int[] { -4, -4, 5 }, 3);
        AffineTransform saved = g2.getTransform();
        g2.translate(cx, cy);
        double theta = switch (facing) {
            case NORTH -> 0;
            case EAST  -> -Math.PI / 2;
            case SOUTH -> Math.PI;
            case WEST  -> Math.PI / 2;
        };
        g2.rotate(theta);
        g2.fill(tri);
        g2.setTransform(saved);
    }

    // -- signals -------------------------------------------------------------

    private void drawSignals(Graphics2D g2) {
        int halfLane = Lane.LANE_HALF_WIDTH;
        int offset = INT_HALF + 6;
        for (Intersection ix : engine.getNetwork().getIntersections()) {
            if (!ix.hasSignal()) continue;
            LightPhase hPhase = ix.getLight().phaseFor(Axis.HORIZONTAL);
            LightPhase vPhase = ix.getLight().phaseFor(Axis.VERTICAL);
            int cx = ix.getX(), cy = ix.getY();

            drawSignalWithPole(g2, cx + offset,        cy + halfLane,       false, hPhase, cx + INT_HALF, cy + halfLane);
            drawSignalWithPole(g2, cx - offset - 26,   cy - halfLane - 12,  false, hPhase, cx - INT_HALF, cy - halfLane);
            drawSignalWithPole(g2, cx - halfLane - 12, cy + offset,         true,  vPhase, cx - halfLane, cy + INT_HALF);
            drawSignalWithPole(g2, cx + halfLane + 4,  cy - offset - 26,    true,  vPhase, cx + halfLane, cy - INT_HALF);
        }
    }

    private void drawSignalWithPole(Graphics2D g2, int hx, int hy, boolean vertical, LightPhase phase,
                                    int poleAnchorX, int poleAnchorY) {
        int bulb = 9, pad = 3;
        int longSide = 3 * bulb + 4 * pad;
        int shortSide = bulb + 2 * pad;
        int w = vertical ? shortSide : longSide;
        int h = vertical ? longSide : shortSide;

        g2.setColor(SIGNAL_POLE);
        g2.setStroke(new BasicStroke(2.4f));
        g2.drawLine(hx + w / 2, hy + h / 2, poleAnchorX, poleAnchorY);

        g2.setColor(SIGNAL_HOUSING);
        g2.fillRoundRect(hx, hy, w, h, 3, 3);

        Color rC = phase == LightPhase.RED    ? BULB_RED : BULB_DIM;
        Color yC = phase == LightPhase.YELLOW ? BULB_YEL : BULB_DIM;
        Color gC = phase == LightPhase.GREEN  ? BULB_GRN : BULB_DIM;
        if (vertical) {
            drawBulb(g2, hx + pad, hy + pad,                    bulb, rC);
            drawBulb(g2, hx + pad, hy + pad + bulb + pad,       bulb, yC);
            drawBulb(g2, hx + pad, hy + pad + 2 * (bulb + pad), bulb, gC);
        } else {
            drawBulb(g2, hx + pad,                    hy + pad, bulb, rC);
            drawBulb(g2, hx + pad + bulb + pad,       hy + pad, bulb, yC);
            drawBulb(g2, hx + pad + 2 * (bulb + pad), hy + pad, bulb, gC);
        }
    }

    private void drawBulb(Graphics2D g2, int x, int y, int size, Color c) {
        // At night, an illuminated bulb glows into its surroundings
        boolean illuminated = !c.equals(BULB_DIM);
        if (illuminated && nightAmount() > 0.35) {
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 90));
            g2.fillOval(x - 3, y - 3, size + 6, size + 6);
        }
        g2.setColor(c);
        g2.fillOval(x, y, size, size);
    }

    // -- bus shelters --------------------------------------------------------

    private void drawBusShelters(Graphics2D g2) {
        // For every Bus on the map, draw a shelter next to their first stop
        java.util.Set<int[]> drawn = new java.util.HashSet<>();
        for (Road road : engine.getNetwork().getRoads())
            for (Lane lane : road.getLanes())
                for (Vehicle v : lane.getVehicles()) {
                    if (!(v instanceof Bus b)) continue;
                    for (var stop : b.getStops()) {
                        int[] p = stop.getPosition();
                        int key = p[0] * 10000 + p[1];
                        boolean seen = drawn.stream().anyMatch(a -> a[0] == p[0] && a[1] == p[1]);
                        if (seen) continue;
                        drawn.add(p);
                        drawShelter(g2, p[0], p[1]);
                    }
                }
    }

    private void drawShelter(Graphics2D g2, int x, int y) {
        int shx = x - 18, shy = y - SIDEWALK_H - 10;
        int w = 36, roofH = 6;
        // Ground shadow
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRect(shx + 2, shy + roofH + 12, w, 3);
        // Bright yellow-green roof — instantly reads as bus shelter
        g2.setColor(new Color(0xEA, 0xD8, 0x50));
        g2.fillRect(shx, shy, w, roofH);
        g2.setColor(new Color(0x2A, 0x2A, 0x30));
        g2.drawRect(shx, shy, w, roofH);
        // Glass sides
        g2.setColor(BUS_SHELTER_GLASS);
        g2.fillRect(shx + 1, shy + roofH, w - 2, 10);
        g2.setColor(new Color(0xFF, 0xFF, 0xFF, 80));
        g2.drawRect(shx + 1, shy + roofH, w - 2, 10);
        // BUS text on the roof
        g2.setColor(new Color(0x2A, 0x2A, 0x30));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 8));
        g2.drawString("BUS", shx + w / 2 - 8, shy + roofH - 1);
    }

    // -- vehicles ------------------------------------------------------------

    private void drawVehicles(Graphics2D g2) {
        long tick = engine.getTickCount();
        for (Road road : engine.getNetwork().getRoads())
            for (Lane lane : road.getLanes())
                for (Vehicle v : lane.getVehicles())
                    drawVehicle(g2, v, tick, false);
        for (Intersection ix : engine.getNetwork().getIntersections())
            if (ix instanceof Roundabout ring)
                for (Vehicle v : ring.getInRing())
                    drawVehicle(g2, v, tick, true);
        // Vehicles mid-turn at a signalised intersection aren't in any lane either
        for (Vehicle v : engine.getTurningVehicles()) drawVehicle(g2, v, tick, true);
    }

    private void drawVehicle(Graphics2D g2, Vehicle v, long tick, boolean inRing) {
        int longSide = (int) v.getLength();
        int shortSide = v instanceof Bus || v instanceof Truck ? 12
                : v instanceof EmergencyVehicle ? 11 : 10;

        // Visual pull-over: when a siren-on EV is nearby in the same lane and going the
        // same direction, nudge this vehicle perpendicular-right toward the shoulder.
        double pullX = 0, pullY = 0;
        if (!(v instanceof EmergencyVehicle) && !inRing && v.getLane() != null) {
            for (Vehicle other : v.getLane().getVehicles()) {
                if (!(other instanceof EmergencyVehicle ev) || !ev.isSirenOn()) continue;
                if (other == v) continue;
                if (Math.hypot(other.getX() - v.getX(), other.getY() - v.getY())
                        < trafficsim.SimConstants.EV_PULLOVER_RANGE) {
                    pullX = v.getDirection().rightX() * trafficsim.SimConstants.EV_PULLOVER_NUDGE;
                    pullY = v.getDirection().rightY() * trafficsim.SimConstants.EV_PULLOVER_NUDGE;
                    break;
                }
            }
        }

        Color body;
        if (v instanceof Truck) body = TRUCK_COL;
        else if (v instanceof Bus) body = BUS_COL;
        else if (v instanceof EmergencyVehicle) body = new Color(0xF4, 0xF4, 0xF0);
        else {
            int h = System.identityHashCode(v);
            body = switch (Math.floorMod(h, 4)) {
                case 0 -> CAR_A; case 1 -> CAR_B; case 2 -> CAR_C; default -> CAR_D;
            };
        }

        AffineTransform saved = g2.getTransform();
        // Always use the vehicle's continuously-tracked orientation — smooth for both
        // circulating and turning vehicles, matches the discrete direction otherwise.
        double theta = v.getOrientation();
        g2.translate(v.getX() + pullX, v.getY() + pullY);
        g2.rotate(theta);

        int w = longSide, h = shortSide;
        int x = -w / 2, y = -h / 2;

        // Shadow
        g2.setColor(VEHICLE_SHADOW);
        g2.fillRoundRect(x + 2, y + 2, w, h, 3, 3);
        // Body — at night, brighten the body slightly so the vehicle stays legible on dark asphalt
        double n = nightAmount();
        Color paintBody = body;
        if (n > 0.4) {
            int lift = (int) (n * 30);
            paintBody = new Color(
                    Math.min(255, body.getRed() + lift),
                    Math.min(255, body.getGreen() + lift),
                    Math.min(255, body.getBlue() + lift));
        }
        g2.setColor(paintBody);
        g2.fillRoundRect(x, y, w, h, 3, 3);
        // Windshield at nose (right side after rotation)
        g2.setColor(WINDSHIELD);
        g2.fillRect(x + w - 5, y + 1, 3, h - 2);
        // Rear window (smaller, slightly darker)
        g2.setColor(new Color(0x10, 0x18, 0x22, 180));
        g2.fillRect(x + 2, y + 1, 2, h - 2);
        // Headlights at nose
        g2.setColor(HEADLIGHT);
        g2.fillOval(x + w - 2, y, 2, 2);
        g2.fillOval(x + w - 2, y + h - 2, 2, 2);

        // Brake lights when stopped or braking
        if (v.isBraking()) {
            g2.setColor(BRAKE_LIGHT);
            g2.fillRect(x, y + 1, 2, 2);
            g2.fillRect(x, y + h - 3, 2, 2);
        }

        // Turn signal blinker
        Direction pending = v.peekPendingDirection();
        if (pending != null && (tick / 6) % 2 == 0) {
            g2.setColor(TURN_BLINK);
            boolean rightTurn = pending == turnRight(v.getDirection());
            if (rightTurn) g2.fillRect(x + w - 3, y + h - 2, 3, 2);
            else           g2.fillRect(x + w - 3, y, 3, 2);
        }

        // Emergency lightbar across roof
        if (v instanceof EmergencyVehicle ev && ev.isSirenOn()) {
            boolean flashA = (tick / 4) % 2 == 0;
            int barY = -1;
            int barLen = w - 6;
            g2.setColor(flashA ? EV_A : EV_B);
            g2.fillRect(-barLen / 2, barY, barLen / 2, 2);
            g2.setColor(flashA ? EV_B : EV_A);
            g2.fillRect(0, barY, barLen - barLen / 2, 2);
        }

        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawRoundRect(x, y, w - 1, h - 1, 3, 3);

        g2.setTransform(saved);

        // Highlight halo (drawn without rotation)
        if (highlightEmergency && v instanceof EmergencyVehicle) {
            g2.setColor(new Color(0xFF, 0xE0, 0x40, 140));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval((int) v.getX() - 14, (int) v.getY() - 14, 28, 28);
        }
    }

    private static Direction turnRight(Direction d) {
        return switch (d) {
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            case NORTH -> Direction.EAST;
        };
    }

    // -- pedestrians ---------------------------------------------------------

    private void drawPedestrians(Graphics2D g2) {
        for (Pedestrian p : engine.getPedestrians().getAll()) drawPed(g2, p.getX(), p.getY(), p.getShirt(), p.getPants());
        for (CrosswalkPedestrian p : engine.getPedestrians().getCrossers()) drawPed(g2, p.getX(), p.getY(), p.getShirt(), p.getPants());
    }

    private void drawPed(Graphics2D g2, double dx, double dy, Color shirt, Color pants) {
        int px = (int) dx, py = (int) dy;
        g2.setColor(new Color(0, 0, 0, 110));
        g2.fillOval(px - 3, py + 3, 9, 4);
        g2.setColor(pants);
        g2.fillRect(px - 3, py, 6, 7);
        g2.setColor(shirt);
        g2.fillRect(px - 4, py - 7, 8, 7);
        g2.setColor(new Color(0xD8, 0xB0, 0x92));
        g2.fillOval(px - 3, py - 11, 6, 6);
    }

    // -- day/night -----------------------------------------------------------

    private double nightAmount() {
        // 0 = full day, 1 = full night. Sinusoidal over CYCLE_TICKS.
        double phase = (engine.getTickCount() % SimConstants.CYCLE_TICKS) / (double) SimConstants.CYCLE_TICKS;
        double val = (1 - Math.cos(phase * 2 * Math.PI)) / 2.0; // 0..1
        return val;
    }

    private void drawNightTint(Graphics2D g2) {
        double n = nightAmount();
        if (n < 0.05) return;
        int alpha = (int) (n * 100);
        g2.setColor(new Color(10, 15, 40, alpha));
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    /**
     * Street lamps at every intersection corner cast warm pools of light on the asphalt;
     * moving vehicles at night get a small headlight cone in their direction of travel.
     * Only draws if night is at least partly on.
     */
    private void drawStreetLampsAndHeadlights(Graphics2D g2) {
        double n = nightAmount();
        if (n < 0.35) return; // lamps only glow at meaningful night
        Composite saved = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1.0, (n - 0.2) * 1.6)));

        // Street lamp glow: light pool at each intersection corner
        Color glow = new Color(0xFF, 0xE4, 0x8C, 90);
        Color glowInner = new Color(0xFF, 0xF0, 0xB4, 160);
        for (Intersection ix : engine.getNetwork().getIntersections()) {
            int cx = ix.getX(), cy = ix.getY();
            int off = INT_HALF + 8;
            drawLamp(g2, cx - off, cy - off, glow, glowInner);
            drawLamp(g2, cx + off, cy - off, glow, glowInner);
            drawLamp(g2, cx - off, cy + off, glow, glowInner);
            drawLamp(g2, cx + off, cy + off, glow, glowInner);
        }

        // Headlights are ALWAYS on for every vehicle — alpha scales with speed so
        // stopped cars have a dim "parking light" glow and fast cars a bright cone.
        for (Road road : engine.getNetwork().getRoads())
            for (Lane lane : road.getLanes())
                for (Vehicle v : lane.getVehicles()) drawHeadlightCone(g2, v);
        for (Intersection ix : engine.getNetwork().getIntersections())
            if (ix instanceof Roundabout ring)
                for (Vehicle v : ring.getInRing()) drawHeadlightCone(g2, v);
        for (Vehicle v : engine.getTurningVehicles()) drawHeadlightCone(g2, v);

        g2.setComposite(saved);
    }

    private void drawLamp(Graphics2D g2, int x, int y, Color glow, Color glowInner) {
        g2.setColor(glow);
        g2.fillOval(x - 22, y - 22, 44, 44);
        g2.setColor(glowInner);
        g2.fillOval(x - 10, y - 10, 20, 20);
        g2.setColor(new Color(0x1E, 0x1E, 0x22, 220));
        g2.fillRect(x - 1, y - 1, 2, 2); // tiny lamp fixture
    }

    private void drawHeadlightCone(Graphics2D g2, Vehicle v) {
        double a = v.getOrientation();
        double cx = v.getX() + Math.cos(a) * v.getLength() / 2.0;
        double cy = v.getY() + Math.sin(a) * v.getLength() / 2.0;
        // Scale cone length + alpha by speed. Stopped car → tiny dim glow, moving car → full cone.
        double speedFrac = Math.min(1.0, v.getSpeed() / 5.0);
        int reach = 12 + (int) (speedFrac * 26);
        double spread = 0.45 + speedFrac * 0.15;
        int alpha = 40 + (int) (speedFrac * 55);
        int p1x = (int) (cx + Math.cos(a - spread) * reach);
        int p1y = (int) (cy + Math.sin(a - spread) * reach);
        int p2x = (int) (cx + Math.cos(a + spread) * reach);
        int p2y = (int) (cy + Math.sin(a + spread) * reach);
        Polygon cone = new Polygon(new int[] { (int) cx, p1x, p2x }, new int[] { (int) cy, p1y, p2y }, 3);
        g2.setColor(new Color(0xFF, 0xF4, 0xB4, alpha));
        g2.fill(cone);
    }

    // -- HUD -----------------------------------------------------------------

    private void drawHud(Graphics2D g2) {
        List<Vehicle> all = new ArrayList<>();
        int totalTurns = 0;
        for (Road r : engine.getNetwork().getRoads())
            for (Lane l : r.getLanes())
                for (Vehicle v : l.getVehicles()) { all.add(v); totalTurns += v.getTurnCount(); }

        double avg = Statistics.averageSpeed(all);
        long stopped = Statistics.stoppedCount(all);

        int panelW = 260, panelH = 138;
        g2.setColor(new Color(0, 0, 0, 165));
        g2.fillRoundRect(10, 10, panelW, panelH, 10, 10);
        g2.setColor(new Color(255, 255, 255, 30));
        g2.drawRoundRect(10, 10, panelW, panelH, 10, 10);

        g2.setColor(new Color(0xF6, 0xF6, 0xF0));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        String label = String.format("TrafficSim  ·  tick %d  ·  %s",
                engine.getTickCount(), nightAmount() > 0.5 ? "night" : "day");
        g2.drawString(label, 22, 30);
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        int row = 50, step = 14;
        g2.drawString(String.format("avg speed   %5.2f", avg),      22, row); row += step;
        g2.drawString(String.format("vehicles    %5d",  all.size()), 22, row); row += step;
        g2.drawString(String.format("stopped     %5d",  stopped),    22, row); row += step;
        g2.drawString(String.format("turns       %5d",  totalTurns), 22, row); row += step;

        // Rolling avg-speed strip chart
        drawSpeedChart(g2, 22, row - 2, panelW - 32, 24);
    }

    private void drawSpeedChart(Graphics2D g2, int x, int y, int w, int h) {
        double[] hist = engine.getSpeedHistory();
        int head = engine.getHistoryHead();
        g2.setColor(new Color(255, 255, 255, 40));
        g2.drawRect(x, y, w, h);
        double max = 6.0;
        for (int i = 0; i < hist.length; i++) {
            int idx = (head + i) % hist.length;
            double v = Math.min(max, hist[idx]);
            int barH = (int) (v / max * h);
            int bx = x + 1 + (int) ((i / (double) hist.length) * (w - 2));
            int bw = Math.max(1, (w - 2) / hist.length);
            g2.setColor(new Color(0x50, 0xD8, 0x64, 200));
            g2.fillRect(bx, y + h - barH, bw, barH);
        }
    }

    // -- helpers -------------------------------------------------------------
    private List<Road> horizontals() {
        return engine.getNetwork().getRoads().stream()
                .filter(Road::isHorizontal)
                .sorted((a, b) -> Integer.compare(a.getY1(), b.getY1())).toList();
    }
    private List<Road> verticals() {
        return engine.getNetwork().getRoads().stream()
                .filter(Road::isVertical)
                .sorted((a, b) -> Integer.compare(a.getX1(), b.getX1())).toList();
    }
}
