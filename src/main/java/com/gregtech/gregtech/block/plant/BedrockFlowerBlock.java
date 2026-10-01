package com.gregtech.gregtech.block.plant;

import com.gregtech.gregtech.block.IconSetPlantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraftforge.common.PlantType;

import java.util.List;

/** GT6 BlockFlowersB uses cactus-compatible desert soil; A keeps ordinary flower soil. */
public final class BedrockFlowerBlock extends IconSetPlantBlock {
    private final boolean desert;

    public BedrockFlowerBlock(Properties properties, String iconName, boolean desert) {
        super(properties, iconName);
        this.desert = desert;
    }

    public boolean desert() { return desert; }

    @Override
    public PlantType getPlantType(BlockGetter level, BlockPos pos) {
        // Forge's pre-placement sustain check asks the plant type before this block is
        // present in the world; mayPlaceOn alone cannot enforce GT6's desert soil rule.
        return desert ? PlantType.DESERT : PlantType.PLAINS;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return desert ? state.is(BlockTags.SAND) : super.mayPlaceOn(state, level, pos);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(this));
    }
}
