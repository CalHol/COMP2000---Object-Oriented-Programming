package trafficsim.view;

import trafficsim.engine.SimulationEngine;
import trafficsim.engine.SimulationObserver;
import trafficsim.engine.Statistics;
import trafficsim.model.road.BusStop;
import trafficsim.model.road.Intersection;
import trafficsim.model.road.Lane;
import trafficsim.model.road.Road;
import trafficsim.model.vehicle.Bus;
import trafficsim.model.vehicle.EmergencyVehicle;
import trafficsim.model.vehicle.Truck;
import trafficsim.model.vehicle.Vehicle;
import trafficsim.util.Direction;
import trafficsim.util.LightPhase;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial") // Swing panels are not persisted by this application.
public final class SimulationDisplay extends JPanel implements SimulationObserver {
    private static final long serialVersionUID = 1L;

    private static final Color GRASS = new Color(34, 82, 62);
    private static final Color ROAD = new Color(49, 54, 61);
    private static final Color ROAD_EDGE = new Color(25, 29, 34);
    private static final Color LANE_MARKING = new Color(230, 215, 160);
    private static final Color[] CAR_COLOURS = {
            new Color(235, 91, 91), new Color(79, 184, 235),
            new Color(117, 213, 132), new Color(195, 119, 232),
            new Color(244, 157, 78), new Color(78, 211, 199),
            new Color(238, 112, 175), new Color(126, 131, 238)
    };

    private final SimulationEngine engine;
    private final Map<Vehicle, Color> vehicleColours = new IdentityHashMap<>();

    public SimulationDisplay(SimulationEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("Simulation engine cannot be null");
        }
        this.engine = engine;
        setBackground(GRASS);
        setFocusable(true);
        installKeyBindings();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphics = (Graphics2D) g.create();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        synchronized (engine.getNetwork()) {
            List<Vehicle> vehicles = allVehicles();
            vehicleColours.keySet().retainAll(vehicles);
            drawCityBlocks(graphics);
            drawRoads(graphics);
            drawBusStops(graphics);
            drawIntersections(graphics);
            drawVehicles(graphics, vehicles);
            drawDashboard(graphics, vehicles);
        }
        graphics.dispose();
    }

    @Override
    public void onSimulationStep() {
        repaint();
    }

    private void drawCityBlocks(Graphics2D graphics) {
        int[][] blocks = {
                {75, 205, 145, 110}, {280, 205, 240, 110},
                {580, 205, 240, 110}, {880, 205, 145, 110},
                {75, 385, 145, 110}, {280, 385, 240, 110},
                {580, 385, 240, 110}, {880, 385, 145, 110}
        };
        for (int index = 0; index < blocks.length; index++) {
            int[] block = blocks[index];
            graphics.setColor(new Color(96, 105, 101));
            graphics.fillRoundRect(block[0] - 6, block[1] - 6, block[2] + 12, block[3] + 12, 14, 14);
            graphics.setColor(index % 3 == 0 ? new Color(73, 105, 86) : new Color(80, 88, 98));
            graphics.fillRoundRect(block[0], block[1], block[2], block[3], 10, 10);
            graphics.setColor(new Color(244, 207, 102, 150));
            for (int x = block[0] + 16; x < block[0] + block[2] - 8; x += 28) {
                for (int y = block[1] + 15; y < block[1] + block[3] - 8; y += 25) {
                    graphics.fillRoundRect(x, y, 9, 7, 2, 2);
                }
            }
        }
    }

    private void drawRoads(Graphics2D graphics) {
        Stroke originalStroke = graphics.getStroke();
        for (Road road : engine.getNetwork().getRoads()) {
            graphics.setColor(ROAD_EDGE);
            graphics.setStroke(new BasicStroke(38, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            graphics.drawLine(road.getX1(), road.getY1(), road.getX2(), road.getY2());
            graphics.setColor(ROAD);
            graphics.setStroke(new BasicStroke(32, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            graphics.drawLine(road.getX1(), road.getY1(), road.getX2(), road.getY2());
            graphics.setColor(LANE_MARKING);
            graphics.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, new float[]{9.0f, 9.0f}, 0.0f));
            graphics.drawLine(road.getX1(), road.getY1(), road.getX2(), road.getY2());
        }
        graphics.setStroke(originalStroke);
    }

    private void drawIntersections(Graphics2D graphics) {
        for (Intersection intersection : engine.getNetwork().getIntersections()) {
            int x = intersection.getX();
            int y = intersection.getY();
            graphics.setColor(ROAD);
            graphics.fillRect(x - 19, y - 19, 38, 38);

            LightPhase horizontalPhase = intersection.getLight().phaseFor(Direction.EAST);
            LightPhase verticalPhase = intersection.getLight().phaseFor(Direction.NORTH);
            Color horizontal = signalColor(horizontalPhase == LightPhase.GREEN, horizontalPhase);
            Color vertical = signalColor(verticalPhase == LightPhase.GREEN, verticalPhase);
            drawSignal(graphics, x - 28, y - 28, horizontal);
            drawSignal(graphics, x + 18, y + 18, horizontal);
            drawSignal(graphics, x + 18, y - 28, vertical);
            drawSignal(graphics, x - 28, y + 18, vertical);
        }
    }

    private void drawBusStops(Graphics2D graphics) {
        for (BusStop stop : engine.getNetwork().getBusStops()) {
            drawBusStop(graphics, stop, stop.getDirection());
        }
    }

    private void drawBusStop(Graphics2D graphics, BusStop stop, Direction direction) {
            int[] position = stop.getPosition();
            int shelterX = position[0];
            int shelterY = position[1];
            if (isHorizontal(direction)) {
                shelterY += direction == Direction.EAST ? -31 : 31;
            } else {
                // Southbound traffic is on the right side of a vertical road; northbound is on the left.
                shelterX += direction == Direction.SOUTH ? 31 : -31;
            }

            graphics.setColor(new Color(210, 220, 222));
            graphics.setStroke(new BasicStroke(2));
            graphics.drawLine(position[0], position[1], shelterX, shelterY);
            graphics.setColor(new Color(31, 43, 51));
            graphics.fillRoundRect(shelterX - 17, shelterY - 10, 34, 20, 5, 5);
            graphics.setColor(new Color(63, 158, 205));
            graphics.fillRect(shelterX - 14, shelterY - 7, 28, 11);
            graphics.setColor(new Color(255, 205, 59));
            graphics.fillRoundRect(shelterX - 12, shelterY - 15, 24, 9, 4, 4);
            graphics.setColor(new Color(28, 35, 39));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 7));
            graphics.drawString("BUS", shelterX - 8, shelterY - 8);
            graphics.setColor(new Color(235, 240, 241));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));
            int labelWidth = graphics.getFontMetrics().stringWidth(stop.getName());
            graphics.drawString(stop.getName(), shelterX - labelWidth / 2, shelterY + 19);
    }

    private Color signalColor(boolean canProceed, LightPhase phase) {
        if (phase == LightPhase.YELLOW) {
            return new Color(255, 190, 45);
        }
        return canProceed ? new Color(63, 220, 105) : new Color(242, 72, 72);
    }

    private void drawSignal(Graphics2D graphics, int x, int y, Color color) {
        graphics.setColor(new Color(19, 22, 25));
        graphics.fillRoundRect(x - 2, y - 2, 14, 14, 5, 5);
        graphics.setColor(color);
        graphics.fillOval(x, y, 10, 10);
    }

    private void drawVehicles(Graphics2D graphics, List<Vehicle> vehicles) {
        for (Vehicle vehicle : vehicles) {
            int[] size = vehicleSize(vehicle);
            int width = size[0];
            int height = size[1];
            int vehicleX = (int) Math.round(vehicle.getX());
            int vehicleY = (int) Math.round(vehicle.getY());
            int x = vehicleX - width / 2;
            int y = vehicleY - height / 2;

            graphics.setColor(vehicleColor(vehicle));
            graphics.fillRoundRect(x, y, width, height, 5, 5);
            if (vehicle instanceof Bus) {
                drawBusWindows(graphics, vehicle, x, y, width, height);
            } else if (vehicle instanceof Truck) {
                graphics.setColor(new Color(232, 226, 245, 175));
                if (isHorizontal(vehicle.getDirection())) {
                    graphics.fillRect(x + width - 8, y + 3, 5, height - 6);
                } else {
                    graphics.fillRect(x + 3, y + height - 8, width - 6, 5);
                }
            } else {
                graphics.setColor(new Color(255, 255, 255, 150));
                graphics.fillRoundRect(x + 4, y + 4, width - 8, height - 8, 3, 3);
            }
            if (vehicle instanceof EmergencyVehicle && ((EmergencyVehicle) vehicle).isSirenOn()) {
                graphics.setColor(new Color(235, 60, 60));
                graphics.fillRect(vehicleX - 5, vehicleY - 2, 5, 4);
                graphics.setColor(new Color(70, 145, 245));
                graphics.fillRect(vehicleX, vehicleY - 2, 5, 4);
            }
        }
    }

    private int[] vehicleSize(Vehicle vehicle) {
        boolean horizontal = isHorizontal(vehicle.getDirection());
        if (vehicle instanceof Bus) {
            return horizontal ? new int[]{32, 16} : new int[]{16, 32};
        }
        if (vehicle instanceof Truck) {
            return horizontal ? new int[]{25, 16} : new int[]{16, 25};
        }
        return new int[]{16, 16};
    }

    private void drawBusWindows(Graphics2D graphics, Vehicle vehicle, int x, int y, int width, int height) {
        graphics.setColor(new Color(235, 247, 250, 190));
        if (isHorizontal(vehicle.getDirection())) {
            for (int windowX = x + 4; windowX < x + width - 4; windowX += 8) {
                graphics.fillRoundRect(windowX, y + 4, 5, height - 8, 2, 2);
            }
        } else {
            for (int windowY = y + 4; windowY < y + height - 4; windowY += 8) {
                graphics.fillRoundRect(x + 4, windowY, width - 8, 5, 2, 2);
            }
        }
    }

    private Color vehicleColor(Vehicle vehicle) {
        if (vehicle instanceof EmergencyVehicle) return new Color(245, 245, 245);
        if (vehicle instanceof Bus) return new Color(255, 183, 50);
        if (vehicle instanceof Truck) return new Color(165, 120, 205);
        Color colour = vehicleColours.get(vehicle);
        if (colour == null) {
            colour = CAR_COLOURS[vehicleColours.size() % CAR_COLOURS.length];
            vehicleColours.put(vehicle, colour);
        }
        return colour;
    }

    private void drawDashboard(Graphics2D graphics, List<Vehicle> vehicles) {
        graphics.setColor(new Color(12, 18, 23, 215));
        graphics.fillRoundRect(18, 16, 324, 92, 14, 14);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        graphics.drawString("CITY FLOW", 32, 40);
        graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        String state = engine.isPaused() ? "PAUSED" : "RUNNING";
        graphics.drawString(String.format("%s  tick %,d  vehicles %d", state,
                engine.getTickCount(), vehicles.size()), 32, 59);
        graphics.drawString(String.format("avg speed %.2f  stopped %d",
                Statistics.averageSpeed(vehicles), Statistics.stoppedCount(vehicles)), 32, 76);
        graphics.drawString(String.format("run seed %016X",
                engine.getRunSeed()), 32, 94);

        graphics.setColor(new Color(12, 18, 23, 185));
        graphics.fillRoundRect(18, getHeight() - 45, 170, 28, 12, 12);
        graphics.setColor(new Color(225, 232, 235));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        graphics.drawString("SPACE pause/resume", 31, getHeight() - 26);

    }

    private void installKeyBindings() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        inputMap.put(KeyStroke.getKeyStroke("SPACE"), "toggle-running");
        actionMap.put("toggle-running", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                engine.setPaused(!engine.isPaused());
                repaint();
            }
        });
    }

    private List<Vehicle> allVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();
        for (Road road : engine.getNetwork().getRoads()) {
            for (Lane lane : road.getLanes()) {
                vehicles.addAll(lane.getVehicles());
            }
        }
        return vehicles;
    }

    private static boolean isHorizontal(Direction direction) {
        return direction == Direction.EAST || direction == Direction.WEST;
    }
}
