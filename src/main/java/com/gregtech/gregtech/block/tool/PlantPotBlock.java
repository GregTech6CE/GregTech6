package com.gregtech.gregtech.block.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraftforge.common.IPlantable;

import java.util.List;

import javax.annotation.Nullable;

/**
 * GT6's universal plant pot ({@code MultiTileEntityPlantPot}). The mesh and collision shape come
 * from the two original render passes in {@link ShapedToolBlock}; its top is a full 16x16 surface.
 * GT6 allows every plant only on that top face, independent of the plant's normal soil type.
 */
public final class PlantPotBlock extends ShapedToolBlock {
    public PlantPotBlock(Properties properties) {
        super("plant_pot", properties);
    }

    @Override
    public boolean canSustainPlant(BlockState state, BlockGetter level, BlockPos pos,
                                   Direction facing, IPlantable plantable) {
        return facing == Direction.UP;
    }

    /** GT6's pot has no inventory: breaking it returns the pot itself. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(this));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.multitileentity.plantpot.tooltip.1")
                .withStyle(net.minecraft.ChatFormatting.AQUA));
    }
}
