package trafficsim.strategy;

import trafficsim.util.Direction;
import trafficsim.strategy.WeightedRandom;

import java.util.List;

/**
 * Turn strategy that biases heavily toward continuing straight — 70% straight,
 * 15% left, 15% right, 0% U-turn. Together with {@link RandomTurnStrategy}
 * this gives spawned vehicles two visibly different driving personalities and
 * lets road load emerge unevenly (a route-preferring driver hammers the same
 * arterial road, a random-turner spreads over side streets).
 */
public class StraightPreferredTurnStrategy implements TurnStrategy {

    @Override
    public Direction chooseDirection(List<Direction> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("no direction options");
        }
        Direction straight = options.get(0); // first element = current direction

        WeightedRandom<Direction> picker = new WeightedRandom<>();
        picker.add(straight, 82);
        for (int i = 1; i < options.size(); i++) {
            Direction opt = options.get(i);
            if (opt == straight.opposite()) continue; // no U-turns
            picker.add(opt, 9);
        }
        return picker.pick();
    }
}
