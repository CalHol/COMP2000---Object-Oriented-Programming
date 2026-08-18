package trafficsim.strategy;

import trafficsim.util.Direction;
import java.util.List;

// TODO: [Pair C] — Ben + Jacob
public class RandomTurnStrategy implements TurnStrategy {
    private WeightedRandom<Direction> picker = new WeightedRandom<>();

    @Override
    public Direction chooseDirection(List<Direction> available) {
        // TODO: use picker to choose a direction from available
        return null;
    }
}
