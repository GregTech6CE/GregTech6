/* Copyright (c) 2021 GregTech-6 Team; Gregorius Techneticies.
 * LGPL-3.0-or-later. Adapted from Loader_MultiTileEntities, the four
 * Heater/Cooler Electric/Flux classes, TileEntityBase11Twotypes and LH. */
package com.gregtech.gregtech.content.energy;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.GregTechTags;
import java.util.*;

/** Original twenty thermal devices. Native storage, collision, faces and both emissions stay native. */
public final class OriginalThermalConverter {
    private OriginalThermalConverter() {}
    public record Variant(String id, int sourceId, GTMaterial material, long input, long output,
                          String texture, String englishName) {}
    public static final List<Variant> VARIANTS;
    static {
        String[] tiers = {"lv", "mv", "hv", "ev", "iv"};
        GTMaterial[] electric = {Materials.SteelGalvanized, Materials.Aluminium, Materials.StainlessSteel, Materials.Chromium, Materials.Titanium};
        GTMaterial[] flux = {Materials.Lead, Materials.Invar, Materials.Electrum, Materials.EnderiumBase, Materials.Enderium};
        var variants = new ArrayList<Variant>();
        for (boolean cooler : new boolean[]{false, true}) for (boolean rf : new boolean[]{false, true}) for (int i = 0; i < 5; i++) {
            String id = (rf ? "flux_" : "electric_") + (cooler ? "cooler_" : "heater_") + tiers[i];
            GTMaterial material = (rf ? flux : electric)[i];
            long input = (32L << (2 * i)) * (rf ? 4 : 1), output = (cooler ? 8L : 16L) << (2 * i);
            int sourceId = (cooler ? 10160 : 10000) + (rf ? 1000 : 0) + i + 1;
            String name = (cooler ? (rf ? "Thermofluxic Cooler" : "Thermoelectric Cooler") : (rf ? "Flux Heater" : "Electric Heater"))
                    + " (" + (rf ? material.getLocalName() : tiers[i].toUpperCase(Locale.ROOT)) + ")";
            variants.add(new Variant(id, sourceId, material, input, output,
                    (cooler ? "cooler/cryo_" : "heaters/heat_") + (rf ? "flux" : "electric"), name));
        }
        VARIANTS = List.copyOf(variants);
    }
    public static boolean handles(EnergyNodeSpec spec) {
        return spec.id().startsWith("electric_heater_") || spec.id().startsWith("electric_cooler_")
                || spec.id().startsWith("flux_heater_") || spec.id().startsWith("flux_cooler_");
    }
    public static boolean cooler(EnergyNodeSpec spec) { return spec.id().startsWith("electric_cooler_") || spec.id().startsWith("flux_cooler_"); }
    /** Source Flux Cooler inherits saved mode arithmetic but does not expose ITileEntitySwitchableMode. */
    public static boolean modeSelectable(EnergyNodeSpec spec) { return handles(spec) && !spec.id().startsWith("flux_cooler_"); }
    public static int efficiency(EnergyNodeSpec spec) {
        // Source LH's twin-type RF branch deliberately uses 8; single-output RF uses RF_PER_EU=4.
        long factor = spec.inType() == GregTechTags.Energy.RF ? cooler(spec) ? 8 : 4 : 1;
        return (int) (10000 * spec.outputRate() * factor / spec.inputRate());
    }
    public static float contactDamage(EnergyNodeSpec spec) { return cooler(spec) ? 0 : Math.min(10.0F, spec.outputRate() / 10.0F); }
    public static List<EnergyNodeSpec> specifications() {
        return VARIANTS.stream().map(v -> EnergyNodeSpec.builder(v.id(), v.material()).kind(EnergyNodeSpec.Kind.CONVERTER)
                .texture(v.texture()).input(v.id().startsWith("flux_") ? GregTechTags.Energy.RF : GregTechTags.Energy.EU, v.input())
                .output(v.id().contains("cooler_") ? GregTechTags.Energy.CU : GregTechTags.Energy.HU, v.output())
                .capacity(v.input() * 2).names(v.englishName(), v.englishName()).build()).toList();
    }
}
