package gregtech.gametest;

import gregtech.content.registry.GTFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Draft: actual registered FluidType values, no synthetic catalog-only proof. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class FluidMaterialMetadataTests {
    @GameTest(template = "test_empty")
    public static void cryoliteKeepsIdAndOriginalMoltenProperties(GameTestHelper h) {
        check(h, "GenLiquid_Cryolite", "cryolite", 1285, 10, 1000, false);
        h.succeed();
    }
    @GameTest(template = "test_empty")
    public static void aluminiumFluorideKeepsIdAndOriginalMoltenProperties(GameTestHelper h) {
        check(h, "GenLiquid_AluminiumFluoride", "aluminiumfluoride", 1560, 10, 1000, false);
        h.succeed();
    }
    @GameTest(template = "test_empty")
    public static void legacyMoltenSaltsKeepIdsAndOriginalProperties(GameTestHelper h) {
        check(h, "GenLiquid_LithiumChloride", "lithiumchloride", 880, 10, 1000, false);
        check(h, "GenLiquid_LithiumChlorate", "lithiumchlorate", 400, 10, 1000, false);
        h.succeed();
    }
    @GameTest(template = "test_empty")
    public static void generatedMoltenUsesOriginalHeatAndLight(GameTestHelper h) {
        check(h, "GenMolten_Iron", "molten_iron", 1811, 10, 1000, false);
        check(h, "GenMolten_Mercury", "molten_mercury", 300, 10, 1000, false);
        h.succeed();
    }
    @GameTest(template = "test_empty")
    public static void staticGeneratedMoltenUsesMaterialHeatAndLight(GameTestHelper h) {
        check(h, "GenMolten_Wax", "molten_wax", 350, 10, 1000, false);
        h.succeed();
    }
    @GameTest(template = "test_empty")
    public static void explicitFluidPropertiesRemainAuthoritative(GameTestHelper h) {
        check(h, "Steam", "steam", 373, 0, 200, true);
        check(h, "Liquid_Oxygen", "liquidoxygen", 85, 0, 1000, false);
        check(h, "Pyrotheum", "pyrotheum", 4000, 15, 1200, false);
        check(h, "Cryotheum", "cryotheum", 50, 0, 3000, false);
        check(h, "Ender", "molten_enderpearl", 2723, 5, 1000, false);
        check(h, "Lava_Pahoehoe", "ic2pahoehoelava", 1200, 10, 250000, false);
        var steam = GTFluids.stack("Steam", 1000);
        h.assertTrue(steam.getFluid().getFluidType().getDensity(steam) == -1000, "steam explicit density remains -1000");
        var pyro = GTFluids.stack("Pyrotheum", 1000);
        h.assertTrue(pyro.getFluid().getFluidType().getDensity(pyro) == 2000, "pyrotheum explicit density remains 2000");
        h.succeed();
    }
    private static void check(GameTestHelper h, String field, String path, int temperature,
                              int light, int viscosity, boolean gas) {
        var stack = GTFluids.stack(field, 1000);
        h.assertTrue(!stack.isEmpty(), "registered fluid exists: " + field);
        h.assertTrue(BuiltInRegistries.FLUID.getKey(stack.getFluid()).equals(
                ResourceLocation.fromNamespaceAndPath("gt6ce", path)), "stable registry ID: " + field);
        var type = stack.getFluid().getFluidType();
        h.assertTrue(type.getTemperature(stack) == temperature, field + " temperature expected " + temperature + " actual " + type.getTemperature(stack));
        h.assertTrue(type.getLightLevel(stack) == light, field + " light expected " + light + " actual " + type.getLightLevel(stack));
        var entry = GTFluids.entryForFluid(stack.getFluid());
        h.assertTrue(entry != null && entry.temperature() == temperature && entry.luminosity() == light,
                field + " catalog metadata matches native expectations for tank/pipe/sensor consumers");
        h.assertTrue(type.getViscosity(stack) == viscosity, field + " viscosity remains " + viscosity);
        h.assertTrue(GTFluids.isGas(stack) == gas, field + " phase state unchanged");
    }
}
