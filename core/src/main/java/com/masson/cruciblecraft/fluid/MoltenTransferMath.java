package com.masson.cruciblecraft.fluid;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Bootstrap-free arithmetic used by molten fluid capabilities.
 */
public final class MoltenTransferMath {
    public static final int MILLIBUCKETS_PER_INGOT = 144;

    private MoltenTransferMath() {}

    public static int planFill(int requested, int availableCapacity, int compositionQuantum) {
        if (requested <= 0 || availableCapacity <= 0 || compositionQuantum <= 0) {
            return 0;
        }
        int limit = Math.min(requested, availableCapacity);
        return limit - limit % compositionQuantum;
    }

    /**
     * Plans a ratio-preserving removal. The ratio may be scaled (108:36 and
     * 3:1 are equivalent); the returned component amounts are normalized to
     * the smallest integral quantum.
     */
    public static Optional<DrainPlan> planDrain(
            Map<String, Integer> contents,
            Map<String, Integer> ratio,
            int requested) {
        if (requested <= 0 || contents.isEmpty() || ratio.isEmpty()
                || !contents.keySet().equals(ratio.keySet())) {
            return Optional.empty();
        }

        int divisor = 0;
        for (int value : ratio.values()) {
            if (value <= 0) {
                return Optional.empty();
            }
            divisor = gcd(divisor, value);
        }

        LinkedHashMap<String, Integer> normalized = new LinkedHashMap<>();
        int quantum = 0;
        long availableBatches = Long.MAX_VALUE;
        for (var entry : ratio.entrySet()) {
            int part = entry.getValue() / divisor;
            int amount = contents.getOrDefault(entry.getKey(), 0);
            if (amount <= 0 || amount % part != 0) {
                return Optional.empty();
            }
            long batches = amount / part;
            if (availableBatches == Long.MAX_VALUE) {
                availableBatches = batches;
            } else if (availableBatches != batches) {
                return Optional.empty();
            }
            normalized.put(entry.getKey(), part);
            quantum += part;
        }

        long requestedBatches = requested / quantum;
        int drainedBatches = (int) Math.min(availableBatches, requestedBatches);
        if (drainedBatches <= 0) {
            return Optional.empty();
        }
        LinkedHashMap<String, Integer> removals = new LinkedHashMap<>();
        normalized.forEach((id, part) -> removals.put(id, part * drainedBatches));
        return Optional.of(new DrainPlan(quantum * drainedBatches, removals));
    }

    private static int gcd(int left, int right) {
        left = Math.abs(left);
        right = Math.abs(right);
        while (right != 0) {
            int next = left % right;
            left = right;
            right = next;
        }
        return left;
    }

    public record DrainPlan(int amount, Map<String, Integer> removals) {
        public DrainPlan {
            removals = Map.copyOf(removals);
        }
    }
}
