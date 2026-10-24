package gregtech.gametest;

import gregtech.api.addon.AddonMaterials;
import gregtech.api.material.*;
import gregtech.api.prefix.PrefixRegistry;
import gregtech.data.MachineRecipeMaps;
import gregtech.data.MaterialPrefix;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.Arrays;
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

/** Literal original Handlers145-150 rows; accepted map/lookup tests, not full machine execution.
 * No row insertion/producer call. Repeated Div72 slots and fixed durations remain independent expectations.
 */
@GameTestHolder("gregtech_ore_source_audit")
@PrefixGameTestTemplate(false)
public final class OreShredderSourceTests {
    private static ItemStack form(GameTestHelper h, String prefixName, String material, int count) {
        var prefix = PrefixRegistry.byName(prefixName);
        String path = switch (prefixName) {
            case "crushed" -> "crushed";
            case "crushedPurified" -> "crushed_purified";
            case "crushedCentrifuged" -> "crushed_centrifuged";
            case "crushedTiny" -> "crushed_tiny";
            case "crushedPurifiedTiny" -> "crushed_purified_tiny";
            case "crushedCentrifugedTiny" -> "crushed_centrifuged_tiny";
            case "dust" -> "dust";
            case "dustSmall" -> "dust_small";
            case "dustTiny" -> "dust_tiny";
            case "dustDiv72" -> "dust_div72";
            default -> throw new AssertionError("Unreviewed form " + prefixName);
        };
        var stack = OreSourceDifferentialTests.s(prefixName, material, count);
        var id = ResourceLocation.parse("gt6ce:" + path + '_' + material.toLowerCase(Locale.ROOT));
        h.assertTrue(BuiltInRegistries.ITEM.containsKey(id) && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id)
                        && stack.getCount() == count && stack.getItem() instanceof MaterialFormItem bound
                        && bound.getPrefix() == prefix && bound.getMaterial().resolve() == GTMaterialRegistry.get(material).resolve(),
                "literal native registry/form/material identity " + id);
        return stack;
    }
    private static void check(GameTestHelper h, String name, int id, int quality, String inputPrefix,
                              String[] outputPrefixes, long ticks, String oldCrusherPrefix, int oldCrusherCount, long oldCrusherTicks) {
        var source = GTMaterialRegistry.get(name).resolve();
        h.assertTrue(source.isValid() && source.getId() == id && source.getName().equals(name)
                        && GTMaterialRegistry.isCoreMaterial(source) && !AddonMaterials.owns(source)
                        && source.getToolQuality() == quality && source.getTargetPulverMaterial().resolve() == source
                        && source.getTargetPulverAmount() == GTValues.U,
                "reviewed source identity/quality/self pulver target/U: " + name);
        // Only Copper/Nickel themselves match the sample addon METAL+MOLTEN declaration in this roster.
        // Other owners reuse Copper solely as an enabled-addon control, not as a relevant form claim.
        String witness = name.equals("Nickel") ? "Nickel" : "Copper";
        var witnessId = ResourceLocation.parse("addon_sample:sample_blank_" + witness.toLowerCase(Locale.ROOT));
        var witnessItem = BuiltInRegistries.ITEM.get(witnessId);
        h.assertTrue(BuiltInRegistries.ITEM.containsKey(witnessId) && ItemMaterialRegistry.entries().containsKey(witnessItem)
                        && witnessItem instanceof MaterialFormItem bound && bound.getPrefix().getName().equals("sampleBlank")
                        && bound.getMaterial().resolve() == GTMaterialRegistry.get(witness).resolve(),
                "real unrelated addon form/control remains present: " + witnessId);
        var input = form(h, inputPrefix, name, 1);
        // This batch must not change the adjacent legacy Crusher producer; not a Crusher parity claim.
        OreSourceDifferentialTests.row(h, "unchanged Crusher " + name + '/' + inputPrefix,
                MachineRecipeMaps.Crusher, input, new FluidStack[0],
                new ItemStack[]{form(h, oldCrusherPrefix, name, oldCrusherCount)}, new long[]{10000},
                new FluidStack[0], oldCrusherTicks, 16);
        var outputs = new ItemStack[outputPrefixes.length];
        for (int i = 0; i < outputs.length; i++) outputs[i] = form(h, outputPrefixes[i], name, 1);
        long[] chances = new long[outputs.length]; Arrays.fill(chances, 10000);
        OreSourceDifferentialTests.row(h, "original Handlers145-150 " + name + '/' + inputPrefix,
                MachineRecipeMaps.Shredder, input, new FluidStack[0], outputs, chances, new FluidStack[0], ticks, 16);
        h.succeed();
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void copperCrushed(GameTestHelper h) {
        check(h, "Copper", 290, 0, "crushed", new String[]{"dust", "dustTiny", "dustDiv72"}, 18, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void copperCrushedPurified(GameTestHelper h) {
        check(h, "Copper", 290, 0, "crushedPurified", new String[]{"dust", "dustSmall"}, 20, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void copperCrushedCentrifuged(GameTestHelper h) {
        check(h, "Copper", 290, 0, "crushedCentrifuged", new String[]{"dust", "dustSmall", "dustTiny", "dustDiv72"}, 22, "dustTiny", 11, 32);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void copperCrushedTiny(GameTestHelper h) {
        check(h, "Copper", 290, 0, "crushedTiny", new String[]{"dustTiny", "dustDiv72"}, 2, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void copperCrushedPurifiedTiny(GameTestHelper h) {
        check(h, "Copper", 290, 0, "crushedPurifiedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void copperCrushedCentrifugedTiny(GameTestHelper h) {
        check(h, "Copper", 290, 0, "crushedCentrifugedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void galenaCrushed(GameTestHelper h) {
        check(h, "Galena", 9117, 0, "crushed", new String[]{"dust", "dustTiny", "dustDiv72"}, 18, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void galenaCrushedPurified(GameTestHelper h) {
        check(h, "Galena", 9117, 0, "crushedPurified", new String[]{"dust", "dustSmall"}, 20, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void galenaCrushedCentrifuged(GameTestHelper h) {
        check(h, "Galena", 9117, 0, "crushedCentrifuged", new String[]{"dust", "dustSmall", "dustTiny", "dustDiv72"}, 22, "dustTiny", 11, 32);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void galenaCrushedTiny(GameTestHelper h) {
        check(h, "Galena", 9117, 0, "crushedTiny", new String[]{"dustTiny", "dustDiv72"}, 2, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void galenaCrushedPurifiedTiny(GameTestHelper h) {
        check(h, "Galena", 9117, 0, "crushedPurifiedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void galenaCrushedCentrifugedTiny(GameTestHelper h) {
        check(h, "Galena", 9117, 0, "crushedCentrifugedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void netherQuartzCrushed(GameTestHelper h) {
        check(h, "NetherQuartz", 8346, 3, "crushed", new String[]{"dust", "dustTiny", "dustDiv72"}, 72, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void netherQuartzCrushedPurified(GameTestHelper h) {
        check(h, "NetherQuartz", 8346, 3, "crushedPurified", new String[]{"dust", "dustSmall"}, 80, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void netherQuartzCrushedCentrifuged(GameTestHelper h) {
        check(h, "NetherQuartz", 8346, 3, "crushedCentrifuged", new String[]{"dust", "dustSmall", "dustTiny", "dustDiv72"}, 88, "dustTiny", 11, 32);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void netherQuartzCrushedTiny(GameTestHelper h) {
        check(h, "NetherQuartz", 8346, 3, "crushedTiny", new String[]{"dustTiny", "dustDiv72"}, 8, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void netherQuartzCrushedPurifiedTiny(GameTestHelper h) {
        check(h, "NetherQuartz", 8346, 3, "crushedPurifiedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72"}, 9, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void netherQuartzCrushedCentrifugedTiny(GameTestHelper h) {
        check(h, "NetherQuartz", 8346, 3, "crushedCentrifugedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72", "dustDiv72"}, 10, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void chalcopyriteCrushed(GameTestHelper h) {
        check(h, "Chalcopyrite", 9111, 0, "crushed", new String[]{"dust", "dustTiny", "dustDiv72"}, 18, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void chalcopyriteCrushedPurified(GameTestHelper h) {
        check(h, "Chalcopyrite", 9111, 0, "crushedPurified", new String[]{"dust", "dustSmall"}, 20, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void chalcopyriteCrushedCentrifuged(GameTestHelper h) {
        check(h, "Chalcopyrite", 9111, 0, "crushedCentrifuged", new String[]{"dust", "dustSmall", "dustTiny", "dustDiv72"}, 22, "dustTiny", 11, 32);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void chalcopyriteCrushedTiny(GameTestHelper h) {
        check(h, "Chalcopyrite", 9111, 0, "crushedTiny", new String[]{"dustTiny", "dustDiv72"}, 2, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void chalcopyriteCrushedPurifiedTiny(GameTestHelper h) {
        check(h, "Chalcopyrite", 9111, 0, "crushedPurifiedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void chalcopyriteCrushedCentrifugedTiny(GameTestHelper h) {
        check(h, "Chalcopyrite", 9111, 0, "crushedCentrifugedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void pentlanditeCrushed(GameTestHelper h) {
        check(h, "Pentlandite", 9145, 0, "crushed", new String[]{"dust", "dustTiny", "dustDiv72"}, 18, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void pentlanditeCrushedPurified(GameTestHelper h) {
        check(h, "Pentlandite", 9145, 0, "crushedPurified", new String[]{"dust", "dustSmall"}, 20, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void pentlanditeCrushedCentrifuged(GameTestHelper h) {
        check(h, "Pentlandite", 9145, 0, "crushedCentrifuged", new String[]{"dust", "dustSmall", "dustTiny", "dustDiv72"}, 22, "dustTiny", 11, 32);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void pentlanditeCrushedTiny(GameTestHelper h) {
        check(h, "Pentlandite", 9145, 0, "crushedTiny", new String[]{"dustTiny", "dustDiv72"}, 2, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void pentlanditeCrushedPurifiedTiny(GameTestHelper h) {
        check(h, "Pentlandite", 9145, 0, "crushedPurifiedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void pentlanditeCrushedCentrifugedTiny(GameTestHelper h) {
        check(h, "Pentlandite", 9145, 0, "crushedCentrifugedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void sphaleriteCrushed(GameTestHelper h) {
        check(h, "Sphalerite", 9130, 0, "crushed", new String[]{"dust", "dustTiny", "dustDiv72"}, 18, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void sphaleriteCrushedPurified(GameTestHelper h) {
        check(h, "Sphalerite", 9130, 0, "crushedPurified", new String[]{"dust", "dustSmall"}, 20, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void sphaleriteCrushedCentrifuged(GameTestHelper h) {
        check(h, "Sphalerite", 9130, 0, "crushedCentrifuged", new String[]{"dust", "dustSmall", "dustTiny", "dustDiv72"}, 22, "dustTiny", 11, 32);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void sphaleriteCrushedTiny(GameTestHelper h) {
        check(h, "Sphalerite", 9130, 0, "crushedTiny", new String[]{"dustTiny", "dustDiv72"}, 2, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void sphaleriteCrushedPurifiedTiny(GameTestHelper h) {
        check(h, "Sphalerite", 9130, 0, "crushedPurifiedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void sphaleriteCrushedCentrifugedTiny(GameTestHelper h) {
        check(h, "Sphalerite", 9130, 0, "crushedCentrifugedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void tetrahedriteCrushed(GameTestHelper h) {
        check(h, "Tetrahedrite", 9132, 0, "crushed", new String[]{"dust", "dustTiny", "dustDiv72"}, 18, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void tetrahedriteCrushedPurified(GameTestHelper h) {
        check(h, "Tetrahedrite", 9132, 0, "crushedPurified", new String[]{"dust", "dustSmall"}, 20, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void tetrahedriteCrushedCentrifuged(GameTestHelper h) {
        check(h, "Tetrahedrite", 9132, 0, "crushedCentrifuged", new String[]{"dust", "dustSmall", "dustTiny", "dustDiv72"}, 22, "dustTiny", 11, 32);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void tetrahedriteCrushedTiny(GameTestHelper h) {
        check(h, "Tetrahedrite", 9132, 0, "crushedTiny", new String[]{"dustTiny", "dustDiv72"}, 2, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void tetrahedriteCrushedPurifiedTiny(GameTestHelper h) {
        check(h, "Tetrahedrite", 9132, 0, "crushedPurifiedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void tetrahedriteCrushedCentrifugedTiny(GameTestHelper h) {
        check(h, "Tetrahedrite", 9132, 0, "crushedCentrifugedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void pyriteCrushed(GameTestHelper h) {
        check(h, "Pyrite", 9125, 0, "crushed", new String[]{"dust", "dustTiny", "dustDiv72"}, 18, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void pyriteCrushedPurified(GameTestHelper h) {
        check(h, "Pyrite", 9125, 0, "crushedPurified", new String[]{"dust", "dustSmall"}, 20, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void pyriteCrushedCentrifuged(GameTestHelper h) {
        check(h, "Pyrite", 9125, 0, "crushedCentrifuged", new String[]{"dust", "dustSmall", "dustTiny", "dustDiv72"}, 22, "dustTiny", 11, 32);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void pyriteCrushedTiny(GameTestHelper h) {
        check(h, "Pyrite", 9125, 0, "crushedTiny", new String[]{"dustTiny", "dustDiv72"}, 2, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void pyriteCrushedPurifiedTiny(GameTestHelper h) {
        check(h, "Pyrite", 9125, 0, "crushedPurifiedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void pyriteCrushedCentrifugedTiny(GameTestHelper h) {
        check(h, "Pyrite", 9125, 0, "crushedCentrifugedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void nickelCrushed(GameTestHelper h) {
        check(h, "Nickel", 280, 2, "crushed", new String[]{"dust", "dustTiny", "dustDiv72"}, 54, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void nickelCrushedPurified(GameTestHelper h) {
        check(h, "Nickel", 280, 2, "crushedPurified", new String[]{"dust", "dustSmall"}, 60, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void nickelCrushedCentrifuged(GameTestHelper h) {
        check(h, "Nickel", 280, 2, "crushedCentrifuged", new String[]{"dust", "dustSmall", "dustTiny", "dustDiv72"}, 66, "dustTiny", 11, 32);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void nickelCrushedTiny(GameTestHelper h) {
        check(h, "Nickel", 280, 2, "crushedTiny", new String[]{"dustTiny", "dustDiv72"}, 6, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void nickelCrushedPurifiedTiny(GameTestHelper h) {
        check(h, "Nickel", 280, 2, "crushedPurifiedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72"}, 7, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void nickelCrushedCentrifugedTiny(GameTestHelper h) {
        check(h, "Nickel", 280, 2, "crushedCentrifugedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72", "dustDiv72"}, 8, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void cooperiteCrushed(GameTestHelper h) {
        check(h, "Cooperite", 9116, 0, "crushed", new String[]{"dust", "dustTiny", "dustDiv72"}, 18, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void cooperiteCrushedPurified(GameTestHelper h) {
        check(h, "Cooperite", 9116, 0, "crushedPurified", new String[]{"dust", "dustSmall"}, 20, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void cooperiteCrushedCentrifuged(GameTestHelper h) {
        check(h, "Cooperite", 9116, 0, "crushedCentrifuged", new String[]{"dust", "dustSmall", "dustTiny", "dustDiv72"}, 22, "dustTiny", 11, 32);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void cooperiteCrushedTiny(GameTestHelper h) {
        check(h, "Cooperite", 9116, 0, "crushedTiny", new String[]{"dustTiny", "dustDiv72"}, 2, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void cooperiteCrushedPurifiedTiny(GameTestHelper h) {
        check(h, "Cooperite", 9116, 0, "crushedPurifiedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void cooperiteCrushedCentrifugedTiny(GameTestHelper h) {
        check(h, "Cooperite", 9116, 0, "crushedCentrifugedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void yellowLimoniteCrushed(GameTestHelper h) {
        check(h, "YellowLimonite", 9137, 0, "crushed", new String[]{"dust", "dustTiny", "dustDiv72"}, 18, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void yellowLimoniteCrushedPurified(GameTestHelper h) {
        check(h, "YellowLimonite", 9137, 0, "crushedPurified", new String[]{"dust", "dustSmall"}, 20, "dust", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void yellowLimoniteCrushedCentrifuged(GameTestHelper h) {
        check(h, "YellowLimonite", 9137, 0, "crushedCentrifuged", new String[]{"dust", "dustSmall", "dustTiny", "dustDiv72"}, 22, "dustTiny", 11, 32);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void yellowLimoniteCrushedTiny(GameTestHelper h) {
        check(h, "YellowLimonite", 9137, 0, "crushedTiny", new String[]{"dustTiny", "dustDiv72"}, 2, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void yellowLimoniteCrushedPurifiedTiny(GameTestHelper h) {
        check(h, "YellowLimonite", 9137, 0, "crushedPurifiedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }

    @GameTest(template = "test_empty", batch = "ore_shredder_source", timeoutTicks = 100)
    public static void yellowLimoniteCrushedCentrifugedTiny(GameTestHelper h) {
        check(h, "YellowLimonite", 9137, 0, "crushedCentrifugedTiny", new String[]{"dustTiny", "dustDiv72", "dustDiv72", "dustDiv72"}, 3, "dustTiny", 1, 64);
    }
}
