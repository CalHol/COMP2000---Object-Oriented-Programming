package trafficsim.command;

import trafficsim.engine.SimulationEngine;

public final class ResetCommand implements SimulationCommand {
    @Override
    public void execute(SimulationEngine engine) {
        engine.reset();
    }
}
