package gregtech.engine.fluid;

import gregtech.api.material.GTMaterial;
import gregtech.engine.machine.crucible.CrucibleMaterialStack;
import java.util.List;
import java.util.Map;

/** Exact material-unit plans for crucible fluid capabilities; legacy no-material calls stay 144mB/U. */
public final class MoltenFluidPlans {
    public static final long UNITS_PER_MB = MoltenMaterialUnits.STANDARD.unitsPerMillibucket();

    private MoltenFluidPlans() {}

    public static long toUnits(int mb) {
        return toUnits(mb, MoltenMaterialUnits.STANDARD);
    }

    public static long toUnits(int mb, MoltenMaterialUnits.Ratio ratio) {
        return ratio.toUnits(mb);
    }

    public static int millibuckets(long units) {
        return millibuckets(units, MoltenMaterialUnits.STANDARD);
    }

    public static int millibuckets(long units, MoltenMaterialUnits.Ratio ratio) {
        return ratio.millibuckets(units);
    }

    public static int fillAmount(
            List<CrucibleMaterialStack> contents, long capacity, GTMaterial material, int requested, long temperature) {
        return fillAmount(contents, capacity, material, requested, temperature, MoltenMaterialUnits.ratio(material));
    }

    public static int fillAmount(
            List<CrucibleMaterialStack> contents,
            long capacity,
            GTMaterial material,
            int requested,
            long temperature,
            MoltenMaterialUnits.Ratio ratio) {
        if (material == null
                || !material.resolve().isValid()
                || temperature < material.resolve().getMeltingPoint()) return 0;
        long total = checkedTotal(contents, null);
        if (total < 0) return 0;
        long remaining = capacity <= 0 || total >= capacity ? 0 : capacity - total;
        return MoltenTransferMath.planFill(requested, millibuckets(remaining, ratio), 1);
    }

    public static int drainAmount(
            List<CrucibleMaterialStack> contents, GTMaterial material, int requested, long temperature) {
        return drainAmount(contents, material, requested, temperature, MoltenMaterialUnits.ratio(material));
    }

    public static int drainAmount(
            List<CrucibleMaterialStack> contents,
            GTMaterial material,
            int requested,
            long temperature,
            MoltenMaterialUnits.Ratio ratio) {
        if (requested <= 0
                || material == null
                || !material.resolve().isValid()
                || temperature < material.resolve().getMeltingPoint()) return 0;
        GTMaterial canonical = material.resolve();
        long amount = checkedTotal(contents, canonical);
        if (amount < 0) return 0;
        int available = millibuckets(amount, ratio);
        if (available <= 0) return 0;
        return MoltenTransferMath.planDrain(
                        Map.of(canonical.getName(), available), Map.of(canonical.getName(), 1), requested)
                .map(MoltenTransferMath.DrainPlan::amount)
                .orElse(0);
    }

    public static void remove(List<CrucibleMaterialStack> contents, GTMaterial material, int mb) {
        remove(contents, material, mb, MoltenMaterialUnits.ratio(material));
    }

    public static void remove(
            List<CrucibleMaterialStack> contents, GTMaterial material, int mb, MoltenMaterialUnits.Ratio ratio) {
        long remaining = toUnits(mb, ratio);
        if (remaining <= 0) return;
        for (var stack : contents)
            if (stack.material.resolve() == material.resolve()) {
                long removed = Math.min(remaining, stack.amount);
                stack.amount -= removed;
                remaining -= removed;
                if (remaining == 0) break;
            }
        contents.removeIf(stack -> stack.amount <= 0);
    }
    /** Corrupt/overflowing saved content cannot turn into apparent free capacity. */
    private static long checkedTotal(List<CrucibleMaterialStack> contents, GTMaterial material) {
        long total = 0;
        try {
            for (var stack : contents) {
                if (stack == null) continue;
                if (stack.amount < 0) return -1;
                if (material == null || stack.material.resolve() == material)
                    total = Math.addExact(total, stack.amount);
            }
            return total;
        } catch (ArithmeticException invalidAmount) {
            return -1;
        }
    }
}
