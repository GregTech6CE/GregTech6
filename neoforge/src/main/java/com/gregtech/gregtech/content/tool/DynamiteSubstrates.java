package com.gregtech.gregtech.content.tool;

import com.gregtech.gregtech.block.RockOreBlock;
import com.gregtech.gregtech.block.stone.GTStoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

/** GT6 BlocksGT.drillableDynamite + natural stone/ore; shared by placement and support checks. */
public final class DynamiteSubstrates {
    public static final TagKey<Block> DRILLABLE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("gregtech", "drillable_dynamite"));
    private DynamiteSubstrates() {}

    public static boolean canDrill(BlockGetter level, BlockPos pos) {
        var state = level.getBlockState(pos);
        var block = state.getBlock();
        boolean explicit = state.is(DRILLABLE) || block instanceof RockOreBlock;
        if (state.getDestroySpeed(level, pos) < 0 && !explicit) return false;
        // GT6 rejects processed stone variants even when an addon puts them in the whitelist.
        if (block instanceof GTStoneBlock stone) return stone.variant().meta() < 3;
        return explicit || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER) || state.is(net.neoforged.neoforge.common.Tags.Blocks.ORES);
    }
}
