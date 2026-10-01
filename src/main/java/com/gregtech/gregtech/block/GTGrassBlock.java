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
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;

import java.util.List;

/** The six coloured GT6 BlockGrass variants. They keep their colour and do not spread. */
public final class GTGrassBlock extends IconSetBlock {
    public GTGrassBlock(Properties properties, String iconName) {
        super(properties, iconName);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        if (tool != null && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0)
            return List.of(new ItemStack(this));
        return List.of(new ItemStack(Blocks.DIRT));
    }

    @Override
    public boolean canSustainPlant(BlockState state, BlockGetter level, BlockPos pos,
                                   Direction side, IPlantable plant) {
        if (side != Direction.UP) return false;
        PlantType type = plant.getPlantType(level, pos.above());
        if (type == PlantType.PLAINS) return true;
        if (type != PlantType.BEACH) return false;
        for (Direction horizontal : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(horizontal)).is(FluidTags.WATER)) return true;
        }
        return false;
    }
}
