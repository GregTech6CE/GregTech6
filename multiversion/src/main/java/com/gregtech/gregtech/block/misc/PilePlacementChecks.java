package com.gregtech.gregtech.block.misc;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Current native expiry replacement predicate; original WD.irrelevant accepts additional waterlike blocks. */
final class PilePlacementChecks {
    private PilePlacementChecks() {}

    static boolean canReplaceForExpiry(BlockState state) {
        return state.isAir() || state.is(Blocks.VINE) || state.is(Blocks.SNOW)
                || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
//? if >=1.21.1 {
                /*|| state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
*///?} else {
                || state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS)
//?}
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.WATER);
    }
}
