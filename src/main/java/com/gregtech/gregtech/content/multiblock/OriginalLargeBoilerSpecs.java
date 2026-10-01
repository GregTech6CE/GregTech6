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

    private static final List<Variant> VARIANTS = List.of(
            new Variant(17201, "stainless_steel_boiler_main_barometer", 18022,   8_192,   6),
            new Variant(17202, "titanium_boiler_main_barometer",       18026,  16_384,   9),
            new Variant(17203, "tungstensteel_boiler_main_barometer",   18023,  32_768, 12.5f),
            new Variant(17204, "adamantium_boiler_main_barometer",     18025, 262_144, 100),
            new Variant(17205, "invar_boiler_main_barometer",          18027,   8_192,   6));

    /** Controller at front-centre of the lowest wall layer, one block ahead of the body centre. */
    public static final MultiblockLayout LAYOUT = layout();

    private OriginalLargeBoilerSpecs() {}
    public static List<Variant> all() { return VARIANTS; }
    public static Variant byOriginalId(int id) {
        return VARIANTS.stream().filter(v -> v.originalId() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown GT6 large boiler " + id));
    }

    private static MultiblockLayout layout() {
        var cells = new ArrayList<MultiblockLayout.Cell>();
        for (int y = -1; y <= 2; y++) for (int back = 0; back <= 2; back++)
            for (int right = -1; right <= 1; right++) {
                MultiblockLayout.Role role;
                if (y == -1) role = MultiblockLayout.Role.HEAT_INPUT;
                else if (y == 0) role = MultiblockLayout.Role.FLUID_INPUT;
                else if (y == 1 && right == 0 && back == 1) role = MultiblockLayout.Role.AIR;
                else role = MultiblockLayout.Role.FLUID_OUTPUT;
                cells.add(new MultiblockLayout.Cell(right, y, back, role));
            }
        return new MultiblockLayout(cells);
    }
}
