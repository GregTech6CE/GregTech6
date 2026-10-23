package com.gregtech.gregtech.block.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
//? if >=1.21.1 {
//?} else {
import net.minecraftforge.common.IPlantable;
//?}

import java.util.List;

import javax.annotation.Nullable;

/**
 * Universal plant pot with top-only plant support, independent of the plant's normal soil type.
 * {@link ShapedToolBlock} also uses the original two render bounds for this port's collision and
 * outline; the original plant pot instead inherits a full-cube collision and selection box.
 */
public final class PlantPotBlock extends ShapedToolBlock {

    public PlantPotBlock(Properties properties) {
        super("plant_pot", properties);
    }

    @Override
//? if >=1.21.1 {
    /*public net.neoforged.neoforge.common.util.TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos pos,
            Direction facing, BlockState plantable) {
        return facing == Direction.UP ? net.neoforged.neoforge.common.util.TriState.TRUE : net.neoforged.neoforge.common.util.TriState.FALSE;
*///?} else {
    public boolean canSustainPlant(BlockState state, BlockGetter level, BlockPos pos,
            Direction facing, IPlantable plantable) {
        return facing == Direction.UP;
//?}
    }

    /** GT6's pot has no inventory: breaking it returns the pot itself. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(this));
    }

    @Override
//? if >=1.21.1 {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
*///?} else {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
//?}
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.multitileentity.plantpot.tooltip.1")
                .withStyle(net.minecraft.ChatFormatting.AQUA));
    }
}
