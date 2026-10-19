package com.gregtech.gregtech.content.energy;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.machine.BoilerSpec;

/** Single-block boiler ratings from GT6 Loader_MultiTileEntities, IDs 1200–1212 and 1250–1262. */
public final class BoilerCatalog {
    private BoilerCatalog() {}

    /** Table outputs are multiplied by STEAM_PER_EU (2) when constructing a specification. */
    private record MaterialVariant(String id, GTMaterial material, float hardness,
                                   int normalOutput, int strongOutput, String displayName) {}

    private static final MaterialVariant[] VARIANTS = {
            new MaterialVariant("lead",           Materials.Lead,          4.0F,  16,  64,   "Lead"),
            new MaterialVariant("bismuth",        Materials.Bismuth,       4.0F,  20,  80,   "Bismuth"),
            new MaterialVariant("bronze",         Materials.Bronze,        7.0F,  24,  96,   "Bronze"),
            new MaterialVariant("arsenic_copper", Materials.ArsenicCopper, 7.0F,  24,  96,   "Arsenic Copper"),
            new MaterialVariant("arsenic_bronze", Materials.ArsenicBronze, 7.0F,  28,  112,  "Arsenic Bronze"),
            new MaterialVariant("invar",          Materials.Invar,         4.0F,  16,  64,   "Invar"),
            new MaterialVariant("steel",          Materials.Steel,         6.0F,  32,  128,  "Steel"),
            new MaterialVariant("chromium",       Materials.Chromium,      4.0F,  96,  384,  "Chromium"),
            new MaterialVariant("titanium",       Materials.Titanium,      9.0F,  112, 448,  "Titanium"),
            new MaterialVariant("netherite",      Materials.Netherite,     9.0F,  112, 448,  "Netherite"),
            new MaterialVariant("tungsten",       Materials.Tungsten,      10.0F, 128, 512,  "Tungsten"),
            new MaterialVariant("tungsten_steel", Materials.Tungstensteel, 12.5F, 128, 512,  "Tungsten Steel"),
            new MaterialVariant("ultimet",        Materials.Ultimet,       12.5F, 256, 1024, "Ultimet"),
    };

    public static java.util.List<BoilerSpec> all() {
        java.util.List<BoilerSpec> specifications = new java.util.ArrayList<>();
        for (MaterialVariant variant : VARIANTS) {
            specifications.add(new BoilerSpec("steam_boiler_" + variant.id(), variant.material(), false, variant.normalOutput() * 2L, variant.hardness(),
                    "Steam Boiler Tank (" + variant.displayName() + ")"));
        }
        for (MaterialVariant variant : VARIANTS) {
            specifications.add(new BoilerSpec("strong_steam_boiler_" + variant.id(), variant.material(), true, variant.strongOutput() * 2L, variant.hardness(),
                    "Strong Steam Boiler Tank (" + variant.displayName() + ")"));
        }
        return java.util.List.copyOf(specifications);
    }
}
