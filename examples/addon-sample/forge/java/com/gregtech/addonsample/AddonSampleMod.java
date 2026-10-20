package com.gregtech.addonsample;

import com.gregtech.gregtech.api.addon.GregTechAddon;
import com.gregtech.gregtech.api.addon.GregTechAddons;
import com.gregtech.gregtech.api.addon.GregTechMaterials;
import com.gregtech.gregtech.api.addon.platform.AddonItems;
import com.gregtech.gregtech.api.addon.platform.AddonRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
                var added = AddonRecipes.map("addon_sample.recipe.press").add(new AddonRecipes.Line(
                        new ItemStack[]{new ItemStack(Items.IRON_INGOT)},
                        new ItemStack[]{new ItemStack(Items.IRON_NUGGET)},
                        null, null, 20, 16, 0, new int[]{5_000}));
                if (added == null) throw new IllegalStateException("sample press chance row rejected");
            }
        });
    }
}
