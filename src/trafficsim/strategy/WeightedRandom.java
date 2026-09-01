package trafficsim.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WeightedRandom<T> {
    private final List<T> items = new ArrayList<>();
    private final List<Double> weights = new ArrayList<>();
    private final Random rng;
    private double totalWeight = 0.0;

    public WeightedRandom() {
        this(new Random());
    }

    public WeightedRandom(Random rng) {
        this.rng = rng;
    }

    public void add(T item, double weight) {
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be positive");
        }
        items.add(item);
        weights.add(weight);
        totalWeight += weight;
    }

    public T pick() {
        if (items.isEmpty()) {
            throw new IllegalStateException("no items to pick");
        }
        double roll = rng.nextDouble() * totalWeight;
        double cumulative = 0.0;
        for (int i = 0; i < items.size(); i++) {
            cumulative += weights.get(i);
            if (roll < cumulative) {
                return items.get(i);
            }
        }
        return items.get(items.size() - 1);
    }

    public int size() {
        return items.size();
    }
}
