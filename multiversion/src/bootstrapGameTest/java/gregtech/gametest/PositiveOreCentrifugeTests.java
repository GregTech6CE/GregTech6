package gregtech.gametest;

import gregtech.internal.addon.AddonMaterials;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.material.MaterialFormItem;
import gregtech.internal.material.ItemMaterialRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import java.util.Locale;
import gregtech.data.MachineRecipeMaps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Literal original Ores464-470 expectations over actual accepted startup recipes; no row insertion. */
@GameTestHolder("gregtech_ore_source_audit")
@PrefixGameTestTemplate(false)
public final class PositiveOreCentrifugeTests {
    private static void check(GameTestHelper h, String material, int id, int quality,
                              String first, String second, String third, boolean tiny, long ticks) {
        var source = GTMaterialRegistry.get(material).resolve();
        h.assertTrue(source.isValid() && source.getId() == id && GTMaterialRegistry.isCoreMaterial(source)
                        && !AddonMaterials.owns(source) && source.getToolQuality() == quality,
                "reviewed builtin identity and source quality: " + material);
        var products = source.getByProducts();
        h.assertTrue(products.size() >= 3 && products.get(0).resolve().getName().equals(first)
                        && products.get(1).resolve().getName().equals(second)
                        && products.get(2).resolve().getName().equals(third),
                "source byproduct order prerequisite: " + material);
        // The enabled sample addon creates this unrelated custom prefix through METAL+MOLTEN.
        // Require the actual bound item that the ownership scan sees; missing fixtures cannot mask overbroad deferral.
        String witness = switch (material) {
            case "Copper" -> "Copper";
            case "Galena" -> "Lead";
            case "NetherQuartz" -> "Efrine";
            default -> throw new AssertionError("Unreviewed fixture " + material);
        };
        var witnessId = ResourceLocation.parse("addon_sample:sample_blank_" + witness.toLowerCase(Locale.ROOT));
        var witnessItem = BuiltInRegistries.ITEM.get(witnessId);
        h.assertTrue(BuiltInRegistries.ITEM.containsKey(witnessId) && ItemMaterialRegistry.entries().containsKey(witnessItem)
                        && witnessItem instanceof MaterialFormItem form && form.getPrefix().getName().equals("sampleBlank")
                        && form.getMaterial().resolve() == GTMaterialRegistry.get(witness).resolve(),
                "actual unrelated addon form must coexist with builtin ore recipe: " + witnessId);
        int count = tiny ? 1 : 9;
        String input = tiny ? "crushedPurifiedTiny" : "crushedPurified";
        var outputs = new ItemStack[]{
                OreSourceDifferentialTests.s("crushedCentrifugedTiny", material, count),
                OreSourceDifferentialTests.s("crushedCentrifugedTiny", first, count),
                OreSourceDifferentialTests.s("crushedCentrifugedTiny", second, count),
                OreSourceDifferentialTests.s("crushedCentrifugedTiny", third, count)};
        OreSourceDifferentialTests.row(h, "original Ores464-470 " + input + ':' + material,
                MachineRecipeMaps.Centrifuge, OreSourceDifferentialTests.s(input, material, 1),
                new FluidStack[0], outputs, new long[]{10000, 1000, 1000, 1000}, new FluidStack[0], ticks, 16);
        h.succeed();
    }

    @GameTest(template = "test_empty", batch = "positive_ore_centrifuge", timeoutTicks = 100)
    public static void copperPurifiedWholeHasThreeSourceByproducts(GameTestHelper h) {
        check(h, "Copper", 290, 0, "Cobaltite", "Gold", "Nickel", false, 144);
    }
    @GameTest(template = "test_empty", batch = "positive_ore_centrifuge", timeoutTicks = 100)
    public static void copperPurifiedTinyKeepsSourceGradeAndWork(GameTestHelper h) {
        check(h, "Copper", 290, 0, "Cobaltite", "Gold", "Nickel", true, 16);
    }
    @GameTest(template = "test_empty", batch = "positive_ore_centrifuge", timeoutTicks = 100)
    public static void galenaPurifiedWholeHasThreeSourceByproducts(GameTestHelper h) {
        check(h, "Galena", 9117, 0, "Sphalerite", "Silver", "Lead", false, 144);
    }
    @GameTest(template = "test_empty", batch = "positive_ore_centrifuge", timeoutTicks = 100)
    public static void galenaPurifiedTinyKeepsSourceGradeAndWork(GameTestHelper h) {
        check(h, "Galena", 9117, 0, "Sphalerite", "Silver", "Lead", true, 16);
    }
    @GameTest(template = "test_empty", batch = "positive_ore_centrifuge", timeoutTicks = 100)
    public static void netherQuartzPurifiedWholeUsesQualityThreeWork(GameTestHelper h) {
        check(h, "NetherQuartz", 8346, 3, "Barite", "Efrine", "VoidQuartz", false, 576);
    }
    @GameTest(template = "test_empty", batch = "positive_ore_centrifuge", timeoutTicks = 100)
    public static void netherQuartzPurifiedTinyKeepsSourceGradeAndWork(GameTestHelper h) {
        check(h, "NetherQuartz", 8346, 3, "Barite", "Efrine", "VoidQuartz", true, 64);
    }
}
