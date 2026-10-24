package gregtech.gametest;

import gregtech.internal.addon.AddonMaterials;
import gregtech.api.addon.platform.AddonFluids;
import gregtech.internal.fluid.FluidDisplayBinding;
import gregtech.internal.fluid.FluidTankGT;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.data.RegisteredFluids;
import gregtech.registry.GTFluidItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
//?}

/** Registry ownership must not follow the late material metadata on a reused built-in fluid. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class AddonFluidIdentityTests {
    private AddonFluidIdentityTests() {}

    @GameTest(template = "test_empty")
    public static void addonBindingKeepsBuiltInFluidRegistryIdentity(GameTestHelper h) {
        var material = GTMaterialRegistry.get("Beer");
        h.assertTrue(material.isValid() && "addon_sample".equals(AddonMaterials.owner(material)),
                "the sample actually contributes the otherwise absent Beer material");
        h.assertTrue("Beer".equals(RegisteredFluids.boundMaterial("beer")),
                "the addon material really binds to the existing fluid");
        var beer = registryFamily(h, "Beer", "gt6ce", "beer");
        for (String field : new String[]{"GenLiquid_Beer", "GenGas_Beer"}) {
            var alias = AddonFluids.still(field, 250);
            h.assertTrue(alias != null && !alias.isEmpty() && alias.getFluid() == beer.getFluid(),
                    "generated field alias reuses the built-in native fluid: " + field);
        }
        h.assertTrue(!BuiltInRegistries.FLUID.containsKey(id("addon_sample", "beer"))
                        && !BuiltInRegistries.FLUID.containsKey(id("addon_sample", "beer_flowing"))
                        && !BuiltInRegistries.ITEM.containsKey(id("addon_sample", "fluid_item_beer")),
                "binding does not relocate or duplicate built-in registrations");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void legacyBuiltInFluidSaveSurvivesAddonMaterialBinding(GameTestHelper h) {
        // A preexisting tank save must resolve by its old native registry ID, not by an addon alias.
        var fluid = new CompoundTag();
        fluid.putString("FluidName", "gt6ce:beer");
        fluid.putInt("Amount", 250);
        var legacy = new CompoundTag();
        legacy.put("Fluid", fluid);
        legacy.putLong("Amount", 250);
        legacy.putLong("Capacity", 1000);
        var tank = new FluidTankGT(1000);
        read(h, tank, legacy);
        assertBeerTank(h, tank, "preexisting FluidName save");

        var current = new CompoundTag();
//? if >=1.21.1 {
        /*tank.writeToNBT(current, h.getLevel().registryAccess());
*///?} else {
        tank.writeToNBT(current);
//?}
        h.assertTrue(current.getCompound("Fluid").toString().contains("gt6ce:beer"),
                "current loader codec writes the original registry identity");
        var reloaded = new FluidTankGT(1);
        read(h, reloaded, current);
        assertBeerTank(h, reloaded, "current codec roundtrip");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void generatedAddonFluidsRetainTheirOwnNamespace(GameTestHelper h) {
        h.assertTrue(!RegisteredFluids.isBuiltInDefinition(RegisteredFluids.get("GenMolten_SampleAlloy")),
                "new generated molten fluid is not a built-in definition");
        registryFamily(h, "GenMolten_SampleAlloy", "addon_sample", "molten_samplealloy");
        h.assertTrue(!BuiltInRegistries.FLUID.containsKey(id("gt6ce", "molten_samplealloy"))
                        && !BuiltInRegistries.ITEM.containsKey(id("gt6ce", "fluid_item_molten_samplealloy")),
                "novel addon fluid and carrier keep the intended addon namespace");
        h.succeed();
    }

    private static FluidStack registryFamily(GameTestHelper h, String field, String namespace, String path) {
        var source = AddonFluids.still(field, 144);
        var flowing = AddonFluids.fluid(field, AddonFluids.Kind.FLOWING, 144);
        h.assertTrue(source != null && !source.isEmpty() && flowing != null && !flowing.isEmpty(),
                "native source and flowing fluids exist: " + field);
        h.assertTrue(id(namespace, path).equals(BuiltInRegistries.FLUID.getKey(source.getFluid()))
                        && id(namespace, path + "_flowing").equals(BuiltInRegistries.FLUID.getKey(flowing.getFluid())),
                "source and flowing registry identities: " + field);
//? if >=1.21.1 {
        /*var typeId = NeoForgeRegistries.FLUID_TYPES.getKey(source.getFluid().getFluidType());
*///?} else {
        var typeId = ForgeRegistries.FLUID_TYPES.get().getKey(source.getFluid().getFluidType());
//?}
        h.assertTrue(id(namespace, path).equals(typeId), "fluid type registry identity: " + field);
        var carrier = GTFluidItems.get(field);
        h.assertTrue(carrier != null && carrier != Items.AIR
                        && id(namespace, "fluid_item_" + path).equals(BuiltInRegistries.ITEM.getKey(carrier)),
                "field lookup resolves the native display item: " + field);
//? if >=1.21.1 {
        /*var plain = FluidDisplayBinding.resolve(h.getLevel().registryAccess(), new ItemStack(carrier));
        var display = FluidDisplayBinding.display(h.getLevel().registryAccess(), source);
        var payload = FluidDisplayBinding.resolve(h.getLevel().registryAccess(), display);
*///?} else {
        var plain = FluidDisplayBinding.resolve(new ItemStack(carrier));
        var display = FluidDisplayBinding.display(source);
        var payload = FluidDisplayBinding.resolve(display);
//?}
        h.assertTrue(!plain.isEmpty() && plain.getFluid() == source.getFluid() && plain.getAmount() == 1000,
                "plain carrier resolves its own namespace without payload: " + field);
        h.assertTrue(display.getItem() == carrier && !payload.isEmpty()
                        && payload.getFluid() == source.getFluid() && payload.getAmount() == 144,
                "payload carrier preserves native fluid identity and amount: " + field);
        return source;
    }

    private static void assertBeerTank(GameTestHelper h, FluidTankGT tank, String stage) {
        var stored = tank.getFluid();
        var beer = AddonFluids.still("Beer", 250);
        h.assertTrue(beer != null && !beer.isEmpty() && !stored.isEmpty()
                        && stored.getFluid() == beer.getFluid() && stored.getAmount() == 250
                        && tank.getAmount() == 250 && tank.baseCapacity() == 1000
                        && id("gt6ce", "beer").equals(BuiltInRegistries.FLUID.getKey(stored.getFluid())),
                stage + " loads all Beer into the real native tank");
    }

    private static void read(GameTestHelper h, FluidTankGT tank, CompoundTag tag) {
//? if >=1.21.1 {
        /*tank.readFromNBT(tag, h.getLevel().registryAccess());
*///?} else {
        tank.readFromNBT(tag);
//?}
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
