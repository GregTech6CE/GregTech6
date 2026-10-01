package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Production coke oven output must be the registered creosote used by GT6 recipes. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class CokeOvenFluidRepairTests {
    @GameTest(template = "test_empty")
    public static void coalProducesRegisteredCreosote(GameTestHelper helper) {
        var creosote = GTFluids.still("Oil_Creosote");
        helper.assertTrue(creosote != null && creosote.isPresent(), "creosote fluid registered under original field");
        var oven = new CokeOvenControllerBlockEntity(BlockPos.ZERO, Blocks.STONE.defaultBlockState());
        oven.items.setStackInSlot(0, new ItemStack(Items.COAL));
        oven.heat = CokeOvenControllerBlockEntity.HU_PER_OP;
        oven.tickCooking(helper.getLevel(), BlockPos.ZERO);
        helper.assertTrue(oven.outputTank.getFluid().getFluid() == creosote.get()
                        && oven.outputTank.getAmount() == 500
                        && oven.items.getStackInSlot(1).is(com.gregtech.gregtech.registry.GTItems.getStack(
                                com.gregtech.gregtech.data.MaterialPrefix.gem,
                                com.gregtech.gregtech.content.material.Materials.CoalCoke, 1).getItem()),
                "coal cooking yields GT6 coal coke and 500 mB of real creosote");
        oven.outputTank.fill(new net.minecraftforge.fluids.FluidStack(creosote.get(), 15_500),
                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        oven.items.setStackInSlot(0, new ItemStack(Items.COAL));
        oven.heat = CokeOvenControllerBlockEntity.HU_PER_OP;
        oven.tickCooking(helper.getLevel(), BlockPos.ZERO);
        helper.assertTrue(oven.outputTank.getAmount() == 16_000
                        && oven.items.getStackInSlot(0).getCount() == 1
                        && oven.heat == CokeOvenControllerBlockEntity.HU_PER_OP,
                "full creosote tank stops processing without destroying input or heat");
        helper.succeed();
    }
}
