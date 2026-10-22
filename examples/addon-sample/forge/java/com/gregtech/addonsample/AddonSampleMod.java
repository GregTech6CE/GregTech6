package com.gregtech.addonsample;

import com.gregtech.gregtech.api.addon.GregTechAddon;
import com.gregtech.gregtech.api.addon.GregTechAddons;
import com.gregtech.gregtech.api.addon.GregTechMaterials;
import com.gregtech.gregtech.api.addon.platform.AddonFluids;
import com.gregtech.gregtech.api.addon.platform.AddonItems;
import com.gregtech.gregtech.api.addon.platform.AddonRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;

@Mod("addon_sample")
public final class AddonSampleMod {
    public AddonSampleMod() {
        GregTechAddons.register(new GregTechAddon() {
            @Override public String id() { return "addon_sample"; }

            @Override public void onRecipesReady(Context context) {
                if (AddonItems.stack("ingot", "Iron", 1).isEmpty())
                    throw new IllegalStateException("sample iron ingot missing");
                if (!GregTechMaterials.find("Steel").valid())
                    throw new IllegalStateException("sample steel missing");
                if (AddonItems.stack("plate", "SampleRubber").isEmpty() || AddonItems.stack("foil", "SampleRubber").isEmpty())
                    throw new IllegalStateException("sample rubber plate or foil missing");
                if (AddonItems.stack("sampleBlank", "Iron").isEmpty() || AddonItems.stack("sampleBlank", "SampleAlloy").isEmpty())
                    throw new IllegalStateException("sample blank missing");
                var added = AddonRecipes.map("addon_sample.recipe.press").add(new AddonRecipes.Line(
                        new ItemStack[]{new ItemStack(Items.IRON_INGOT)},
                        new ItemStack[]{new ItemStack(Items.IRON_NUGGET)},
                        null, null, 20, 16, 0, new int[]{5_000}));
                if (added == null) throw new IllegalStateException("sample press chance row rejected");
                var circuit = AddonRecipes.map("addon_sample.recipe.press").add(new AddonRecipes.Line(
                        new ItemStack[]{new ItemStack(Items.GOLD_INGOT), AddonItems.selectorTag(3)},
                        new ItemStack[]{new ItemStack(Items.CLOCK)},
                        null, null, 20, 16, 0));
                if (circuit == null) throw new IllegalStateException("sample circuit row rejected");
                var kept = AddonRecipes.map("addon_sample.recipe.press").add(new AddonRecipes.Line(
                        new ItemStack[]{new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.EMERALD)},
                        new ItemStack[]{new ItemStack(Items.BRICK)},
                        null, null, 20, 16, 0, null, new int[]{1}));
                if (kept == null) throw new IllegalStateException("sample kept input row rejected");
                var filled = AddonRecipes.map("addon_sample.recipe.detector").add(new AddonRecipes.Line(
                        new ItemStack[]{new ItemStack(Items.COBBLESTONE)},
                        new ItemStack[]{new ItemStack(Items.GOLD_NUGGET), new ItemStack(Items.IRON_NUGGET)},
                        null, new FluidStack[]{new FluidStack(Fluids.WATER, 250)},
                        20, 16, 0, new int[]{6_000, 4_000}));
                if (filled == null) throw new IllegalStateException("sample detector fill row rejected");
                var read = AddonRecipes.map("addon_sample.recipe.detector").add(new AddonRecipes.Line(
                        new ItemStack[]{new ItemStack(Items.IRON_INGOT)},
                        new ItemStack[]{new ItemStack(Items.EMERALD), new ItemStack(Items.DIAMOND)},
                        new FluidStack[]{new FluidStack(Fluids.WATER, 250)}, null,
                        20, 16, 0, new int[]{6_000, 4_000}));
                if (read == null) throw new IllegalStateException("sample detector read row rejected");
                var keptFluid = AddonRecipes.map("addon_sample.recipe.detector").add(new AddonRecipes.Line(
                        new ItemStack[]{new ItemStack(Items.CLAY_BALL)},
                        new ItemStack[]{new ItemStack(Items.BRICK), new ItemStack(Items.CLAY)},
                        new FluidStack[]{new FluidStack(Fluids.LAVA, 100)}, null,
                        20, 16, 0, new int[]{10_000, 10_000}, null, new int[]{0}));
                if (keptFluid == null) throw new IllegalStateException("sample kept fluid row rejected");
                var hydrogen = AddonFluids.still("Hydrogen", 2);
                if (hydrogen == null || hydrogen.isEmpty())
                    throw new IllegalStateException("sample hydrogen missing");
                var fuel = AddonRecipes.map("addon_sample.fuel.gas").add(new AddonRecipes.Line(
                        null, null,
                        new FluidStack[]{hydrogen},
                        new FluidStack[]{new FluidStack(Fluids.WATER, 3)},
                        2, -16, 0));
                if (fuel == null) throw new IllegalStateException("sample hydrogen fuel rejected");
                addByproductRow("Copper");
                addByproductRow("SampleAlloy");
                var moved = AddonRecipes.map("addon_sample.recipe.large_press").add(new AddonRecipes.Line(
                        new ItemStack[]{new ItemStack(Items.IRON_INGOT)},
                        new ItemStack[]{new ItemStack(Items.IRON_NUGGET)},
                        null, new FluidStack[]{new FluidStack(Fluids.WATER, 250)},
                        100, 128, 0));
                if (moved == null) throw new IllegalStateException("sample large press output row rejected");
            }
        });
    }

    /** Crushed ore to one dust of each byproduct. The large press map holds six item outputs. */
    private static void addByproductRow(String material) {
        var names = GregTechMaterials.find(material).byproducts();
        if (names.isEmpty() || names.size() > 6)
            throw new IllegalStateException(material + " byproducts do not fit the large press: " + names);
        var outputs = new ItemStack[names.size()];
        for (int i = 0; i < names.size(); i++) {
            ItemStack dust = AddonItems.stack("dust", names.get(i));
            if (dust.isEmpty()) throw new IllegalStateException(material + " byproduct dust missing: " + names.get(i));
            outputs[i] = dust;
        }
        ItemStack crushed = AddonItems.stack("crushed", material);
        if (crushed.isEmpty()) throw new IllegalStateException(material + " crushed missing");
        var added = AddonRecipes.map("addon_sample.recipe.large_press").add(new AddonRecipes.Line(
                new ItemStack[]{crushed}, outputs, null, null, 100, 128, 0));
        if (added == null) throw new IllegalStateException(material + " byproduct row rejected");
    }
}
