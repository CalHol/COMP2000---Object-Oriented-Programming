package trafficsim;

import trafficsim.engine.SimulationEngine;
import trafficsim.engine.VehicleSpawner;
import trafficsim.factory.NetworkLoader;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.vehicle.Vehicle;
import trafficsim.view.SimulationDisplay;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.security.SecureRandom;
import java.util.Random;

public class Main {

    private static final String DEFAULT_GRID = "networks/grid.txt";

    /**
     * Args:
     *   [network-file]      optional path; falls back to networks/grid.txt or buildDefault()
     *   --seed=<long>       optional deterministic seed for the whole run
     */
    public static void main(String[] args) {
        String networkPath = null;
        Long seed = null;
        for (String a : args) {
            if (a.startsWith("--seed=")) seed = Long.parseLong(a.substring("--seed=".length()));
            else if (!a.startsWith("--")) networkPath = a;
        }
        long runSeed = seed != null ? seed : new SecureRandom().nextLong();
        // Derive a separate repeatable stream for small driver slowdowns.
        Vehicle.seedNoise(runSeed ^ 0x9E3779B97F4A7C15L);

        RoadNetwork network;
        if (networkPath != null) network = NetworkLoader.loadFromFile(networkPath);
        else if (new File(DEFAULT_GRID).exists()) network = NetworkLoader.loadFromFile(DEFAULT_GRID);
        else network = NetworkLoader.buildDefault();

        VehicleSpawner spawner = new VehicleSpawner(
                new Random(runSeed), SimConstants.SPAWN_PROBABILITY);
        for (int i = 0; i < 6; i++) spawner.spawnOne(network);
        SimulationEngine engine = new SimulationEngine(
                network, 30, spawner, runSeed);

        SwingUtilities.invokeLater(() -> {
            SimulationDisplay display = new SimulationDisplay(engine);
            engine.addObserver(display);
            display.setPreferredSize(new java.awt.Dimension(1100, 700));

            JFrame frame = new JFrame("Traffic Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(display);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    engine.shutdown();
                }
            });
            frame.setVisible(true);

            engine.run();
        });
    }
}
