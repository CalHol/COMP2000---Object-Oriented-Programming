package trafficsim.command;

import trafficsim.engine.SimulationEngine;

public final class SpawnOneCommand implements SimulationCommand {
    @Override
    public void execute(SimulationEngine engine) {
        engine.forceSpawn();
    }
}
