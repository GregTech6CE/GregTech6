package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.block.OreBlock;
import com.gregtech.gregtech.block.OreHostStone;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;

/** Masson host-adaptation rule over GT6's existing state-based host identity, without paired ore IDs. */
public final class OreHostStateAdapter {
    private OreHostStateAdapter() {}
    public static Optional<BlockState> adapt(BlockState selected, BlockState replaced) {
        if (!"gregtech".equals(BuiltInRegistries.BLOCK.getKey(selected.getBlock()).getNamespace())
                || !(selected.getBlock() instanceof OreBlock ore)) return Optional.of(selected);
        OreHostStone host = GTOreBlockResolver.stoneOf(replaced);
        if (host == null && ore.isSmall()) host = GTOreBlockResolver.sedimentOf(replaced);
        if (host == null) return Optional.empty();
        // Retaining the selected state preserves every existing property (including broken).
        return Optional.of(selected.setValue(OreBlock.STONE, host));
    }
}
