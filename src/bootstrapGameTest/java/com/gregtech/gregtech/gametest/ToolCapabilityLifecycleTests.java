package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.tool.*;
import com.gregtech.gregtech.blockentity.tool.*;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Existing tool inventories/tanks survive invalidation and revive without resurrecting stale handles. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class ToolCapabilityLifecycleTests {
    private static <T> T cycle(GameTestHelper h, BlockEntity be, Capability<T> cap) {
        var old=be.getCapability(cap);
        h.assertTrue(old.isPresent(),"initial capability "+be.getBlockState());
        boolean[] notified={false}; old.addListener(ignored->notified[0]=true);
        be.invalidateCaps();
        h.assertTrue(notified[0]&&!old.isPresent(),"old handle invalidated and listener notified");
        h.assertTrue(!be.getCapability(cap).isPresent(),"query must not revive invalid capabilities");
        if(be instanceof PortableContainerBlockEntity vessel) {
            vessel.setContents(vessel.contents());
            h.assertTrue(!be.getCapability(cap).isPresent(),"updating vessel contents must not revive invalid capabilities");
        }
        be.reviveCaps();
        var renewed=be.getCapability(cap);
        h.assertTrue(renewed.isPresent()&&renewed!=old&&!old.isPresent(),"fresh handle on revive: "+be.getBlockState());
        for(Direction side:Direction.values())h.assertTrue(be.getCapability(cap,side).isPresent(),"side restored "+side);
        return renewed.orElseThrow(()->new AssertionError("missing restored handler"));
    }
    @GameTest(template="coin_pile_space")
    public static void processingVesselsRetainFluidAndBothInterfaces(GameTestHelper h) {
        int count=0;
        for(var block:ForgeRegistries.BLOCKS)if(block instanceof ProcessingToolBlock) {
            var be=(ProcessingToolBlockEntity)((EntityBlock)block).newBlockEntity(BlockPos.ZERO,block.defaultBlockState());
            var fluid=be.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new AssertionError("missing initial fluid handler"));
            int amount=fluid.fill(new FluidStack(Fluids.WATER,123),IFluidHandler.FluidAction.EXECUTE);
            if(((ProcessingToolBlock)block).toolId().equals("juicer")) {
                h.assertTrue(amount==0,"juicer has no fluid input");
                var saved=be.saveWithoutMetadata();
                var output=saved.getList("gt.tanks",10).getCompound(0);
                output.putLong("Amount",123);
                output.put("Fluid",new FluidStack(Fluids.WATER,123).writeToNBT(new net.minecraft.nbt.CompoundTag()));
                be.load(saved); // Restore a pre-existing output tank, never add an input tank.
            } else h.assertTrue(amount==123,"water input established before lifecycle");
            if(((ProcessingToolBlock)block).toolId().equals("juicer"))
                h.assertTrue(!be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent(),"juicer only exposes fluid automation");
            else cycle(h,be,ForgeCapabilities.ITEM_HANDLER);
            var restored=cycle(h,be,ForgeCapabilities.FLUID_HANDLER);
            h.assertTrue(restored.drain(1000,IFluidHandler.FluidAction.EXECUTE).getAmount()==123,"tank contents preserved exactly");
            h.assertTrue(restored.drain(1000,IFluidHandler.FluidAction.EXECUTE).isEmpty(),"no duplicate water");
            count++;
        }
        h.assertTrue(count==7,"all seven processing vessels exercised");h.succeed();
    }
    @GameTest(template="coin_pile_space")
    public static void funnelAndBothApiariesRestoreAutomation(GameTestHelper h) {
        int count=0;
        for(var block:ForgeRegistries.BLOCKS)if(block instanceof DustFunnelBlock||block instanceof BumbliaryBlock) {
            var be=((EntityBlock)block).newBlockEntity(BlockPos.ZERO,block.defaultBlockState());
            if(be instanceof DustFunnelBlockEntity funnel) {
                var dust=GTItems.getStack(MaterialPrefix.dust,Materials.Iron,3);
                h.assertTrue(funnel.insert(dust)==3,"funnel has three iron dust");
                var restored=cycle(h,be,ForgeCapabilities.ITEM_HANDLER);
                h.assertTrue(restored.extractItem(0,64,false).getCount()==3,"funnel contents retained");
            } else {
                var apiary=(BumbliaryBlockEntity)be;
                apiary.inventory().setStackInSlot(apiary.layout().combs()[0],new ItemStack(Items.HONEYCOMB,3));
                var restored=cycle(h,be,ForgeCapabilities.ITEM_HANDLER);
                int total=0;for(int i=0;i<restored.getSlots();i++)total+=restored.extractItem(i,64,false).getCount();
                int expected=((BumbliaryBlock)block).advanced()?3:0;
                h.assertTrue(total==expected,"only advanced apiary exposes comb output");
                h.assertTrue(apiary.inventory().getStackInSlot(apiary.layout().combs()[0]).getCount()==3-expected,
                        "standard apiary combs remain in their manual output slots");
                h.assertTrue(restored.insertItem(0,new ItemStack(Items.HONEYCOMB),false).getCount()==1,
                        "lifecycle does not enable forbidden automated insertion");
            }
            count++;
        }
        h.assertTrue(count==3,"funnel and both apiaries exercised");h.succeed();
    }
    @GameTest(template="coin_pile_space")
    public static void portableVesselsDoNotReviveOnQuery(GameTestHelper h) {
        int count=0;
        for(var block:ForgeRegistries.BLOCKS)if(block instanceof PortableContainerBlock) {
            var be=(PortableContainerBlockEntity)((EntityBlock)block).newBlockEntity(BlockPos.ZERO,block.defaultBlockState());
            be.setContents(new ItemStack(block));
            var before=be.contents();
            cycle(h,be,ForgeCapabilities.FLUID_HANDLER);
            h.assertTrue(ItemStack.isSameItemSameTags(before,be.contents()),"vessel item/NBT preserved");
            be.setRemoved();
            h.assertTrue(!be.getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent(),"removed vessel stays inaccessible");
            count++;
        }
        h.assertTrue(count>5,"material variants included");h.succeed();
    }
}
