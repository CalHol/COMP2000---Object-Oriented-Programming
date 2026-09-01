package trafficsim.command;

import trafficsim.engine.SimulationEngine;

public final class ResumeCommand implements SimulationCommand {
    @Override
    public void execute(SimulationEngine engine) {
        engine.setPaused(false);
    }
}
