/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Loader_MultiTileEntities 10031..10035 / 11031..11035 and TileEntityBase11Bipolar. */
package com.gregtech.gregtech.content.energy;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.GregTechTags;

import java.util.ArrayList;
import java.util.List;

/** The two GT6 bipolar magnet families, distinct from passive material magnet blocks. */
public final class MagnetMachineDefinitions {
    private static final String[] TIERS = {"lv", "mv", "hv", "ev", "iv"};
    private static final GTMaterial[] ELECTRIC_CASINGS = {
            Materials.SteelGalvanized, Materials.Aluminium, Materials.StainlessSteel,
            Materials.Chromium, Materials.Titanium};
    private static final GTMaterial[] FLUX_CASINGS = {
            Materials.Lead, Materials.Invar, Materials.Electrum,
            Materials.EnderiumBase, Materials.Enderium};
    private static final long[] ELECTRIC_INPUT = {32, 128, 512, 2048, 8192};
    private static final long[] FLUX_INPUT = {128, 512, 2048, 8192, 32768};
    private static final long[] MAGNETIC_OUTPUT = {16, 64, 256, 1024, 4096};
    private static final String[] FLUX_NAMES_EN = {"Lead", "Invar", "Electrum", "Enderium Base", "Enderium"};
    private static final String[] FLUX_NAMES_ZH = {"铅", "殷钢", "琥珀金", "末影粗胚", "末影"};

    private MagnetMachineDefinitions() {}

    public static boolean handles(EnergyNodeSpec spec) {
        return spec.kind() == EnergyNodeSpec.Kind.MAGNET
                && (spec.id().startsWith("electromagnet_") || spec.id().startsWith("flux_magnet_"));
    }

    /** Bipolar efficiency includes both equally rated poles; RF has four units per EU. */
    public static int efficiency(EnergyNodeSpec spec) {
        return (int) (10000L * spec.outputRate() * (spec.inType() == GregTechTags.Energy.RF ? 8 : 2) / spec.inputRate());
    }

    public static List<EnergyNodeSpec> specifications() {
        List<EnergyNodeSpec> result = new ArrayList<>(10);
        for (int i = 0; i < TIERS.length; i++) {
            String tier = TIERS[i];
            result.add(EnergyNodeSpec.builder("electromagnet_" + tier, ELECTRIC_CASINGS[i])
                    .kind(EnergyNodeSpec.Kind.MAGNET).texture("magnets/magnet_electric")
                    .input(GregTechTags.Energy.EU, ELECTRIC_INPUT[i])
                    .output(GregTechTags.Energy.MU, MAGNETIC_OUTPUT[i])
                    .capacity(ELECTRIC_INPUT[i] * 2)
                    .names("Electromagnet (" + tier.toUpperCase() + ")", "电磁铁 (" + tier.toUpperCase() + ")")
                    .build());
            result.add(EnergyNodeSpec.builder("flux_magnet_" + tier, FLUX_CASINGS[i])
                    .kind(EnergyNodeSpec.Kind.MAGNET).texture("magnets/magnet_flux")
                    .input(GregTechTags.Energy.RF, FLUX_INPUT[i])
                    .output(GregTechTags.Energy.MU, MAGNETIC_OUTPUT[i])
                    .capacity(FLUX_INPUT[i] * 2)
                    .names("Flux Magnet (" + FLUX_NAMES_EN[i] + ")",
                            "红石通量磁场产生器 (" + FLUX_NAMES_ZH[i] + ")")
                    .build());
        }
        return List.copyOf(result);
    }
}
