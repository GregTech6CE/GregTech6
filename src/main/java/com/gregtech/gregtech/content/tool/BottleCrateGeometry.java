package com.gregtech.gregtech.content.tool;

import net.minecraft.world.phys.AABB;

/** GT6 MultiTileEntityBottleCrate render passes 9–35; positions are independent of facing. */
public final class BottleCrateGeometry {
    private BottleCrateGeometry() {}
    public static AABB bounds(int slot,int part) {
        var b=com.gregtech.gregtech.content.storage.ContainerStorageRules.bottleBounds(slot,part);
        return new AABB(b.x0(),b.y0(),b.z0(),b.x1(),b.y1(),b.z1());
    }
}
