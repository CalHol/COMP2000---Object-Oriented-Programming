package trafficsim.view;

import trafficsim.engine.SimulationEngine;
import trafficsim.engine.SimulationObserver;
import trafficsim.model.road.Intersection;
import trafficsim.model.road.Lane;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.vehicle.Vehicle;
import trafficsim.util.LightPhase;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

// [Pair A] Henry
public class SimulationDisplay extends JPanel implements SimulationObserver {

    private static final Color BACKGROUND    = new Color(232, 232, 228);
    private static final Color ROAD_COLOR    = new Color(60, 60, 65);
    private static final Color LANE_LINE     = new Color(220, 220, 220);
    private static final Color VEHICLE_COLOR = new Color(30, 90, 200);
    private static final Color LIGHT_GREEN   = new Color(0, 180, 60);
    private static final Color LIGHT_YELLOW  = new Color(230, 190, 0);
    private static final Color LIGHT_RED     = new Color(210, 40, 40);
    private static final Color OUTLINE       = new Color(20, 20, 20);

    private final SimulationEngine engine;

    public SimulationDisplay(SimulationEngine engine) {
        this.engine = engine;
        engine.addObserver(this);
        setBackground(BACKGROUND);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

            RoadNetwork network = engine.getNetwork();
            if (network == null) return;

            drawRoads(g2, network);
            drawIntersections(g2, network);
            drawVehicles(g2, network);
        } finally {
            g2.dispose();
        }
    }

    private void drawRoads(Graphics2D g2, RoadNetwork network) {
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(ROAD_COLOR);
        for (Road road : network.getRoads()) {
            g2.drawLine(road.getX1(), road.getY1(), road.getX2(), road.getY2());
        }
        // dashed centre line on top
        float[] dash = { 6f, 6f };
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, dash, 0f));
        g2.setColor(LANE_LINE);
        for (Road road : network.getRoads()) {
            g2.drawLine(road.getX1(), road.getY1(), road.getX2(), road.getY2());
        }
        g2.setStroke(old);
    }

    private void drawIntersections(Graphics2D g2, RoadNetwork network) {
        for (Intersection i : network.getIntersections()) {
            int r = 10;
            int x = i.getX() - r;
            int y = i.getY() - r;
            g2.setColor(colorForLight(i));
            g2.fillOval(x, y, r * 2, r * 2);
            g2.setColor(OUTLINE);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(x, y, r * 2, r * 2);
        }
    }

    private Color colorForLight(Intersection i) {
        if (i.getLight() == null) return Color.GRAY;
        LightPhase phase = i.getLight().getPhase();
        if (phase == null) return Color.GRAY;
        switch (phase) {
            case GREEN:  return LIGHT_GREEN;
            case YELLOW: return LIGHT_YELLOW;
            case RED:    return LIGHT_RED;
            default:     return Color.GRAY;
        }
    }

    private void drawVehicles(Graphics2D g2, RoadNetwork network) {
        g2.setColor(VEHICLE_COLOR);
        for (Road road : network.getRoads()) {
            for (Lane lane : road.getLanes()) {
                for (Vehicle v : lane.getVehicles()) {
                    // small rectangle centred on the vehicle's position
                    g2.fillRect(v.getX() - 4, v.getY() - 3, 8, 6);
                }
            }
        }
    }

    @Override
    public void onSimulationStep() {
        // Swing components must be updated on the EDT
        SwingUtilities.invokeLater(this::repaint);
    }
}
