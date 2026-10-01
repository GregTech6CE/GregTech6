package com.gregtech.gregtech.content.tool;

import net.minecraft.world.phys.AABB;

/** GT6 MultiTileEntityBottleCrate render passes 9–35; positions are independent of facing. */
public final class BottleCrateGeometry {
    private BottleCrateGeometry() {}
    public static AABB bounds(int slot,int part) {
        if(slot<0 || slot>=9 || part<0 || part>2) throw new IllegalArgumentException();
        double x=1+5*(slot%3),z=1+5*(slot/3);
        return switch(part) {
            case 0 -> new AABB(x/16+.01,1/16.,z/16+.01,(x+4)/16-.01,12/16.,(z+4)/16-.01);
            case 1 -> new AABB(x/16+.005,1/16.,z/16+.005,(x+4)/16-.005,13/16.,(z+4)/16-.005);
            default -> new AABB((x+1)/16,13/16.,(z+1)/16,(x+3)/16,1,(z+3)/16);
        };
    }
}
