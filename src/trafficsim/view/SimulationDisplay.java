package trafficsim.view;

import trafficsim.engine.SimulationEngine;
import trafficsim.engine.SimulationObserver;
import javax.swing.JPanel;
import java.awt.Graphics;

// TODO: [Pair A] — Henry + TBD
public class SimulationDisplay extends JPanel implements SimulationObserver {
    private SimulationEngine engine;

    public SimulationDisplay(SimulationEngine engine) {
        this.engine = engine;
        engine.addObserver(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // TODO: draw roads, vehicles, and traffic lights from engine.getNetwork()
    }

    @Override
    public void onSimulationStep() {
        repaint();
    }
}
