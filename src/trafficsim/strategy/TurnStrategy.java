package trafficsim.strategy;

import trafficsim.util.Direction;

import java.util.List;

public interface TurnStrategy {
    Direction chooseDirection(List<Direction> options);
}
