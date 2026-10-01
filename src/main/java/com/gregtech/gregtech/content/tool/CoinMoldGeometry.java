package com.gregtech.gregtech.content.tool;

import net.minecraft.world.phys.AABB;

/** MultiTileEntityMoldCoinage render pass 2, independent of its 12/16 collision/selection box. */
public final class CoinMoldGeometry {
    private CoinMoldGeometry() {}
    public static final AABB CONTENT = new AABB(5/16D,11/16D,5/16D,11/16D,13/16D,11/16D);
}
