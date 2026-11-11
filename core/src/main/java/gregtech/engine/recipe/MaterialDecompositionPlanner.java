package gregtech.engine.recipe;

import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.addon.material.MaterialDecompositionRule;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import gregtech.api.material.MaterialProperty;
import gregtech.content.recipe.SourceDecompositionRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static gregtech.api.material.GTValues.U;

/** Pure decomposition planning shared by recipe publication and content-source projections. */
public final class MaterialDecompositionPlanner {
    public static final long ENVIRONMENT_TEMPERATURE = 293;

    private MaterialDecompositionPlanner() {}

    public enum Status {
        PLANNED,
        NO_PERMISSION,
        NO_COMPOSITION,
        INVALID_DIVIDER,
        INVALID_QUANTITY,
        ANTIMATTER,
        MISSING_COMPONENT,
        MISSING_OUTPUT
    }

    public record FluidInput(MaterialDecompositionRule.Phase phase, String fluid, int millibuckets, boolean declared) {}

    public record DustOutput(MaterialKey material, long units) {}

    public record FluidOutput(
            MaterialKey component,
            MaterialDecompositionRule.Phase phase,
            String fluid,
            int millibuckets,
            boolean declared) {}

    /**
     * A platform-neutral executable plan. Fluid inputs remain separate alternatives; the adapter
     * resolves their registered identities and applies the solid fallback rule.
     */
    public record Plan(
            MaterialKey material,
            MaterialDecompositionRule.Route route,
            long divider,
            long inputUnits,
            long eut,
            long duration,
            List<FluidInput> fluidInputs,
            int blockDustCount,
            List<DustOutput> itemOutputs,
            List<FluidOutput> fluidOutputs,
            int droppedItemOutputs,
            int droppedFluidOutputs,
            boolean electrolyserCatalyst) {}

    public record Result(Status status, String reason, Plan plan) {
        public boolean planned() {
            return status == Status.PLANNED;
        }

        private static Result rejected(Status status, String reason) {
            return new Result(status, reason, null);
        }

        private static Result planned(Plan plan) {
            return new Result(Status.PLANNED, "", plan);
        }
    }

    public static Result plan(
            GTMaterial input,
            MaterialDecompositionRule rule,
            MaterialDecompositionRule.Route route,
            int itemOutputSlots,
            int fluidOutputSlots) {
        if (input == null || !input.isValid()) return Result.rejected(Status.MISSING_COMPONENT, "unknown input");
        GTMaterial material = input.resolve();
        if (rule == null || !rule.routes().contains(route))
            return Result.rejected(Status.NO_PERMISSION, "route is not declared");
        if (!material.hasComposition()) return Result.rejected(Status.NO_COMPOSITION, "material has no composition");
        long divider = material.getCompositionDivider();
        if (divider <= 0 || divider > 64)
            return Result.rejected(Status.INVALID_DIVIDER, "divider must be 1..64, found " + divider);
        if (material.has(MaterialProperty.ANTIMATTER))
            return Result.rejected(Status.ANTIMATTER, "antimatter decomposition is not a recipe source");

        long inputUnits;
        long totalUnits = 0;
        try {
            inputUnits = Math.multiplyExact(divider, U);
            for (MaterialComponent component : material.getCompositionComponents())
                totalUnits = Math.addExact(totalUnits, component.amount());
        } catch (ArithmeticException overflow) {
            return Result.rejected(Status.INVALID_QUANTITY, "composition quantity overflow");
        }

        List<FluidInput> fluidInputs = new ArrayList<>();
        for (MaterialDecompositionRule.FluidBinding binding : rule.fluids().stream()
                .filter(value -> value.phase() == MaterialDecompositionRule.Phase.LIQUID
                        || value.phase() == MaterialDecompositionRule.Phase.GAS)
                .sorted(Comparator.comparingInt(value -> value.phase().ordinal()))
                .toList()) {
            int amount = fluidAmount(binding.sourceUnits(), binding.millibuckets(), inputUnits, true);
            if (amount <= 0)
                return Result.rejected(
                        Status.INVALID_QUANTITY,
                        "fluid input " + binding.fluid() + " cannot represent " + inputUnits + " source units");
            fluidInputs.add(new FluidInput(binding.phase(), binding.fluid(), amount, true));
        }
        if (fluidInputs.isEmpty()) {
            long amount = SourceDecompositionRules.units(inputUnits, U, 1000, true);
            if (amount <= 0 || amount > Integer.MAX_VALUE)
                return Result.rejected(Status.INVALID_QUANTITY, "generic fluid input cannot represent " + inputUnits);
            fluidInputs.add(new FluidInput(null, material.getName(), (int) amount, false));
        }

        List<DustOutput> itemOutputs = new ArrayList<>();
        List<FluidOutput> fluidOutputs = new ArrayList<>();
        for (MaterialComponent component : material.getCompositionComponents()) {
            GTMaterial part = component.material().resolve();
            if (!part.isValid())
                return Result.rejected(
                        Status.MISSING_COMPONENT,
                        "unknown component " + component.material().getName());
            FluidOutput fluid = ambientOutput(part, component.amount());
            if (fluid != null) {
                fluidOutputs.add(fluid);
                continue;
            }
            GTMaterial pulver = part.getTargetPulverMaterial().resolve();
            long pulverAmount = part.getTargetPulverAmount();
            if (!pulver.isValid() || pulverAmount <= 0)
                return Result.rejected(Status.MISSING_OUTPUT, "component " + part.getName() + " has no powder target");
            long units = SourceDecompositionRules.units(component.amount(), U, pulverAmount, false);
            if (units <= 0)
                return Result.rejected(
                        Status.INVALID_QUANTITY, "component " + part.getName() + " cannot represent powder output");
            itemOutputs.add(new DustOutput(new MaterialKey(pulver.getName()), units));
        }
        if (itemOutputs.isEmpty() && fluidOutputs.isEmpty())
            return Result.rejected(Status.MISSING_OUTPUT, "composition produced no resolvable output");

        int keptItems = Math.min(itemOutputs.size(), Math.max(0, itemOutputSlots));
        int keptFluids = Math.min(fluidOutputs.size(), Math.max(0, fluidOutputSlots));
        int droppedItems = itemOutputs.size() - keptItems;
        int droppedFluids = fluidOutputs.size() - keptFluids;
        itemOutputs = List.copyOf(itemOutputs.subList(0, keptItems));
        fluidOutputs = List.copyOf(fluidOutputs.subList(0, keptFluids));

        long eut = Math.max(16L, units(totalUnits, U, 14, false));
        long duration = Math.max(1L, units(totalUnits, U, 292, true));
        int blockDustCount = divider % 9 == 0 ? (int) (divider / 9) : 0;
        return Result.planned(new Plan(
                new MaterialKey(material.getName()),
                route,
                divider,
                inputUnits,
                eut,
                duration,
                fluidInputs,
                blockDustCount,
                itemOutputs,
                fluidOutputs,
                droppedItems,
                droppedFluids,
                route == MaterialDecompositionRule.Route.ELECTROLYSER));
    }

    private static FluidOutput ambientOutput(GTMaterial material, long units) {
        if (material.getMeltingPoint() > ENVIRONMENT_TEMPERATURE) return null;
        MaterialDecompositionRule.Phase phase = material.getBoilingPoint() <= ENVIRONMENT_TEMPERATURE
                ? MaterialDecompositionRule.Phase.GAS
                : MaterialDecompositionRule.Phase.LIQUID;
        var process = GregTechMaterials.processes()
                .findSingle(new MaterialKey(material.getName()), MaterialProcessKeys.DECOMPOSITION);
        if (process.isEmpty() || process.get().value().fluids().isEmpty())
            return genericAmbientOutput(material, phase, units);
        for (MaterialDecompositionRule.FluidBinding binding :
                process.get().value().fluids()) {
            if (binding.phase() != phase) continue;
            int amount = fluidAmount(binding.sourceUnits(), binding.millibuckets(), units, false);
            if (amount <= 0) return null;
            return new FluidOutput(new MaterialKey(material.getName()), phase, binding.fluid(), amount, true);
        }
        return null;
    }

    private static FluidOutput genericAmbientOutput(
            GTMaterial material, MaterialDecompositionRule.Phase phase, long units) {
        long amount = units(units, U, 1000, false);
        if (amount <= 0 || amount > Integer.MAX_VALUE) return null;
        return new FluidOutput(new MaterialKey(material.getName()), phase, material.getName(), (int) amount, false);
    }

    private static int fluidAmount(long sourceUnits, long millibuckets, long units, boolean ceil) {
        long value = SourceDecompositionRules.units(units, sourceUnits, millibuckets, ceil);
        return value > Integer.MAX_VALUE ? -1 : (int) value;
    }

    private static long units(long amount, long from, long to, boolean ceil) {
        return SourceDecompositionRules.units(amount, from, to, ceil);
    }
}
