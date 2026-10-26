package gregtech.content.data;

import gregtech.engine.material.GTMaterialRegistry;
import gregtech.content.block.misc.SpikeBlock;
import net.minecraft.world.level.block.state.BlockState;

/** BlockBaseSpike.getHarvestLevel(meta): source material quality, not one tier per family registry key. */
public final class SpikeHarvestPolicy {
    private SpikeHarvestPolicy() {}
    public static int level(BlockState state) {
        var spike = (SpikeBlock) state.getBlock();
        boolean secondary = state.getValue(SpikeBlock.SECONDARY);
        String material = switch (spike.family()) {
            case METAL -> secondary ? "Lead" : "Copper";
            case STEEL -> secondary ? "RedSteel" : "BlueSteel";
            case SHARP -> secondary ? "Titanium" : "Steel";
            case FANCY -> secondary ? "Silver" : "Gold";
            case SUPER -> secondary ? "Adamantium" : "TungstenSteel";
        };
        return GTMaterialRegistry.get(material).getToolQuality();
    }
}
