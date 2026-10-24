package gregtech.gametest;

import gregtech.internal.addon.AddonMaterials;
import gregtech.api.material.*;
import gregtech.internal.material.*;
import gregtech.internal.prefix.BlockMaterialPrefix;
import gregtech.block.MaterialBlockLike;
import gregtech.data.BlockHarvestPolicy;
import gregtech.data.MachineRecipeMaps;
import gregtech.data.MaterialPrefix;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.Locale;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Literal MT qual(long) metadata and accepted startup recipes; no mutations or recipe insertion. */
@GameTestHolder("gregtech_ore_source_audit")
@PrefixGameTestTemplate(false)
public final class OreQualityMetadataTests {
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
    @GameTest(template = "test_empty", batch = "ore_quality_metadata", timeoutTicks = 100)
    public static void reviewedHarvestQualityPreservesNativeOreLevelsAndLegacyForms(GameTestHelper h) {
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
            h.assertTrue(m.isValid() && m.getName().equals(expected.name()) && GTMaterialRegistry.isCoreMaterial(m)
                    && !AddonMaterials.owns(m), "reviewed core identity: " + expected.name());
            h.assertTrue(m.getToolTypes()==0 && m.getToolSpeed()==1.0F && m.getToolDurability()==1
                    && m.getToolQuality()==expected.quality(), "source qual(long) metadata: " + expected.name());
            // Positive harvest-quality ores had no tool-head IDs before this metadata repair.
            // The existing explicit TOOL_HEAD policy on the non-ore carbide is preserved separately.
            if (expected.quality()>0 && expected.id()!=8802) for (var prefix : heads) {
                var id=ResourceLocation.parse("gt6ce:" + prefix.getRegistryName() + '_' + m.getName().toLowerCase(Locale.ROOT));
                h.assertTrue(!BuiltInRegistries.ITEM.containsKey(id), "harvest quality cannot add source-type0 native head: " + id);
            }
        }
        for (var expected : new Expected[]{new Expected(9105,"Bauxite",2),new Expected(9192,"Rutile",2),
                new Expected(9128,"Scheelite",3)}) {
            var id=ResourceLocation.parse("gt6ce:ore_"+expected.name().toLowerCase(Locale.ROOT));
            var block=BuiltInRegistries.BLOCK.get(id);
            h.assertTrue(BuiltInRegistries.BLOCK.containsKey(id) && block instanceof MaterialBlockLike form
                    && form.material().resolve()==GTMaterialRegistry.get(expected.id()).resolve()
                    && form.prefix()==BlockMaterialPrefix.ore, "actual native standard ore block: " + id);
            h.assertTrue(BlockHarvestPolicy.level(block)==expected.quality(), "native ore harvest level follows source metadata: " + id);
        }
        for (String name : new String[]{"AncientDebris","TantalumHafniumCarbide","Stone"}) {
            var stack=OreSourceDifferentialTests.s("toolHeadPickaxe",name,1);
            var id=ResourceLocation.parse("gt6ce:tool_head_pickaxe_"+name.toLowerCase(Locale.ROOT));
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(id) && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id),
                    "unrelated/explicit existing native head identity preserved: " + name);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", batch = "ore_quality_metadata", timeoutTicks = 100)
    public static void bauxiteAndScheeliteQualityScaleAcceptedCrusherHammerWork(GameTestHelper h) {
        // Original HandlerCrushing and RecipeMapHammer scale source work by (quality + 1).
        // Literal durations ensure the corrected property, not a test reimplementation, reaches real maps.
        accepted(h,"Bauxite",9105,2,384,96);
        accepted(h,"Scheelite",9128,3,512,128);
        h.succeed();
    }
    private static void accepted(GameTestHelper h,String name,int id,int quality,long crusherTicks,long hammerTicks) {
        var m=GTMaterialRegistry.get(name).resolve();
        h.assertTrue(m.getId()==id && m.getTargetCrushingMaterial().resolve()==m
                && m.getTargetCrushingAmount()==GTValues.U && m.getOreMultiplier()==1 && m.getOreProcessingMultiplier()==1,
                "reviewed source raw-input identity and one-unit crushing target: " + name
                        + " actual id=" + m.getId() + " quality=" + m.getToolQuality()
                        + " target=" + m.getTargetCrushingMaterial().resolve().getName()
                        + " self=" + (m.getTargetCrushingMaterial().resolve()==m)
                        + " amount=" + m.getTargetCrushingAmount() + " U=" + GTValues.U
                        + " oreMultiplier=" + m.getOreMultiplier()
                        + " processingMultiplier=" + m.getOreProcessingMultiplier());
        var input=OreSourceDifferentialTests.s("oreRaw",name,1);
        var output=OreSourceDifferentialTests.s("crushed",name,1);
        OreSourceDifferentialTests.row(h,"source quality raw Crusher "+name,MachineRecipeMaps.Crusher,input,
                new FluidStack[0],new ItemStack[]{output,output.copy()},new long[]{10000,10000},new FluidStack[0],crusherTicks,16);
        OreSourceDifferentialTests.row(h,"source quality raw Hammer "+name,MachineRecipeMaps.Hammer,input,
                new FluidStack[0],new ItemStack[]{output},new long[]{10000},new FluidStack[0],hammerTicks,16);
        // Keep the source quality requirement, but let a baseline reach actual recipe-work checks first.
        h.assertTrue(m.getToolQuality()==quality, "source quality after accepted Crusher/Hammer rows: " + name
                + " expected=" + quality + " actual=" + m.getToolQuality());
    }
}
