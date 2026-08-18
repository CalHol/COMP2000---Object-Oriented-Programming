package trafficsim.strategy;

import trafficsim.util.Direction;
import java.util.List;

// TODO: [Pair C] — Ben + Jacob
public interface TurnStrategy {
    Direction chooseDirection(List<Direction> available);
}
