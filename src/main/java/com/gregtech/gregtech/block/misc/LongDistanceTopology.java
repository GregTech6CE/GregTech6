package com.gregtech.gregtech.block.misc;
import net.minecraft.world.level.Level;
/** Routing changes only; active-state animation must not invalidate a cached connection. */
final class LongDistanceTopology {
    private static final java.util.Map<Level,Long> VERSIONS=new java.util.WeakHashMap<>();
    private LongDistanceTopology(){}
    static long version(Level level){return VERSIONS.getOrDefault(level,0L);}
    static void changed(Level level){if(!level.isClientSide)VERSIONS.put(level,version(level)+1);}
}
