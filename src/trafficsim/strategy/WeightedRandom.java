package trafficsim.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// TODO: [Pair C] — Ben + Jacob
public class WeightedRandom<T> {
    private List<T> items = new ArrayList<>();
    private List<Double> weights = new ArrayList<>();
    private Random random = new Random();

    public void add(T item, double weight) {
        // TODO: add item and weight to their respective lists
    }

    public T pick() {
        // TODO: pick a random item weighted by probability
        return null;
    }
}
