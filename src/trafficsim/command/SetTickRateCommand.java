package trafficsim.command;

import trafficsim.engine.SimulationEngine;

public final class SetTickRateCommand implements SimulationCommand {

    private final int tickRateHz;

    public SetTickRateCommand(int tickRateHz) {
        if (tickRateHz < 1 || tickRateHz > 120) {
            throw new IllegalArgumentException("tickRateHz must be 1..120, got " + tickRateHz);
        }
        this.tickRateHz = tickRateHz;
    }

    @Override
    public void execute(SimulationEngine engine) {
        engine.setTickRate(tickRateHz);
    }
}
