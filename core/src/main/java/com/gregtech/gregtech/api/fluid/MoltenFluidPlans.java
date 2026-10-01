package com.gregtech.gregtech.api.fluid;

import com.masson.cruciblecraft.fluid.MoltenTransferMath;
import com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import java.util.List;
import java.util.Map;

/** Masson 144mB plans adapted to the existing GT6 U precision and integer Kelvin. */
public final class MoltenFluidPlans {
    public static final long UNITS_PER_MB = GTValues.U / MoltenTransferMath.MILLIBUCKETS_PER_INGOT;
    private MoltenFluidPlans() {}
    public static long toUnits(int mb) { return Math.multiplyExact((long) mb, UNITS_PER_MB); }
    public static int millibuckets(long units) { return units <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, units / UNITS_PER_MB); }
    public static int fillAmount(List<CrucibleMaterialStack> contents, long capacity, GTMaterial material, int requested, long temperature) {
        if (material == null || !material.resolve().isValid() || temperature < material.resolve().getMeltingPoint()) return 0;
        long remaining = Math.max(0, capacity - CrucibleMaterialStack.total(contents));
        return MoltenTransferMath.planFill(requested, millibuckets(remaining), 1);
    }
    public static int drainAmount(List<CrucibleMaterialStack> contents, GTMaterial material, int requested, long temperature) {
        if (requested <= 0 || material == null || !material.resolve().isValid() || temperature < material.resolve().getMeltingPoint()) return 0;
        GTMaterial canonical = material.resolve(); long amount = 0;
        for (var stack : contents) if (stack.material.resolve() == canonical) amount = Math.addExact(amount, stack.amount);
        int available = millibuckets(amount);
        if (available <= 0) return 0;
        return MoltenTransferMath.planDrain(Map.of(canonical.getName(), available), Map.of(canonical.getName(), 1), requested)
                .map(MoltenTransferMath.DrainPlan::amount).orElse(0);
    }
    public static void remove(List<CrucibleMaterialStack> contents, GTMaterial material, int mb) {
        long remaining = toUnits(mb);
        for (var stack : contents) if (stack.material.resolve() == material.resolve()) {
            long removed = Math.min(remaining, stack.amount); stack.amount -= removed; remaining -= removed;
            if (remaining == 0) break;
        }
        contents.removeIf(stack -> stack.amount <= 0);
    }
}
