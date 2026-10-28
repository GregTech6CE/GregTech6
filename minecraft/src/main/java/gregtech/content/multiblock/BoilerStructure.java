package gregtech.content.multiblock;

import gregtech.engine.multiblock.MultiblockLayout;

public final class BoilerStructure {
    private BoilerStructure() {}

    public static final MultiblockLayout LAYOUT = MultiblockLayout.fromShared(SharedBoilerStructure.LAYOUT);
}
