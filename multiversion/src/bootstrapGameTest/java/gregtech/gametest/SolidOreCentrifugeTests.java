package gregtech.gametest;

import gregtech.api.addon.AddonMaterials;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTMaterialRegistry;
import gregtech.api.material.ItemMaterialRegistry;
import gregtech.api.material.MaterialFormItem;
import gregtech.data.MachineRecipeMaps;
import gregtech.data.MaterialPrefix;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
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

/** Literal original Ores464-470 source-input rows over accepted startup maps, not machine execution.
 * No producer invocation, expected-row insertion, runtime-derived quality/work or source-flag inference.
 */
@GameTestHolder("gregtech_ore_source_audit")
@PrefixGameTestTemplate(false)
public final class SolidOreCentrifugeTests {
    private record Expected(int id, String name, int melting, String path) {}
    private static final Expected ANTIMONY = new Expected(510, "Antimony", 903, "antimony");
    private static final Expected BROWN_LIMONITE = new Expected(9106, "BrownLimonite", 1523, "brownlimonite");
    private static final Expected CADMIUM = new Expected(480, "Cadmium", 594, "cadmium");
    private static final Expected CHALCOPYRITE = new Expected(9111, "Chalcopyrite", 986, "chalcopyrite");
    private static final Expected COBALTITE = new Expected(9115, "Cobaltite", 1014, "cobaltite");
    private static final Expected COOPERITE = new Expected(9116, "Cooperite", 1677, "cooperite");
    private static final Expected COPPER = new Expected(290, "Copper", 1357, "copper");
    private static final Expected GALLIUM = new Expected(310, "Gallium", 302, "gallium");
    private static final Expected HEMATITE = new Expected(9104, "Hematite", 1207, "hematite");
    private static final Expected IRIDIUM = new Expected(770, "Iridium", 2719, "iridium");
    private static final Expected NICKEL = new Expected(280, "Nickel", 1728, "nickel");
    private static final Expected PALLADIUM = new Expected(460, "Palladium", 1828, "palladium");
    private static final Expected PENTLANDITE = new Expected(9145, "Pentlandite", 1097, "pentlandite");
    private static final Expected PHOSPHORUS = new Expected(8208, "Phosphorus", 829, "phosphorus");
    private static final Expected PYRITE = new Expected(9125, "Pyrite", 862, "pyrite");
    private static final Expected SPHALERITE = new Expected(9130, "Sphalerite", 540, "sphalerite");
    private static final Expected SULFUR = new Expected(160, "Sulfur", 388, "sulfur");
    private static final Expected TETRAHEDRITE = new Expected(9132, "Tetrahedrite", 993, "tetrahedrite");
    private static final Expected YELLOW_LIMONITE = new Expected(9137, "YellowLimonite", 1523, "yellowlimonite");
    private static final Expected ZINC = new Expected(300, "Zinc", 692, "zinc");

    private static GTMaterial material(GameTestHelper h, Expected expected) {
        var found = GTMaterialRegistry.get(expected.name());
        h.assertTrue(found != null && found.isValid(), "required reviewed material " + expected.name());
        var m = found.resolve();
        h.assertTrue(GTMaterialRegistry.isCoreMaterial(m) && !AddonMaterials.owns(m)
                        && m.getId() == expected.id() && m.getName().equals(expected.name())
                        && m.getMeltingPoint() == expected.melting(),
                "literal builtin identity and thermal prerequisite: " + expected.name());
        return m;
    }

    private static ItemStack form(GameTestHelper h, MaterialPrefix prefix, String path, Expected expected, int count) {
        var m = material(h, expected);
        var stack = OreSourceDifferentialTests.s(prefix.getName(), expected.name(), count);
        var id = ResourceLocation.parse("gt6ce:" + path + "_" + expected.path());
        h.assertTrue(BuiltInRegistries.ITEM.containsKey(id) && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id)
                        && stack.getCount() == count && stack.getItem() instanceof MaterialFormItem bound
                        && bound.getPrefix() == prefix && bound.getMaterial().resolve() == m,
                "exact native registry/form/material identity: " + id);
        return stack;
    }

    private static void addonWitness(GameTestHelper h, Expected expected) {
        var id = ResourceLocation.parse("addon_sample:sample_blank_" + expected.path());
        var item = BuiltInRegistries.ITEM.get(id);
        h.assertTrue(BuiltInRegistries.ITEM.containsKey(id) && ItemMaterialRegistry.entries().containsKey(item)
                        && item instanceof MaterialFormItem bound && bound.getPrefix().getName().equals("sampleBlank")
                        && bound.getMaterial().resolve() == material(h, expected),
                "actual unrelated addon form must remain present: " + id);
    }

    private static void check(GameTestHelper h, Expected source, int quality,
                              Expected first, Expected second, Expected third, Expected witness,
                              boolean tiny, long ticks) {
        var m = material(h, source);
        h.assertTrue(m.getToolQuality() == quality, "literal source quality: " + source.name());
        var byproducts = m.getByProducts();
        h.assertTrue(byproducts.size() >= 3 && byproducts.get(0).resolve() == material(h, first)
                        && byproducts.get(1).resolve() == material(h, second)
                        && byproducts.get(2).resolve() == material(h, third),
                "literal first-three source byproduct order: " + source.name());
        // Metal source/byproducts use their actual sampleBlank witness. Pentlandite/Pyrite have no
        // reviewed METAL+MOLTEN identity; their Copper witness is only the shared addon-enabled control.
        // Do not invent sampleBlank ore items for nonmetal materials to make this fixture pass.
        addonWitness(h, witness);
        int count = tiny ? 1 : 9;
        var input = form(h, tiny ? MaterialPrefix.crushedPurifiedTiny : MaterialPrefix.crushedPurified,
                tiny ? "crushed_purified_tiny" : "crushed_purified", source, 1);
        var outputs = new ItemStack[]{
                form(h, MaterialPrefix.crushedCentrifugedTiny, "crushed_centrifuged_tiny", source, count),
                form(h, MaterialPrefix.crushedCentrifugedTiny, "crushed_centrifuged_tiny", first, count),
                form(h, MaterialPrefix.crushedCentrifugedTiny, "crushed_centrifuged_tiny", second, count),
                form(h, MaterialPrefix.crushedCentrifugedTiny, "crushed_centrifuged_tiny", third, count)};
        OreSourceDifferentialTests.row(h, "original Ores464-470 " + source.name() + (tiny ? " tiny" : " whole"),
                MachineRecipeMaps.Centrifuge, input, new FluidStack[0], outputs,
                new long[]{10000, 1000, 1000, 1000}, new FluidStack[0], ticks, 16);
        h.succeed();
    }

    @GameTest(template = "test_empty", batch = "solid_ore_centrifuge", timeoutTicks = 100)
    public static void chalcopyritePurifiedWhole(GameTestHelper h) {
        check(h, CHALCOPYRITE, 0, PYRITE, COBALTITE, CADMIUM, CADMIUM, false, 144);
    }

    @GameTest(template = "test_empty", batch = "solid_ore_centrifuge", timeoutTicks = 100)
    public static void chalcopyritePurifiedTiny(GameTestHelper h) {
        check(h, CHALCOPYRITE, 0, PYRITE, COBALTITE, CADMIUM, CADMIUM, true, 16);
    }

    @GameTest(template = "test_empty", batch = "solid_ore_centrifuge", timeoutTicks = 100)
    public static void pentlanditePurifiedWhole(GameTestHelper h) {
        check(h, PENTLANDITE, 0, HEMATITE, SULFUR, COBALTITE, COPPER, false, 144);
    }

    @GameTest(template = "test_empty", batch = "solid_ore_centrifuge", timeoutTicks = 100)
    public static void pentlanditePurifiedTiny(GameTestHelper h) {
        check(h, PENTLANDITE, 0, HEMATITE, SULFUR, COBALTITE, COPPER, true, 16);
    }

    @GameTest(template = "test_empty", batch = "solid_ore_centrifuge", timeoutTicks = 100)
    public static void sphaleritePurifiedWhole(GameTestHelper h) {
        check(h, SPHALERITE, 0, CADMIUM, GALLIUM, ZINC, CADMIUM, false, 144);
    }

    @GameTest(template = "test_empty", batch = "solid_ore_centrifuge", timeoutTicks = 100)
    public static void sphaleritePurifiedTiny(GameTestHelper h) {
        check(h, SPHALERITE, 0, CADMIUM, GALLIUM, ZINC, CADMIUM, true, 16);
    }

    @GameTest(template = "test_empty", batch = "solid_ore_centrifuge", timeoutTicks = 100)
    public static void tetrahedritePurifiedWhole(GameTestHelper h) {
        check(h, TETRAHEDRITE, 0, COPPER, ANTIMONY, ZINC, COPPER, false, 144);
    }

    @GameTest(template = "test_empty", batch = "solid_ore_centrifuge", timeoutTicks = 100)
    public static void tetrahedritePurifiedTiny(GameTestHelper h) {
        check(h, TETRAHEDRITE, 0, COPPER, ANTIMONY, ZINC, COPPER, true, 16);
    }

    @GameTest(template = "test_empty", batch = "solid_ore_centrifuge", timeoutTicks = 100)
    public static void pyritePurifiedWhole(GameTestHelper h) {
        check(h, PYRITE, 0, SULFUR, PHOSPHORUS, HEMATITE, COPPER, false, 144);
    }

    @GameTest(template = "test_empty", batch = "solid_ore_centrifuge", timeoutTicks = 100)
    public static void pyritePurifiedTiny(GameTestHelper h) {
        check(h, PYRITE, 0, SULFUR, PHOSPHORUS, HEMATITE, COPPER, true, 16);
    }

    @GameTest(template = "test_empty", batch = "solid_ore_centrifuge", timeoutTicks = 100)
    public static void nickelPurifiedWhole(GameTestHelper h) {
        check(h, NICKEL, 2, COBALTITE, COOPERITE, HEMATITE, NICKEL, false, 432);
    }

    @GameTest(template = "test_empty", batch = "solid_ore_centrifuge", timeoutTicks = 100)
    public static void nickelPurifiedTiny(GameTestHelper h) {
        check(h, NICKEL, 2, COBALTITE, COOPERITE, HEMATITE, NICKEL, true, 48);
    }

    @GameTest(template = "test_empty", batch = "solid_ore_centrifuge", timeoutTicks = 100)
    public static void cooperitePurifiedWhole(GameTestHelper h) {
        check(h, COOPERITE, 0, PALLADIUM, NICKEL, IRIDIUM, PALLADIUM, false, 144);
    }

    @GameTest(template = "test_empty", batch = "solid_ore_centrifuge", timeoutTicks = 100)
    public static void cooperitePurifiedTiny(GameTestHelper h) {
        check(h, COOPERITE, 0, PALLADIUM, NICKEL, IRIDIUM, PALLADIUM, true, 16);
    }

    @GameTest(template = "test_empty", batch = "solid_ore_centrifuge", timeoutTicks = 100)
    public static void yellowLimonitePurifiedWhole(GameTestHelper h) {
        check(h, YELLOW_LIMONITE, 0, NICKEL, BROWN_LIMONITE, COBALTITE, NICKEL, false, 144);
    }

    @GameTest(template = "test_empty", batch = "solid_ore_centrifuge", timeoutTicks = 100)
    public static void yellowLimonitePurifiedTiny(GameTestHelper h) {
        check(h, YELLOW_LIMONITE, 0, NICKEL, BROWN_LIMONITE, COBALTITE, NICKEL, true, 16);
    }
}
