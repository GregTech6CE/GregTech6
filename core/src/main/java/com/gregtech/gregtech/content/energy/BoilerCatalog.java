package com.gregtech.gregtech.content.energy;
import com.gregtech.gregtech.content.material.Materials;import com.gregtech.gregtech.api.material.GTMaterial;import com.gregtech.gregtech.api.machine.BoilerSpec;
public final class BoilerCatalog {private BoilerCatalog(){}
    /** material id suffix, material, hardness, normal table value, strong table value, English fallback. */
    private record Mat(String id, GTMaterial mat, float hardness, int normal, int strong, String en) {}

    private static final Mat[] MATS = {
            new Mat("lead",           Materials.Lead,            4.0F,  16,  64, "Lead"),
            new Mat("bismuth",        Materials.Bismuth,            4.0F,  20,  80, "Bismuth"),
            new Mat("bronze",         Materials.Bronze,        7.0F,  24,  96, "Bronze"),
            new Mat("arsenic_copper", Materials.ArsenicCopper, 7.0F,  24,  96, "Arsenic Copper"),
            new Mat("arsenic_bronze", Materials.ArsenicBronze, 7.0F,  28, 112, "Arsenic Bronze"),
            new Mat("invar",          Materials.Invar,         4.0F,  16,  64, "Invar"),
            new Mat("steel",          Materials.Steel,         6.0F,  32, 128, "Steel"),
            new Mat("chromium",       Materials.Chromium,            4.0F,  96, 384, "Chromium"),
            new Mat("titanium",       Materials.Titanium,            9.0F, 112, 448, "Titanium"),
            new Mat("netherite",      Materials.Netherite,     9.0F, 112, 448, "Netherite"),
            new Mat("tungsten",       Materials.Tungsten,            10.0F, 128, 512, "Tungsten"),
            new Mat("tungsten_steel", Materials.Tungstensteel,12.5F, 128, 512, "Tungsten Steel"),
            new Mat("ultimet",        Materials.Ultimet,      12.5F, 256,1024, "Ultimet"),
    };

    public static java.util.List<BoilerSpec> all() {
        java.util.List<BoilerSpec> out=new java.util.ArrayList<>();
        for (Mat m : MATS) {
            out.add(new BoilerSpec("steam_boiler_" + m.id(), m.mat(), false, m.normal() * 2L, m.hardness(),
                    "Steam Boiler Tank (" + m.en() + ")"));
        }
        for (Mat m : MATS) {
            out.add(new BoilerSpec("strong_steam_boiler_" + m.id(), m.mat(), true, m.strong() * 2L, m.hardness(),
                    "Strong Steam Boiler Tank (" + m.en() + ")"));
        }
        return java.util.List.copyOf(out);
    }
}
