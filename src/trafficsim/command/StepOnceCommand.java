package trafficsim.command;

import trafficsim.engine.SimulationEngine;

/** Advance the sim by one tick even when paused. */
public final class StepOnceCommand implements SimulationCommand {
    @Override
    public void execute(SimulationEngine engine) {
        engine.forceStepOnce();
    }
}
