package com.gregtech.gregtech.worldgen.dungeon;


import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.block.tool.FluidAttachmentBlock;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;

/** GT6 workshop 32716 drum, two 32730 taps and 32725 ceiling funnel. */
public final class WorkshopFluidStation {
    private WorkshopFluidStation() {}
    public static void place(GTDungeonData data,int x,int y,int z) {
        var drum=java.util.Objects.requireNonNull(BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","drum_stainless_steel")));
        data.tank(x,y,z,drum,GTFluids.stack("Water",64000));
        attachment(data,x-1,y,z,"tap_stainless_steel",Direction.EAST);
        attachment(data,x,y,z-1,"tap_stainless_steel",Direction.SOUTH);
        attachment(data,x,y+1,z,"fluid_funnel_stainless_steel",Direction.DOWN);
    }
    private static void attachment(GTDungeonData data,int x,int y,int z,String id,Direction direction) {
        var block=BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",id));
        if(!(block instanceof FluidAttachmentBlock)) throw new IllegalStateException("Missing workshop attachment "+id);
        data.set(x,y,z,block.defaultBlockState().setValue(FluidAttachmentBlock.FACING,direction));
    }
}
