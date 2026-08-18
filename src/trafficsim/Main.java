package trafficsim;

import trafficsim.engine.SimulationEngine;
import trafficsim.factory.NetworkLoader;
import trafficsim.model.road.RoadNetwork;
import trafficsim.view.SimulationDisplay;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        RoadNetwork network = new NetworkLoader().buildDefault();
        SimulationEngine engine = new SimulationEngine(network, 60);

        SwingUtilities.invokeLater(() -> {
            SimulationDisplay display = new SimulationDisplay(engine);
            display.setPreferredSize(new java.awt.Dimension(800, 500));

            JFrame frame = new JFrame("Traffic Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(display);
            frame.pack();
            frame.setVisible(true);

            engine.run();
        });
    }
}
