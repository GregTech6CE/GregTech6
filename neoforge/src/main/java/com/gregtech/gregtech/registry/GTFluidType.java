package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.fluid.FluidHazards;
import com.gregtech.gregtech.api.fluid.FluidVisualPolicy;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialStackItemHelper;
import com.gregtech.gregtech.content.fluid.OriginalFluidDisplayRules;
import com.gregtech.gregtech.content.fluid.OriginalFluidDisplayRules.*;
import com.gregtech.gregtech.data.FuelRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.RegisteredFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/** Native language/stack adapter for the shared original fluid display rules. */
public final class GTFluidType {
    private GTFluidType() {}

    public static Component describe(RegisteredFluids.FluidEntry entry, String langPath) {
        String key = "fluid_type.gregtech." + langPath;
        if (Language.getInstance().has(key)) return Component.translatable(key);
        String originalKey = "fluid." + entry.registryName();
        if (Language.getInstance().has(originalKey)) return Component.translatable(originalKey);
        GTMaterial material = FluidVisualPolicy.material(entry);
        if (!material.isValid()) return Component.translatableWithFallback(key, entry.registryName());
        Component name = Component.translatableWithFallback(material.getTranslationKey(), material.getDisplayNameFallback());
        return entry.registryName().startsWith("molten.")
                ? Component.translatable("fluid_type.gregtech.molten_material", name) : name;
    }

    /** Amount is zero for an unbound creative icon; bound recipe/tank stacks retain their amount. */
    public static List<Component> describeTooltip(FluidStack stack, long amount, boolean advanced) {
        if (stack.isEmpty()) return List.of(Component.translatable(OriginalFluidDisplayRules.PREFIX + "missing").withStyle(ChatFormatting.RED));
        var fluid = stack.getFluid();
        var id = BuiltInRegistries.FLUID.getKey(fluid);
        var entry = GTFluids.entryForFluid(fluid);
        var type = fluid.getFluidType();
        var material = entry == null ? GTMaterialRegistry.get("NULL") : FluidVisualPolicy.material(entry);
        long flags = entry == null ? 0 : entry.flags();
        String registry = entry == null ? id.toString() : entry.registryName();
        boolean industrial = "lubricant".equals(registry) || "rc lubricant".equals(registry);
        Owner owner = id.getNamespace().equals("gregtech") ? Owner.GT6
                : fluid == net.minecraft.world.level.material.Fluids.WATER
                || fluid == net.minecraft.world.level.material.Fluids.FLOWING_WATER
                || fluid == net.minecraft.world.level.material.Fluids.LAVA
                || fluid == net.minecraft.world.level.material.Fluids.FLOWING_LAVA ? Owner.VANILLA : Owner.OTHER;
        var facts = new Facts(registry, advanced, (flags & RegisteredFluids.FluidFlags.NONSTANDARD) != 0,
                amount, material.isValid() ? material.getResolvedTooltipChemical() : "",
                type.getTemperature(stack), GTFluids.isGas(stack), FluidHazards.isPlasma(fluid), type.isLighterThanAir(),
                material.isValid() && !MaterialStackItemHelper.mat(MaterialPrefix.ingot, material, 1).isEmpty(),
                type.getDensity(stack), type.getLightLevel(stack), type.getViscosity(stack), flags,
                FluidHazards.isAcid(fluid), FluidHazards.isMagic(fluid), industrial, owner);
        var fuels = new ArrayList<Fuel>();
        if (!industrial) for (var map : FuelRecipeMaps.FUEL_MAP_LIST) {
            var recipes = map.mRecipeFluidMap.get(id.toString());
            if (recipes == null) continue;
            BigInteger maximum = BigInteger.ZERO;
            for (var recipe : recipes) {
                if (!recipe.mEnabled || recipe.mFluidInputs.length == 0 || recipe.mFluidInputs[0] == null) continue;
                maximum = maximum.max(OriginalFluidDisplayRules.fuelPower(recipe.mEUt, recipe.mDuration,
                        recipe.mFluidInputs[0].getAmount()));
            }
            if (maximum.signum() > 0) fuels.add(new Fuel(map.mNameInternal, maximum));
        }
        var result = new ArrayList<Component>();
        for (var line : OriginalFluidDisplayRules.describe(facts, fuels)) {
            var rendered = Component.empty();
            for (var part : line.parts()) {
                var text = part.key() == null ? Component.literal((String) part.arguments().get(0))
                        : Component.translatable(part.key(), part.arguments().toArray());
                if (part.color() != Color.DEFAULT) text.withStyle(ChatFormatting.valueOf(part.color().name()));
                rendered.append(text);
            }
            result.add(rendered);
        }
        return result;
    }
}
