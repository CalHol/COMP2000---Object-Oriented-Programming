package trafficsim.strategy;

import trafficsim.util.Direction;
import trafficsim.strategy.WeightedRandom;

import java.util.List;

/**
 * Non-committal driver — picks a direction with a slight bias toward "straight"
 * (the first entry of {@code options}). Not as opinionated as
 * {@link StraightPreferredTurnStrategy}, but not truly uniform either — real
 * drivers rarely U-turn or side-street at every crossing.
 *
 * <p>Rebalanced 2026-09-01 after the "cars disappear at intersections" review:
 * with a uniform 25% turn chance per crossing, half of every queue vanished
 * onto a perpendicular road each cycle.
 */
public class RandomTurnStrategy implements TurnStrategy {

    @Override
    public Direction chooseDirection(List<Direction> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("no direction options");
        }
        Direction straight = options.get(0);
        WeightedRandom<Direction> picker = new WeightedRandom<>();
        picker.add(straight, 60);
        for (int i = 1; i < options.size(); i++) {
            Direction opt = options.get(i);
            if (opt == straight.opposite()) continue; // no U-turns
            picker.add(opt, 20);
        }
        return picker.pick();
    }
}
