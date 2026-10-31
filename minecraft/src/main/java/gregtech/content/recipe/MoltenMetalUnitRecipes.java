package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialProperty;
import gregtech.api.addon.recipe.MaterialUnitIngredient.Rounding;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.recipe.MaterialUnitRecipe;
import gregtech.engine.recipe.Recipe;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Only proven, single-metal 144 mB/U conversions are folded; chemical and mixed recovery rows remain intact. */
final class MoltenMetalUnitRecipes {
    private record Key(GTMaterial material, Fluid fluid, long power) {}
    private record Input(ItemStack item, long units, long ticks, int fluidAmount, Predicate<ItemStack> guard) {}
    private static final Map<Key, LinkedHashMap<net.minecraft.world.item.Item, Input>> GROUPS = new LinkedHashMap<>();
    private static final List<MaterialUnitRecipe> OWNED = new ArrayList<>();
    private static boolean collecting;
    private MoltenMetalUnitRecipes() {}

    static void clear() {
        for (var rule : OWNED) rule.remove();
        OWNED.clear(); GROUPS.clear(); collecting = false;
    }

    static void begin() {
        clear(); collecting = true;
        var map = MachineRecipeMaps.Smelter;
        for (Recipe row : List.copyOf(map.mRecipeList)) {
            if (row.mFakeRecipe || !row.mEnabled || row.mHidden || row.mInputs.length != 1
                    || row.mOutputs.length != 0 || row.mFluidInputs.length != 0 || row.mFluidOutputs.length != 1
                    || row.hasTaggedInputs() || row.isCatalystInput(0) || row.mSpecialItems != null || row.mSpecialValue != 0) continue;
            if (collect(row.mInputs[0], row.mFluidOutputs[0], row.mDuration, row.mEUt,
                    stack -> row.mEnabled && row.matchesItemInput(0, stack))) map.unlink(row);
        }
    }

    static boolean collect(ItemStack input, FluidStack output, long ticks, long power, Predicate<ItemStack> proof) {
        if (!collecting) return false;
        var material = inputMaterial(input, output);
        if (material == null) return false;
        long units = ItemMaterialRegistry.get(input).orElseThrow().amount();
        var key = new Key(material, output.getFluid(), power);
        GROUPS.computeIfAbsent(key, ignored -> new LinkedHashMap<>()).putIfAbsent(input.getItem(),
                new Input(input.copy(), units, ticks, output.getAmount(), proof));
        return true;
    }

    static GTMaterial inputMaterial(ItemStack input, FluidStack output) {
        if (input.isEmpty() || output.isEmpty() || MaterialUnitInputTags.isBullet(input)) return null;
        var composition = ItemMaterialRegistry.get(input).orElse(null);
        if (composition == null || composition.components().size() != 1 || composition.amount() <= 0) return null;
        GTMaterial material = composition.material().resolve();
        var fluid = gregtech.content.registry.GTFluids.entryForFluid(output.getFluid());
        if (!material.has(MaterialProperty.METAL) || fluid == null || !fluid.registryName().startsWith("molten.")) return null;
        long units = composition.amount();
        if (java.math.BigInteger.valueOf(units).multiply(java.math.BigInteger.valueOf(input.getCount())).multiply(java.math.BigInteger.valueOf(144))
                .compareTo(java.math.BigInteger.valueOf(output.getAmount()).multiply(java.math.BigInteger.valueOf(GTValues.U))) != 0) return null;
        if (GTItems.getStack(MaterialPrefix.unit, material, 1).isEmpty()) return null;
        return material;
    }

    static void finish() {
        int physical = 0;
        for (var entry : GROUPS.entrySet()) {
            Key key = entry.getKey();
            var inputs = entry.getValue().values();
            Input reference = inputs.stream().filter(input -> input.units == GTValues.U && input.item.getCount() == 1)
                    .findFirst().orElse(inputs.iterator().next());
            long ticks = java.math.BigInteger.valueOf(reference.ticks).multiply(java.math.BigInteger.valueOf(GTValues.U))
                    .add(java.math.BigInteger.valueOf(reference.units).multiply(java.math.BigInteger.valueOf(reference.item.getCount())).subtract(java.math.BigInteger.ONE))
                    .divide(java.math.BigInteger.valueOf(reference.units).multiply(java.math.BigInteger.valueOf(reference.item.getCount()))).longValueExact();
            var template = new Recipe(new ItemStack[]{GTItems.getStack(MaterialPrefix.unit, key.material, 1)}, new ItemStack[0],
                    null, null, null, new FluidStack[]{new FluidStack(key.fluid, 144)}, Math.max(1, ticks), key.power, 0);
            var rule = new MaterialUnitRecipe(template, 0, key.material, GTValues.U, Rounding.EXACT_RATIO);
            for (Input input : inputs) rule.source(input.item, input.units, input.guard, input.ticks, input.fluidAmount);
            rule.publish(MachineRecipeMaps.Smelter).withMaterialInputTag(0, MaterialUnitInputTags.smeltable(key.material)); OWNED.add(rule); physical += inputs.size();
        }
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Smelter molten-metal unit rules: {} physical inputs -> {} rules", physical, OWNED.size());
        GROUPS.clear(); collecting = false;
    }
}
