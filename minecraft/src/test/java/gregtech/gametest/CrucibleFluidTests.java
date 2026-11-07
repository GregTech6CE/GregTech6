package gregtech.gametest;

import gregtech.content.food.GTFoodItems;
import gregtech.content.registry.GTFluids;
import gregtech.engine.fluid.MoltenFluidHandler;
import gregtech.engine.fluid.PotionBottleHandler;
import gregtech.engine.material.GTMaterialRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * The crucible and the mercury bottle must address the same fluid. GT6 creates mercury as one
 * ordinary liquid, so the generated molten ID is a compatibility entry rather than a second phase.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class CrucibleFluidTests {
    private static final String BATCH = "crucible_fluid";
    private static final int BOTTLE = 250;

    private CrucibleFluidTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void ordinaryMercuryIsTheCruciblePhase(GameTestHelper helper) {
        var mercury = GTMaterialRegistry.get("Mercury").resolve();
        var liquid = GTFluids.stack("GenLiquid_Mercury", BOTTLE);
        helper.assertTrue(!liquid.isEmpty(), "generated mercury liquid exists");
        helper.assertTrue(MoltenFluidHandler.material(liquid) == mercury, "the ordinary liquid is crucible mercury");
        var molten = GTFluids.stack("GenMolten_Mercury", BOTTLE);
        helper.assertTrue(MoltenFluidHandler.material(molten) == mercury, "the compatibility molten ID stays readable");
        var emitted = MoltenFluidHandler.moltenFluid(mercury, BOTTLE);
        helper.assertTrue(emitted.getFluid() == liquid.getFluid(), "the crucible emits the ordinary liquid");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void glassBottleFillsFromCrucibleMercury(GameTestHelper helper) {
        Item bottle = GTFoodItems.itemOrNull("mercury_bottle");
        helper.assertTrue(bottle != null, "mercury_bottle is registered");
        var handler = new PotionBottleHandler(new ItemStack(Items.GLASS_BOTTLE));
        int filled = handler.fill(GTFluids.stack("GenLiquid_Mercury", BOTTLE), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled == BOTTLE, "the glass bottle takes one bottle of mercury");
        helper.assertTrue(handler.getContainer().is(bottle), "the filled container is mercury_bottle");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void mercuryBottlePoursBackIntoTheCrucible(GameTestHelper helper) {
        Item bottle = GTFoodItems.itemOrNull("mercury_bottle");
        helper.assertTrue(bottle != null, "mercury_bottle is registered");
        var handler = new PotionBottleHandler(new ItemStack(bottle));
        FluidStack contents = handler.drain(BOTTLE, IFluidHandler.FluidAction.SIMULATE);
        helper.assertTrue(!contents.isEmpty(), "the bottle holds mercury");
        helper.assertTrue(MoltenFluidHandler.material(contents) != null, "the crucible accepts the bottle contents");
        helper.succeed();
    }
}
