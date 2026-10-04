package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.block.FluidSpringBlock;
import com.gregtech.gregtech.worldgen.FluidSpringRules;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import java.util.*;

/** Seven source fluid identities. The old empty ID only decodes pre-flattening saves/stacks. */
public final class GTFluidSprings {
    private GTFluidSprings() {}
    public static DeferredHolder<Block,FluidSpringBlock> FLUID_SPRING;
    private static final Map<String,DeferredHolder<Block,FluidSpringBlock>> VARIANTS=new LinkedHashMap<>();
    public static void initialize() {
        if(FLUID_SPRING!=null) throw new IllegalStateException("Spring registration repeated");
        FLUID_SPRING=register("fluid_spring",null);
        for(var spring:FluidSpringRules.SPRINGS)register(FluidSpringRules.blockPath(spring),spring);
    }
    private static DeferredHolder<Block,FluidSpringBlock> register(String path,FluidSpringRules.Spring spring) {
        var block=GTBlocks.BLOCKS.register(path,()->new FluidSpringBlock(spring,BlockBehaviour.Properties.of()
                .mapColor(MapColor.WATER).strength(3,3).requiresCorrectToolForDrops().sound(SoundType.STONE)));
        VARIANTS.put(path,block);
        GTBlocks.BLOCK_ITEMS.register(path,()->new BlockItem(block.get(),new Item.Properties()));return block;
    }
    public static FluidSpringBlock byFluid(String fluid) {
        var spring=FluidSpringRules.byFluid(fluid);if(spring==null)return null;
        return VARIANTS.get(FluidSpringRules.blockPath(spring)).get();
    }
    public static Block[] allBlocks() { return VARIANTS.values().stream().map(e->(Block)e.get()).toArray(Block[]::new); }
}
