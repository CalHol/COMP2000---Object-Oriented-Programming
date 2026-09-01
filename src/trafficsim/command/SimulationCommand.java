package trafficsim.command;

import trafficsim.engine.SimulationEngine;

/**
 * Command pattern: a request to the {@link SimulationEngine} expressed as an
 * object so it can be queued, logged, or undone. UI controls submit commands
 * via {@link SimulationEngine#submit(SimulationCommand)}; the engine drains
 * the queue at the start of each tick.
 *
 * <p>Sealed so the engine's dispatch can be exhaustive and pattern-matched.
 */
public sealed interface SimulationCommand
        permits PauseCommand, ResumeCommand, StepOnceCommand, ResetCommand, SetTickRateCommand, SpawnOneCommand {

    void execute(SimulationEngine engine);
}
