package com.gregtech.gregtech.block.misc;

import net.minecraft.world.level.block.EntityBlock;

/** Only wet GT6 construction foam has a drying block entity. Hardened foam is an ordinary block. */
public final class FreshCFoamBlock extends CFoamBlock implements EntityBlock {
    public FreshCFoamBlock(Properties properties) { super(true, properties); }
}
