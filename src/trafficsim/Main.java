package trafficsim;

import trafficsim.engine.SimulationEngine;
import trafficsim.factory.NetworkLoader;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.vehicle.Vehicle;
import trafficsim.view.MainFrame;

import javax.swing.SwingUtilities;
import java.io.File;

public class Main {

    private static final String DEFAULT_GRID = "networks/grid.txt";

    /**
     * Args:
     *   [network-file]      optional path; falls back to networks/grid.txt or buildDefault()
     *   --seed=<long>       optional deterministic RNG seed for shared noise
     */
    public static void main(String[] args) {
        String networkPath = null;
        Long seed = null;
        for (String a : args) {
            if (a.startsWith("--seed=")) seed = Long.parseLong(a.substring("--seed=".length()));
            else if (!a.startsWith("--")) networkPath = a;
        }
        if (seed != null) Vehicle.seedNoise(seed);

        RoadNetwork network;
        if (networkPath != null) network = NetworkLoader.loadFromFile(networkPath);
        else if (new File(DEFAULT_GRID).exists()) network = NetworkLoader.loadFromFile(DEFAULT_GRID);
        else network = NetworkLoader.buildDefault();

        SimulationEngine engine = new SimulationEngine(network, 20);

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(engine);
            frame.setVisible(true);
            engine.run();
        });
    }
}
