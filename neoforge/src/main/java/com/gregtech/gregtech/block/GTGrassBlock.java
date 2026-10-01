package com.gregtech.gregtech.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;



import java.util.List;

/** The six coloured GT6 BlockGrass variants. They keep their colour and do not spread. */
public final class GTGrassBlock extends IconSetBlock {
    public GTGrassBlock(Properties properties, String iconName) {
        super(properties, iconName);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        if (tool != null && com.gregtech.gregtech.worldgen.OreHarvest.from(builder).silkTouch())
            return List.of(new ItemStack(this));
        return List.of(new ItemStack(Blocks.DIRT));
    }

    @Override
    public net.neoforged.neoforge.common.util.TriState canSustainPlant(BlockState state,BlockGetter level,BlockPos pos,Direction side,BlockState plant){
        if(side!=Direction.UP)return net.neoforged.neoforge.common.util.TriState.FALSE;
        if(plant.is(Blocks.SUGAR_CANE)){for(Direction horizontal:Direction.Plane.HORIZONTAL)if(level.getFluidState(pos.relative(horizontal)).is(FluidTags.WATER))return net.neoforged.neoforge.common.util.TriState.TRUE;return net.neoforged.neoforge.common.util.TriState.FALSE;}
        if(plant.getBlock() instanceof net.minecraft.world.level.block.BushBlock)return net.neoforged.neoforge.common.util.TriState.TRUE;
        return net.neoforged.neoforge.common.util.TriState.DEFAULT;
    }
}
