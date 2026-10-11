package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.block.machine.TankControllerBlock;
import com.gregtech.gregtech.blockentity.machine.MultiblockTankControllerBlockEntity;
import com.gregtech.gregtech.client.CommonBlockTooltips;
import com.gregtech.gregtech.content.multiblock.OriginalTankTooltipData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import java.util.ArrayList;

/** One finite native saved-state boundary prepared for pooled world acceptance. */
@net.minecraftforge.gametest.GameTestHolder("gregtech_multiblock_tanks")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class MultiblockTankTooltipStateTests {
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void actualValveLoadingAndItemTooltipPreserveLongContents(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,0,2)).atY(240);
        for(String path:new String[]{"wood_tank_main_valve","small_stainless_steel_tank_main_valve","large_dense_adamantium_tank_main_valve"}) {
            var block=(TankControllerBlock)BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech",path));
            h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
            var entity=(MultiblockTankControllerBlockEntity)h.getLevel().getBlockEntity(pos);
            long nominal=block.valveSpec().capacity(), amount=3500000000L;
            var tank=new FluidTankGT(1);tank.setFluid(new net.minecraftforge.fluids.FluidStack(Fluids.WATER,1),amount);
            var data=new CompoundTag();var nested=new CompoundTag();tank.writeToNBT(nested);
            data.put("gt.tank",nested);data.putInt("gt.size",block.size()==3?5:3);
            entity.load(data);
            entity.setSize(block.size()); // Existing contents must survive a capacity/proof refresh too.
            var saved=entity.saveWithoutMetadata();
            h.assertTrue(saved.getCompound("gt.tank").getLong("Amount")==amount,"actual native load/save retains long contents without nominal clamping");
            h.assertTrue(saved.getCompound("gt.tank").getLong("Capacity")==nominal,"actual valve owns nominal capacity");
            h.assertTrue(entity.fluidHandler().getFluidInTank(0).getAmount()==Integer.MAX_VALUE
                    && entity.fluidHandler().getTankCapacity(0)==Integer.MAX_VALUE,"modern capability only binds its int representation");
            var item=new ItemStack(block);item.addTagElement("BlockEntityTag",data);var before=data.copy();
            var lines=new ArrayList<Component>();block.appendHoverText(item,h.getLevel(),lines,TooltipFlag.NORMAL);
            h.assertTrue(lines.stream().anyMatch(c->c.getString().startsWith("3_500_000_000 L of ")
                    && c.getString().endsWith(" (Liquid); Max: 3_500_000_000 L)")),"native item reports retained amount/source effective capacity");
            h.assertTrue(before.equals(item.getTagElement("BlockEntityTag")),"native hover does not modify item data");
            h.assertTrue(lines.stream().filter(c->CommonBlockTooltips.containsKey(java.util.List.of(c),"gt.lang.no.powerconducting.fluids")).count()==1,"source functional hazard row once");
            for(String key:OriginalTankTooltipData.structureKeys(block.size()))
                h.assertTrue(CommonBlockTooltips.containsKey(lines,key),"correct source structure row");
            h.getLevel().removeBlock(pos,false);
        }
        h.succeed();
    }
}
