package com.gregtech.gregtech.api.addon;

import java.util.HashSet;
import java.util.List;

/**
 * SPDX-License-Identifier: LGPL-3.0-or-later.
 * Optional energy role on a structure part. EU and RU use the same record; only {@code type} changes.
 */
public record StructurePartEnergy(String type, long capacity, List<BehaviorMachineDeclaration.Face> outputFaces,
                                  long maxSize, long maxAmount) {
    public StructurePartEnergy {
        if (type == null || !type.matches("[A-Za-z][A-Za-z0-9_]{0,15}"))
            throw new IllegalArgumentException("Energy part type is invalid: " + type);
        if (capacity < 1)
            throw new IllegalArgumentException("Energy part capacity must be positive: " + capacity);
        if (outputFaces == null || outputFaces.isEmpty())
            throw new IllegalArgumentException("Energy part needs an output face");
        for (BehaviorMachineDeclaration.Face face : outputFaces)
            if (face == null) throw new IllegalArgumentException("Energy part output face is missing");
        outputFaces = List.copyOf(outputFaces);
        if (new HashSet<>(outputFaces).size() != outputFaces.size())
            throw new IllegalArgumentException("Energy part repeats an output face");
        if (maxSize < 1 || maxAmount < 1)
            throw new IllegalArgumentException("Energy part output maximum must be positive");
    }

    /** How much of {@code amount} fits in the remaining capacity. Does not change {@code stored}. */
    public long accept(long stored, long amount) {
        if (amount < 0) throw new IllegalArgumentException("negative charge");
        long held = Math.max(0, Math.min(stored, capacity));
        return Math.min(amount, capacity - held);
    }

    /** Rejects an output above {@link #maxSize()} and {@link #maxAmount()}. Does not clamp. */
    public void requireOutput(long size, long amount) {
        BehaviorEnergyRules.requireEmit(size, amount, maxSize, maxAmount);
    }
}
