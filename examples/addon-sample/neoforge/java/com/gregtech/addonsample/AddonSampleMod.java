package com.gregtech.addonsample;

import com.gregtech.gregtech.api.addon.GregTechAddon;
import com.gregtech.gregtech.api.addon.GregTechAddons;
import com.gregtech.gregtech.api.addon.GregTechMaterials;
import com.gregtech.gregtech.api.addon.platform.AddonItems;
import net.neoforged.fml.common.Mod;

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
            }
        });
    }
}
