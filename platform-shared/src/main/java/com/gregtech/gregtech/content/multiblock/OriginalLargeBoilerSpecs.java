package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import com.gregtech.gregtech.registry.GTMultiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The five large steam boilers from GT6 Loader_MultiTileEntities 17201–17205. */
public final class OriginalLargeBoilerSpecs {
    /** All volumes are GT6 litres, represented by Forge millibuckets in the port. */
    public record Variant(int originalId, String path, int denseWallId, long steamOutput,
                          float hardness) {
        public Block wall() { return LargeMachineParts.block(denseWallId); }
        public long heatCapacity() { return steamOutput * 10_000L; }
        public long steamCapacity() { return steamOutput * 10_000L; }
        public long heatInputRecommended() { return steamOutput / 2; }
        public Map<BlockPos, Block> previewCells() {
            var result = new LinkedHashMap<BlockPos, Block>();
            for (var cell : LAYOUT.cells()) {
                BlockPos local = new BlockPos(cell.right(), cell.up(), cell.back());
                if (local.equals(BlockPos.ZERO)) continue;
                result.put(local, switch (cell.role()) {
                    case HEAT_INPUT -> GTMultiblocks.HEAT_TRANSMITTER.get();
                    case AIR -> Blocks.AIR;
                    default -> wall();
                });
            }
            return Map.copyOf(result);
        }
    }

    private static final List<Variant> VARIANTS=OriginalLargeBoilerParameters.all().stream().map(p->new Variant(p.originalId(),p.path(),p.denseWallId(),p.steamOutput(),p.hardness())).toList();

    /** Controller at front-centre of the lowest wall layer, one block ahead of the body centre. */
    public static final MultiblockLayout LAYOUT = layout();

    private OriginalLargeBoilerSpecs() {}
    public static List<Variant> all() { return VARIANTS; }
    public static Variant byOriginalId(int id) {
        return VARIANTS.stream().filter(v -> v.originalId() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown GT6 large boiler " + id));
    }

    private static MultiblockLayout layout(){return MultiblockLayout.fromShared(OriginalLargeBoilerParameters.LAYOUT);}
}
