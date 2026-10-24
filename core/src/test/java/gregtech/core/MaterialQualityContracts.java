package gregtech.core;

import gregtech.api.material.*;
import gregtech.internal.material.*;
import gregtech.internal.mod.ModData;
import gregtech.internal.prefix.BlockMaterialPrefix;
import gregtech.content.data.MaterialPrefix;

/** Pinned original MT numeric qual(long) rows; no broad quality inference or recipe rewrite. */
public final class MaterialQualityContracts {
    private record Expected(int id, String name, int quality) {}
    private static final Expected[] SOURCE = {
            new Expected(9192, "Rutile", 2),
            new Expected(9104, "Hematite", 0),
            new Expected(9125, "Pyrite", 0),
            new Expected(8333, "Redstone", 0),
            new Expected(8340, "Nikolite", 0),
            new Expected(8802, "TantalumHafniumCarbide", 2),
            new Expected(9118, "Garnierite", 0),
            new Expected(9122, "Magnetite", 0),
            new Expected(9003, "BasalticMineralSand", 0),
            new Expected(9004, "GraniticMineralSand", 0),
            new Expected(9109, "Realgar", 0),
            new Expected(9145, "Pentlandite", 0),
            new Expected(9111, "Chalcopyrite", 0),
            new Expected(9216, "Arsenopyrite", 0),
            new Expected(9115, "Cobaltite", 0),
            new Expected(9128, "Scheelite", 3),
            new Expected(9217, "Wolframite", 3),
            new Expected(9194, "Ferberite", 3),
            new Expected(9195, "Huebnerite", 3),
            new Expected(9133, "Tungstate", 3),
            new Expected(9193, "Stolzite", 3),
            new Expected(9196, "Russellite", 3),
            new Expected(9197, "Pinalite", 3),
            new Expected(9106, "BrownLimonite", 0),
            new Expected(9137, "YellowLimonite", 0),
            new Expected(9120, "Ilmenite", 2),
            new Expected(9105, "Bauxite", 2),
            new Expected(9113, "Chromite", 0),
            new Expected(9198, "DiduraniumTrioxide", 4),
            new Expected(9199, "DuraniumHexafluoride", 4),
            new Expected(9200, "DuraniumHexachloride", 4),
            new Expected(9201, "DuraniumHexabromide", 4),
            new Expected(9202, "DuraniumHexaiodide", 4),
            new Expected(9203, "DuraniumHexaastatide", 4),
            new Expected(9204, "TritaniumDioxide", 4),
            new Expected(9205, "TritaniumHexafluoride", 4),
            new Expected(9206, "TritaniumHexachloride", 4),
            new Expected(9207, "TritaniumHexabromide", 4),
            new Expected(9208, "TritaniumHexaiodide", 4),
            new Expected(9209, "TritaniumHexaastatide", 4),
    };
    private static int assertions;
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.init(); GTMaterialRegistry.postInit(); BlockMaterialPrefix.bootstrap();
        MaterialPrefix[] heads = {MaterialPrefix.toolHeadSword, MaterialPrefix.toolHeadPickaxe,
                MaterialPrefix.toolHeadShovel, MaterialPrefix.toolHeadSpade, MaterialPrefix.toolHeadAxe,
                MaterialPrefix.toolHeadAxeDouble, MaterialPrefix.toolHeadHoe, MaterialPrefix.toolHeadHammer,
                MaterialPrefix.toolHeadSense, MaterialPrefix.toolHeadPlow, MaterialPrefix.toolHeadRawSword,
                MaterialPrefix.toolHeadRawPickaxe, MaterialPrefix.toolHeadConstructionPickaxe,
                MaterialPrefix.toolHeadRawShovel, MaterialPrefix.toolHeadRawSpade, MaterialPrefix.toolHeadRawAxe,
                MaterialPrefix.toolHeadRawAxeDouble, MaterialPrefix.toolHeadRawHoe, MaterialPrefix.toolHeadRawSense,
                MaterialPrefix.toolHeadRawPlow};
        for (var expected : SOURCE) {
            var m = GTMaterialRegistry.get(expected.id()).resolve();
            check(m.isValid() && m.getName().equals(expected.name()) && GTMaterialRegistry.isCoreMaterial(m),
                    "exact source identity " + expected.name());
            check(m.getToolQuality() == expected.quality(), "source harvest quality " + expected.name()
                    + ": expected " + expected.quality() + ", got " + m.getToolQuality());
            check(m.getToolTypes() == 0 && Float.compare(m.getToolSpeed(), 1.0F) == 0 && m.getToolDurability() == 1,
                    "qual(long) preserves default type/speed and source minimum durability: " + expected.name());
            check(BlockMaterialPrefix.ore.harvestLevel(m) == expected.quality(),
                    "standard ore source harvest level: " + expected.name());
            // Compare only this cohort's quality0 legacy form policy in the isolated contract JVM.
            // Quality is restored even on failure; this is not a fake native registry observation.
            try {
                m.setToolStats(0, 1.0F, 1, 0);
                boolean[] before = new boolean[heads.length];
                for (int i = 0; i < heads.length; i++) before[i] = heads[i].isValidFor(m);
                m.setToolStats(0, 1.0F, 1, expected.quality());
                for (int i = 0; i < heads.length; i++) check(before[i] == heads[i].isValidFor(m),
                        "harvest quality must not create legacy tool forms: " + expected.name() + '/' + heads[i].getName());
            } finally { m.setToolStats(0, 1.0F, 1, expected.quality()); }
        }
        check(!MaterialPrefix.toolHeadPickaxe.isValidFor(GTMaterialRegistry.get("Bauxite")), "Bauxite is not a source tool material");
        check(MaterialPrefix.toolHeadPickaxe.isValidFor(GTMaterialRegistry.get("AncientDebris")), "unreviewed legacy Ancient Debris head preserved");
        check(MaterialPrefix.toolHeadPickaxe.isValidFor(GTMaterialRegistry.get("TantalumHafniumCarbide")), "explicit legacy TOOL_HEAD arm preserved");
        check(MaterialPrefix.toolHeadPickaxe.isValidFor(GTMaterialRegistry.get("Stone")), "existing source stone head preserved");
        check(GTMaterialRegistry.get("Copper").getToolQuality() == 0 && GTMaterialRegistry.get("Iron").getToolQuality() == 2,
                "existing four-argument quality contracts unchanged");
        // Reflection keeps this tests-only source compilable against the unfixed baseline API.
        var setter = GTMaterial.class.getMethod("setToolQuality", int.class);
        var m = GTMaterialRegistry.get("Copper");
        int type=m.getToolTypes(), quality=m.getToolQuality(); float speed=m.getToolSpeed(); long durability=m.getToolDurability();
        var properties=java.util.Set.copyOf(m.getProperties());
        try {
            m.setToolStats(2, 3.5F, 77, 0);
            check(setter.invoke(m, 3) == m && m.getToolTypes()==2 && m.getToolSpeed()==3.5F
                    && m.getToolDurability()==77 && m.getToolQuality()==3, "harvest setter preserves inherited nondefault stats");
            m.setToolStats(9, 3.5F, 0, 0);
            setter.invoke(m, -2);
            check(m.getToolTypes()==3 && m.getToolSpeed()==3.5F && m.getToolDurability()==1 && m.getToolQuality()==0,
                    "source bind2, durability minimum, and lower bind4 clamp");
            setter.invoke(m, 99);
            check(m.getToolQuality()==15, "source upper bind4 clamp");
            check(m.getProperties().equals(properties), "harvest setter does not invent form flags");
        } finally { m.setToolStats(type, speed, durability, quality); }
        System.out.println(assertions + " source material quality assertions passed");
    }
}
