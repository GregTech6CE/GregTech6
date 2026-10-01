package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.blockentity.RockBlockEntity;
import com.gregtech.gregtech.registry.GTBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Places GT6-style surface pebbles ({@link com.gregtech.gregtech.block.RockBlock}). */
public final class GTRockPlacement {
    private GTRockPlacement() {}

    /**
     * Puts a rock of {@code material} on the terrain surface of column (x,z).
     * Rocks above ore veins carry the vein material — the prospecting hint.
     */
    public static boolean placeRock(WorldGenLevel level, int x, int z, GTMaterial material) {
        return placeRock(level, x, z, material, false);
    }

    /** {@code rawOre} rocks (above bedrock deposits) additionally yield a raw ore chunk. */
    public static boolean placeRock(WorldGenLevel level, int x, int z, GTMaterial material, boolean rawOre) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        if (level.isOutsideBuildHeight(y) || y <= level.getMinBuildHeight()) return false;
        BlockPos pos = new BlockPos(x, y, z);
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        if (!level.getBlockState(pos).isAir()) return false;
        if (!ground.isFaceSturdy(level, below, Direction.UP)) return false;
        return place(level, pos, material == null ? null : material.resolve().getName(), null, rawOre);
    }

    /**
     * Places GT6's lying-item rock at an exact position, carrying a material, an explicit item id
     * (GT6's {@code ST.save(NBT_VALUE, stack)}), or both. Used by the nether ground litter and by
     * {@link GTRocksFeature}'s surface rocks.
     */
    public static boolean place(WorldGenLevel level, BlockPos pos, String material, String itemId, boolean rawOre) {
        BlockState ground = level.getBlockState(pos.below());
        // Match the rock's look to the ground it sits on (sand rocks on sand etc.).
        BlockState rockState = GTBlocks.ROCK.get().defaultBlockState()
                .setValue(com.gregtech.gregtech.block.RockBlock.GROUND,
                        com.gregtech.gregtech.block.RockBlock.Ground.of(ground));
        if (!level.setBlock(pos, rockState, 2)) return false;
        if (level.getBlockEntity(pos) instanceof RockBlockEntity rock) {
            if (material != null && !material.isEmpty()) rock.setMaterial(material);
            if (itemId != null && !itemId.isEmpty()) rock.setItemId(itemId);
            rock.setRawOre(rawOre);
        }
        return true;
    }
}
