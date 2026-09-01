package trafficsim.command;

import trafficsim.engine.SimulationEngine;

public final class PauseCommand implements SimulationCommand {
    @Override
    public void execute(SimulationEngine engine) {
        engine.setPaused(true);
    }
}
